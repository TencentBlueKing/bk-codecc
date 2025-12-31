/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 Tencent. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.FileCommentIgnoresDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.IgnoreDefectDao;
import com.tencent.bk.codecc.defect.model.ignore.FileCommentIgnoresEntity;
import com.tencent.bk.codecc.defect.model.ignore.IgnoreCommentDefectModel;
import com.tencent.bk.codecc.defect.model.ignore.IgnoreCommentDefectSubModel;
import com.tencent.bk.codecc.defect.service.IIgnoreDefectService;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreCommentDefectSubVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreCommentDefectVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 注释忽略服务实现
 * 
 * @date 2021/6/30
 * @version V1.0
 */
@Service
public class IgnoreDefectServiceImpl implements IIgnoreDefectService {
    @Autowired
    private FileCommentIgnoresDao fileCommentIgnoresDao;
    @Autowired
    private IgnoreDefectDao ignoreDefectDao;
    private static Logger logger = LoggerFactory.getLogger(IgnoreDefectServiceImpl.class);
    private static final int BATCH_SIZE = 1000;

    private void processFileCommentIgnoresEntity(
            FileCommentIgnoresEntity entity,
            Map<String, List<IgnoreCommentDefectSubVO>> ignoreDefectMap
    ) {
        if (entity == null || CollectionUtils.isEmpty(entity.getLineIgnoreInfos())
                || StringUtils.isBlank(entity.getFilePath())) {
            return;
        }

        String filePath = entity.getFilePath();
        List<IgnoreCommentDefectSubVO> lineIgnoreInfoVOs = new ArrayList<>();
        entity.getLineIgnoreInfos().forEach(lineIgnoreInfo -> {
            if (lineIgnoreInfo.getLineNum() == null || CollectionUtils.isEmpty(lineIgnoreInfo.getIgnoreInfos())) {
                return;
            }

            IgnoreCommentDefectSubVO lineIgnoreInfoVO = new IgnoreCommentDefectSubVO();
            lineIgnoreInfoVO.setLineNum(lineIgnoreInfo.getLineNum().intValue());
            Map<String, String> checker2Reason = new HashMap<>();
            lineIgnoreInfo.getIgnoreInfos().forEach(ignoreInfo -> {
                checker2Reason.put(ignoreInfo.getChecker(), ignoreInfo.getIgnoreReason());
            });
            lineIgnoreInfoVO.setIgnoreRule(checker2Reason);
            lineIgnoreInfoVOs.add(lineIgnoreInfoVO);
        });
        ignoreDefectMap.put(filePath, lineIgnoreInfoVOs);
    }

    private IgnoreCommentDefectVO findIgnoreDefectInfoV2(Long taskId) {
        long count = fileCommentIgnoresDao.countByTaskId(taskId);
        if (count == 0) {
            return null;
        }

        logger.info("get {} fileCommentIgnores for task({})", count, taskId);
        IgnoreCommentDefectVO result = new IgnoreCommentDefectVO();
        result.setTaskId(taskId);
        Map<String, List<IgnoreCommentDefectSubVO>> ignoreDefectMap = new HashMap<>();

        if (count > BATCH_SIZE) {
            // 如果数量太多就分批处理
            String minId = null;
            for (long i = 0; i < count; i += BATCH_SIZE) {
                List<FileCommentIgnoresEntity> entities =
                        fileCommentIgnoresDao.findByTaskIdAndEntityIdBiggerThan(taskId, minId, BATCH_SIZE);
                if (CollectionUtils.isEmpty(entities)) {
                    break;
                }

                entities.forEach(it -> processFileCommentIgnoresEntity(it, ignoreDefectMap));
                int sz = entities.size();
                minId = entities.get(sz - 1).getEntityId();
            }
        } else {
            List<FileCommentIgnoresEntity> entities = fileCommentIgnoresDao.findByTaskId(taskId);
            if (CollectionUtils.isNotEmpty(entities)) {
                entities.forEach(it -> processFileCommentIgnoresEntity(it, ignoreDefectMap));
            }
        }

        result.setIgnoreDefectMap(ignoreDefectMap);

        return result;
    }

    @Override
    public IgnoreCommentDefectVO findIgnoreDefectInfo(Long taskId) {
        IgnoreCommentDefectVO result = findIgnoreDefectInfoV2(taskId);
        // TODO: 迁移相关的代码, 可以在 3-6 个月以后删除. 2025.05.29
        if (result == null) { // 有可能是数据还未迁移
            IgnoreCommentDefectModel oldEntity = ignoreDefectDao.findFirstUnmigratedByTaskId(taskId);
            if (oldEntity == null) {
                return result;
            }

            logger.info("migrate@findIgnoreDefectInfo for task({})", taskId);
            result = new IgnoreCommentDefectVO();
            result.setTaskId(taskId);
            Map<String, List<IgnoreCommentDefectSubVO>> ignoreDefectMap = new HashMap<>();
            List<FileCommentIgnoresEntity> newEntities = migrate(oldEntity, null);
            if (CollectionUtils.isNotEmpty(newEntities)) {
                newEntities.forEach(it -> processFileCommentIgnoresEntity(it, ignoreDefectMap));
            }

            result.setIgnoreDefectMap(ignoreDefectMap);
            ignoreDefectDao.updateMigratedByTaskId(taskId);
        }

        return result;
    }

    /**
     * 对着下面的结构拆 oldEntity, 生成新的存储结构.
     * {
     *     "file_path": [
     *     {
     *         "line_num": xxx,
     *         [
     *         "checker": "ignore_reason",
     *         ...
     *         ]
     *     }]
     * }
     */
    @Override
    public List<FileCommentIgnoresEntity> migrate(IgnoreCommentDefectModel oldEntity, Set<String> excludeFile) {
        logger.info("IIgnoreDefectService.migrate excludeFile size: {}", excludeFile == null ? 0 : excludeFile.size());

        Long taskId = oldEntity.getTaskId();
        List<FileCommentIgnoresEntity> newEntities = new ArrayList<>();

        Map<String, List<IgnoreCommentDefectSubModel>> ignoreDefectMap = oldEntity.getIgnoreDefectMap();
        ignoreDefectMap.forEach((filePath, fileIgnoreInfoVOs) -> {
            if (StringUtils.isBlank(filePath) || CollectionUtils.isEmpty(fileIgnoreInfoVOs)) {
                return;
            }

            // oldEntity 的 filePath 在入库时会把 . 转成 ~
            String realFilePath = filePath.replaceAll("~", ".");
            if (excludeFile != null && excludeFile.contains(realFilePath)) {
                return;
            }

            List<FileCommentIgnoresEntity.LineIgnoreInfo> lineIgnoreInfos = new ArrayList<>();
            fileIgnoreInfoVOs.forEach(lineIgnoreInfoVO -> {
                Integer lineNum = lineIgnoreInfoVO.getLineNum();
                Map<String, String> checkerReason = lineIgnoreInfoVO.getIgnoreRule();
                if (lineNum == null || checkerReason == null) {
                    return;
                }

                List<FileCommentIgnoresEntity.IgnoreInfo> ignoreInfos = new ArrayList<>();
                checkerReason.forEach((checker, reason) -> {
                    if (StringUtils.isBlank(checker)) {
                        return;
                    }
                    // oldEntity 的 checkerName 在入库时会把 . 转成 #DOT#
                    String realChecker = checker.replaceAll("#DOT#", ".");
                    FileCommentIgnoresEntity.IgnoreInfo ignoreInfo = new FileCommentIgnoresEntity.IgnoreInfo();
                    ignoreInfo.setChecker(realChecker);
                    ignoreInfo.setIgnoreReason(reason);
                    ignoreInfos.add(ignoreInfo);
                });

                if (CollectionUtils.isNotEmpty(ignoreInfos)) {
                    FileCommentIgnoresEntity.LineIgnoreInfo lineInfo = new FileCommentIgnoresEntity.LineIgnoreInfo();
                    lineInfo.setLineNum((long) lineNum);
                    lineInfo.setIgnoreInfos(ignoreInfos);

                    lineIgnoreInfos.add(lineInfo);
                }
            });

            if (CollectionUtils.isNotEmpty(lineIgnoreInfos)) {
                FileCommentIgnoresEntity newEntity = new FileCommentIgnoresEntity();
                newEntity.setTaskId(taskId);
                newEntity.setFilePath(realFilePath);
                newEntity.setLineIgnoreInfos(lineIgnoreInfos);
                newEntity.applyAuditInfoOnCreate();
                newEntities.add(newEntity);
            }
        });

        if (CollectionUtils.isNotEmpty(newEntities)) {
            fileCommentIgnoresDao.insertAll(newEntities);
        }

        return newEntities;
    }
}
