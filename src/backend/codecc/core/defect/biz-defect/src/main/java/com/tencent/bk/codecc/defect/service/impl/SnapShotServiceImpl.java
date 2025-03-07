/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.SnapShotRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.SnapShotDao;
import com.tencent.bk.codecc.defect.model.NotRepairedAuthorEntity;
import com.tencent.bk.codecc.defect.model.SnapShotEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.LintSnapShotEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.ToolSnapShotEntity;
import com.tencent.bk.codecc.defect.service.ICheckReportBizService;
import com.tencent.bk.codecc.defect.service.SnapShotService;
import com.tencent.bk.codecc.defect.vo.common.SnapShotVO;
import com.tencent.bk.codecc.task.api.ServiceToolRestResource;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.util.BeanUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 快照服务层代码
 *
 * @version V1.0
 * @date 2019/6/28
 */
@Slf4j
@Service
public class SnapShotServiceImpl implements SnapShotService {

    /**
     * 字符串锁前缀
     */
    private static final String LOCK_KEY_PREFIX = "GET_AND_SET_SNAPSHOT:";

    /**
     * 分布式锁超时时间
     */
    private static final Long LOCK_TIMEOUT = 10L;
    private static final String MOCK_KEY = "mock";
    @Autowired
    private BizServiceFactory<ICheckReportBizService> checkReportBizServiceBizServiceFactory;
    @Autowired
    private SnapShotRepository snapShotRepository;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private Client client;
    @Autowired
    private SnapShotDao snapShotDao;
    private LoadingCache<String, List<String>> toolOrderCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(15, TimeUnit.MINUTES)
            .build(new CacheLoader<String, List<String>>() {
                @Override
                public List<String> load(String key) {
                    try {
                        return Arrays.asList(client.get(ServiceToolRestResource.class).findToolOrder().getData()
                                .split(","));
                    } catch (Exception e) {
                        log.info("fail to get tool order", e);
                        return new ArrayList<>();
                    }
                }
            });

    @Override
    public SnapShotEntity saveToolBuildSnapShot(long taskId, String projectId, String pipelineId, String buildId,
            String resultStatus, String resultMessage, String toolName) {
        SnapShotEntity snapShotEntity = null;
        ICheckReportBizService checkReportBizService = checkReportBizServiceBizServiceFactory.createBizService(toolName,
                ComConstants.BusinessType.CHECK_REPORT.value(), ICheckReportBizService.class);
        ToolSnapShotEntity toolSnapShotEntity = checkReportBizService.getReport(taskId, projectId, toolName, buildId);
        if (null == toolSnapShotEntity) {
            toolSnapShotEntity = new ToolSnapShotEntity();
        }
        toolSnapShotEntity.setResultStatus(resultStatus);
        toolSnapShotEntity.setResultMessage(resultMessage);

        RedisLock lock = new RedisLock(redisTemplate,
                LOCK_KEY_PREFIX + taskId + ComConstants.SEPARATOR_SEMICOLON + buildId,
                LOCK_TIMEOUT);
        try {
            lock.lock();
            // buildFlag是每次构建上下文的触发时间，也是重试边界；拿最后那次保证最新
            snapShotEntity = snapShotRepository.findFirstByProjectIdAndBuildIdAndTaskIdOrderByBuildFlagDesc(
                    projectId, buildId, taskId);
            if (null == snapShotEntity) {
                snapShotEntity = new SnapShotEntity();
                snapShotEntity.setProjectId(projectId);
                snapShotEntity.setPipelineId(pipelineId);
                snapShotEntity.setTaskId(taskId);
                snapShotEntity.setBuildId(buildId);
                snapShotEntity.setToolSnapshotList(new ArrayList<>());
            }

            List<ToolSnapShotEntity> toolSnapShotEntityList = snapShotEntity.getToolSnapshotList();
            // 兼容重试场景，替换产出报告
            toolSnapShotEntityList.removeIf(x -> toolName.equalsIgnoreCase(x.getToolNameEn()));
            toolSnapShotEntityList.add(toolSnapShotEntity);

            snapShotRepository.save(snapShotEntity);
        } finally {
            lock.unlock();
        }

        return snapShotEntity;
    }

    @Override
    public SnapShotVO getTaskToolBuildSnapShot(String projectId, String buildId, long taskId) {
        // 查询最新的快照（重试扫描同一个buildId下会产生多个快照）
        Optional<SnapShotEntity> latestSnapShot = snapShotDao.getLatestSnapShot(projectId, buildId, taskId);
        SnapShotEntity snapShot = latestSnapShot.orElseGet(SnapShotEntity::new);

        List<String> toolOrder = toolOrderCache.getUnchecked(MOCK_KEY);
        snapShot.getToolSnapshotList().sort(Comparator.comparingInt(it -> toolOrder.contains(it.getToolNameEn())
                ? toolOrder.indexOf(it.getToolNameEn()) : Integer.MAX_VALUE));
        // 作者排序
        for (ToolSnapShotEntity toolSnapShotEntity : snapShot.getToolSnapshotList()) {
            if (toolSnapShotEntity instanceof LintSnapShotEntity
                    && CollectionUtils.isNotEmpty(((LintSnapShotEntity) toolSnapShotEntity).getAuthorList())) {
                ((LintSnapShotEntity) toolSnapShotEntity).getAuthorList().sort(Comparator.comparing(
                        NotRepairedAuthorEntity::getTotalCount, Comparator.reverseOrder()));
            }
        }

        SnapShotVO snapShotVO = new SnapShotVO();
        BeanUtils.copyProperties(snapShot, snapShotVO);

        return snapShotVO;
    }

    @Override
    public void updateMetadataReportStatus(String projectId, String buildId, long taskId, boolean status) {
        snapShotDao.updateMetadataReportStatus(projectId, taskId, buildId, status, null);
        log.info("update metadata_report projectId: {}, taskId: {}, buildId: {}, status: {}",
                projectId, taskId, buildId, status);
    }

    @Override
    public SnapShotVO getSnapShotVO(String projectId, Long taskId, String buildId) {
        SnapShotEntity snapShot = snapShotRepository.findFirstByProjectIdAndBuildIdAndTaskIdOrderByBuildFlagDesc(
                projectId, buildId, taskId);
        SnapShotVO snapShotVO = new SnapShotVO();
        if (snapShot != null) {
            BeanUtils.copyProperties(snapShot, snapShotVO);
        } else {
            snapShotVO.setMetadataReport(null);
        }
        return snapShotVO;
    }

    @Override
    public void allocateSnapshotOnBuildStart(
            String projectId,
            String pipelineId,
            Long taskId,
            String buildId,
            Long buildFlag
    ) {
        if (buildFlag == null
                || buildFlag == 0L
                || StringUtils.isEmpty(projectId)
                || StringUtils.isEmpty(buildId)) {
            return;
        }

        SnapShotEntity snapShotEntity =
                snapShotRepository.findFirstByProjectIdAndBuildIdAndBuildFlag(projectId, buildId, buildFlag);
        if (snapShotEntity != null) {
            return;
        }

        // 防止插件内部重试导致并发问题
        String redisKey = String.format("ALLOCATE_SNAPSHOT_ON_BUILD_START:%s:%s:%s", projectId, buildId, buildFlag);
        RedisLock lock = new RedisLock(redisTemplate, redisKey, TimeUnit.SECONDS.toSeconds(3));

        try {
            lock.lock();

            snapShotEntity =
                    snapShotRepository.findFirstByProjectIdAndBuildIdAndBuildFlag(projectId, buildId, buildFlag);
            if (snapShotEntity != null) {
                return;
            }

            snapShotEntity = new SnapShotEntity();
            snapShotEntity.setProjectId(projectId);
            snapShotEntity.setBuildId(buildId);
            snapShotEntity.setBuildFlag(buildFlag);
            snapShotEntity.setPipelineId(pipelineId);
            snapShotEntity.setTaskId(taskId);
            snapShotEntity.setToolSnapshotList(Lists.newArrayList());
            snapShotRepository.save(snapShotEntity);
        } finally {
            lock.unlock();
        }
    }
}
