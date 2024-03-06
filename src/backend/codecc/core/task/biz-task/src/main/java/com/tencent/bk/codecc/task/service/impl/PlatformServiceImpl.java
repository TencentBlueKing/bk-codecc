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

package com.tencent.bk.codecc.task.service.impl;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.task.dao.mongorepository.PlatformInfoRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.PlatformMigrateLogRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.PlatformInfoDao;
import com.tencent.bk.codecc.task.model.PlatformInfoEntity;
import com.tencent.bk.codecc.task.model.PlatformMigrateLogEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.service.PlatformService;
import com.tencent.bk.codecc.task.vo.PlatformMigrateReqVO;
import com.tencent.bk.codecc.task.vo.PlatformVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.web.mq.ConstantsKt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Platform业务实现类
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Service
@Slf4j
public class PlatformServiceImpl implements PlatformService {
    @Autowired
    private PlatformInfoRepository platformInfoRepository;

    @Autowired
    private ToolRepository toolRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private PlatformInfoDao platformInfoDao;
    @Autowired
    private PlatformMigrateLogRepository platformMigrateLogRepository;


    @Override
    public List<PlatformVO> getPlatformByToolName(String toolName)
    {
        List<PlatformInfoEntity> entityList = platformInfoRepository.findByToolName(toolName);
        return null == entityList ? new ArrayList<>() : entityList.stream()
                .map(baseDataEntity ->
                {
                    PlatformVO platformVO = new PlatformVO();
                    BeanUtils.copyProperties(baseDataEntity, platformVO);
                    return platformVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public String getPlatformIp(long taskId, String toolName)
    {
        List<ToolConfigInfoEntity> toolEntities = toolRepository.findPlatformIpByTaskIdAndToolName(taskId, toolName);
        ToolConfigInfoEntity toolEntity = CollectionUtils.isEmpty(toolEntities) ? null : toolEntities.get(0);
        if (toolEntity == null)
        {
            log.error("can't find platform ip by taskId [{}] and toolName [{}]", taskId, toolName);
            return null;
        }
        String platformIp = toolEntity.getPlatformIp();
        log.info("success getPlatformIp: {}", platformIp);
        return platformIp;
    }

    @Override
    public PlatformVO getPlatformByToolNameAndIp(String toolName, String ip)
    {
        PlatformInfoEntity platformEntity = platformInfoRepository.findFirstByToolNameAndIp(toolName, ip);

        if (platformEntity != null)
        {
            PlatformVO platformVO = new PlatformVO();
            BeanUtils.copyProperties(platformEntity, platformVO);

            return platformVO;
        }

        return null;
    }

    @Override
    public List<PlatformVO> getPlatformInfo(String toolName, String platformIp)
    {
        List<PlatformInfoEntity> platformInfoEntities = platformInfoDao.queryEntity(toolName, platformIp);
        log.info("query platform info entity count: {}", platformInfoEntities.size());

        List<PlatformVO> platformVoList = null;
        if (CollectionUtils.isNotEmpty(platformInfoEntities))
        {
            platformVoList = platformInfoEntities.stream().map(entity ->
            {
                PlatformVO platformVO = new PlatformVO();
                BeanUtils.copyProperties(entity, platformVO);
                return platformVO;
            }).collect(Collectors.toList());
        }

        return platformVoList;
    }

    /**
     * 将某个Platform上的所有task 迁移到另一个Platform
     *
     * @param reqVO    迁移请求体
     * @param userName 执行人
     * @return boolean
     */
    @Override
    public Boolean batchMigratePlatformForTask(@NotNull PlatformMigrateReqVO reqVO, String userName) {
        log.info("batchMigratePlatformForTask operator: {}, reqVO: {}", userName, reqVO);

        // 参数检查
        String toolName = reqVO.getToolName();
        String sourceIp = reqVO.getSourceIp();
        String targetIp = reqVO.getTargetIp();
        if (StringUtils.isEmpty(toolName) || StringUtils.isEmpty(sourceIp) || StringUtils.isEmpty(targetIp)) {
            log.error("batchMigratePlatformForTask param is empty!");
            return false;
        }

        List<ToolConfigInfoEntity> toolConfigEntities = toolRepository.findByToolNameAndPlatformIp(toolName, sourceIp);
        if (CollectionUtils.isEmpty(toolConfigEntities)) {
            log.warn("toolConfigEntities is empty");
            return false;
        }

        // 批量变更为新的Platform IP并记录任务id
        Set<Long> taskIdSet = Sets.newHashSet();
        for (ToolConfigInfoEntity toolConfigEntity : toolConfigEntities) {
            taskIdSet.add(toolConfigEntity.getTaskId());
            toolConfigEntity.setPlatformIp(targetIp);
            toolConfigEntity.applyAuditInfoOnUpdate(userName);
        }
        toolRepository.saveAll(toolConfigEntities);

        PlatformMigrateLogEntity logEntity = new PlatformMigrateLogEntity();
        logEntity.applyAuditInfoOnCreate(userName);
        logEntity.migrateLog(toolName, sourceIp, targetIp);
        logEntity.setMigratedTaskIdSet(taskIdSet);
        logEntity.setMigrateTaskCount(taskIdSet.size());
        if (StringUtils.isNotEmpty(reqVO.getRemarks())) {
            logEntity.setRemarks(reqVO.getRemarks());
        }

        // 标记可回退状态
        logEntity.setStatus(0);

        // 记录入库
        platformMigrateLogRepository.save(logEntity);
        log.info("batchMigratePlatformForTask finish");
        return true;
    }

    /**
     * 将某次迁移记录的数据进行回滚
     *
     * @param reqVO    回滚请求体
     * @param userName 执行人
     * @return boolean
     */
    @Override
    public Boolean rollsBackMigrateLog(PlatformMigrateReqVO reqVO, String userName) {
        log.info("rollsBackMigrateLog operator: {}, reqVO: {}", userName, reqVO);

        String entityId = reqVO.getEntityId();
        if (StringUtils.isEmpty(entityId)) {
            log.error("rollsBackMigrateLog param is empty!");
            return false;
        }

        PlatformMigrateLogEntity migrateLogEntity = platformMigrateLogRepository.findFirstByEntityId(entityId);
        if (null == migrateLogEntity) {
            log.error("Abort rolls back, migrateLogEntity is null!");
            return false;
        }

        String sourceIp = migrateLogEntity.getSourceIp();

        // 处理极端情况: 原IP已删除,则无法回滚
        PlatformInfoEntity platformInfoEntity =
                platformInfoRepository.findFirstByToolNameAndIp(migrateLogEntity.getToolName(), sourceIp);
        if (null == platformInfoEntity) {
            String errMsg = "platform ip is not found, please new add platform instance for " + sourceIp;
            log.error(errMsg);
            throw new CodeCCException(CommonMessageCode.RECORD_NOT_EXITS, errMsg);
        }

        String targetIp = migrateLogEntity.getTargetIp();
        Set<Long> migratedTaskIdSet = migrateLogEntity.getMigratedTaskIdSet();

        List<ToolConfigInfoEntity> toolConfigInfoEntityList =
                toolRepository.findByTaskIdInAndToolName(migratedTaskIdSet, migrateLogEntity.getToolName());
        if (CollectionUtils.isNotEmpty(toolConfigInfoEntityList)) {
            for (ToolConfigInfoEntity entity : toolConfigInfoEntityList) {

                // 判断是否为迁移时修改的目标IP
                if (targetIp.equals(entity.getPlatformIp())) {
                    entity.setPlatformIp(sourceIp);
                }
            }
            toolRepository.saveAll(toolConfigInfoEntityList);
        }

        // 标记已回滚
        migrateLogEntity.setStatus(1);
        String remarks = StringUtils.defaultIfEmpty(migrateLogEntity.getRemarks(), "");
        migrateLogEntity.setRemarks("(已回滚)" + remarks);
        platformMigrateLogRepository.save(migrateLogEntity);
        return true;
    }

    /**
     * 修改Platform配置
     *
     * @param userName   用户名
     * @param platformVO 请求体
     * @return boolean
     */
    @Override
    public Boolean updatePlatformInfo(String userName, PlatformVO platformVO) {
        log.info("updatePlatformInfo req: {}", platformVO);
        // 1.根据toolName和ip查出将要修改的Platform数据
        if (null != platformVO) {
            PlatformInfoEntity platformInfoEntity =
                    platformInfoRepository.findFirstByToolNameAndIp(platformVO.getToolName(), platformVO.getIp());
            if (null != platformInfoEntity) {
                platformInfoEntity.setStatus(platformVO.getStatus());
                platformInfoEntity.setSupportTaskTypes(platformVO.getSupportTaskTypes());
                platformInfoEntity.setOwner(platformVO.getOwner());
                platformInfoEntity.setUserName(platformVO.getUserName());
                platformInfoEntity.setPasswd(platformVO.getPasswd());
                // 修改KLOCWORK工具需要设置token
                if (platformVO.getToolName().equals(ComConstants.Tool.KLOCWORK.name())) {
                    platformInfoEntity.setToken(platformVO.getToken());
                }

                platformInfoEntity.setUpdatedBy(userName);
                platformInfoEntity.setUpdatedDate(System.currentTimeMillis());

                platformInfoRepository.save(platformInfoEntity);
            } else {
                PlatformInfoEntity addPlatformInfoEntity = new PlatformInfoEntity();
                BeanUtils.copyProperties(platformVO, addPlatformInfoEntity);
                addPlatformInfoEntity.setCreatedBy(userName);
                addPlatformInfoEntity.setCreatedDate(System.currentTimeMillis());
                platformInfoRepository.save(addPlatformInfoEntity);
            }

            // 数据变更后,同步到所有节点的缓存实例
            platformQueueDistribute(platformVO);
            return true;
        } else {
            log.error("Request parameter is empty!");
            return false;
        }
    }

    /**
     * 删除Platform配置
     *
     * @param platformVO 请求体
     * @return boolean
     */
    @Override
    public Boolean deletePlatformInfo(PlatformVO platformVO) {
        log.info("deletePlatformInfo req: {}", platformVO);

        if (StringUtils.isNotEmpty(platformVO.getToolName()) && StringUtils.isNotEmpty(platformVO.getIp())) {
            // 根据toolName和ip查询数据
            PlatformInfoEntity platformInfoEntity =
                    platformInfoRepository.findFirstByToolNameAndIp(platformVO.getToolName(), platformVO.getIp());
            if (null != platformInfoEntity) {
                platformInfoRepository.delete(platformInfoEntity);

                // DB删除后,同步到Cov/Klocwork每个节点删除缓存实例
                BeanUtils.copyProperties(platformInfoEntity, platformVO);
                platformVO.setStatus(-1);

                platformQueueDistribute(platformVO);
                return true;
            } else {
                log.error("platformInfoData not found!");
                return false;
            }
        } else {
            log.error("Parameters tool name and Ip are empty!");
            return false;
        }
    }

    /**
     * 公共方法 队列分配(根据工具 分配到不同队列)
     *
     * @param platformVO VO
     */
    private void platformQueueDistribute(PlatformVO platformVO) {
        if (platformVO.getToolName().equals(ComConstants.Tool.COVERITY.name())) {
            // COVERITY工具
            redisTemplate.convertAndSend(ConstantsKt.EXCHANGE_COVERITY_INST_UPDATE,
                    JsonUtil.INSTANCE.toJson(platformVO));
        } else if (platformVO.getToolName().equals(ComConstants.Tool.KLOCWORK.name())) {
            // KLOCWORK工具
            redisTemplate.convertAndSend(ConstantsKt.EXCHANGE_KLOCWORK_INST_UPDATE,
                    JsonUtil.INSTANCE.toJson(platformVO));
        }
    }

}
