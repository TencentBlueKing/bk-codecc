package com.tencent.bk.codecc.defect.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.StatDefectDao;
import com.tencent.bk.codecc.defect.model.DefectJsonFileEntity;
import com.tencent.bk.codecc.defect.model.defect.StatDefectEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.service.impl.GithubIssueStatisticServiceImpl;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.api.ServiceToolMetaRestResource;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import com.tencent.devops.common.web.mq.ConstantsKt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("statDefectCommitConsumer")
public class StatDefectCommitConsumer extends AbstractDefectCommitConsumer {

    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    private Client client;
    @Autowired
    private StatDefectDao statDefectDao;
    @Autowired
    private GithubIssueStatisticServiceImpl githubIssueStatisticServiceImpl;

    @Override
    protected boolean uploadDefects(
            CommitDefectVO commitDefectVO,
            Map<String, ScmBlameVO> fileChangeRecordsMap,
            Map<String, RepoSubModuleVO> codeRepoIdMap
    ) {
        long taskId = commitDefectVO.getTaskId();
        String streamName = commitDefectVO.getStreamName();
        String toolName = commitDefectVO.getToolName();
        String buildId = commitDefectVO.getBuildId();

        // 判断增量还是全量
        ToolBuildStackEntity toolBuildStackEntity = toolBuildStackRepository
                .findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        boolean isFullScan = toolBuildStackEntity == null || toolBuildStackEntity.isFullScan();

        // 获取统计类工具自定义参数
        Result<ToolMetaDetailVO> res = client.get(ServiceToolMetaRestResource.class).obtainToolDetail(toolName);
        if (res == null || res.isNotOk() || res.getData() == null) {
            throw new CodeCCException(
                    String.format("obtain tool meta info fail: toolName: %s, taskId: %s, buildId: %s", toolName,
                            taskId, buildId));
        }

        ToolMetaDetailVO.CustomToolInfo customToolInfo = res.getData().getCustomToolInfo();

        String defectListJson = scmJsonComponent.loadRawDefects(streamName, toolName, buildId);
        log.info("stat defect json: {}", defectListJson);
        DefectJsonFileEntity<Map<String, String>> defectJsonFileEntity = JsonUtil.INSTANCE
                .to(defectListJson, new TypeReference<DefectJsonFileEntity<Map<String, String>>>() {
                });

        List<StatDefectEntity> defectEntityList = new ArrayList<>(defectJsonFileEntity.getDefects().size());
        // 设置自定义参数
        defectJsonFileEntity.getDefects()
                .forEach(defectInfo -> {
                    StatDefectEntity defectEntity = new StatDefectEntity();
                    defectEntity.setTaskId(taskId);
                    defectEntity.setToolName(toolName);
                    Map<String, String> customInfoMap = new HashMap<>();
                    customToolInfo.getCustomToolParam()
                            .keySet()
                            .forEach(field -> customInfoMap.put(field, defectInfo.get(field)));
                    defectEntity.setByToolStatInfo(customInfoMap);
                    defectEntityList.add(defectEntity);
                });

        log.warn("stat tool {} get empty custom info, taskId: {}, buildId: {} {}", toolName, taskId, buildId,
                defectEntityList.size());
        // 如果是全量的话，先设置所有数据失效位
        if (isFullScan) {
            statDefectDao.batchDisableStatInfo(taskId, toolName);
        }

        List<List<StatDefectEntity>> partitionList = Lists.partition(defectEntityList, 500);
        for (List<StatDefectEntity> statDefectEntities : partitionList) {
            // 保存本次信息
            statDefectDao.saveAll(statDefectEntities);
        }

        githubIssueStatisticServiceImpl.statisticGithubIssue(taskId, defectJsonFileEntity.getDefects());
        // statisticCommit(defectEntityList, commitDefectVO, customToolInfo.getCustomToolDimension(), isFullScan);
        return true;
    }

    @Override
    protected String getRecommitMQExchange(CommitDefectVO vo) {
        return ConstantsKt.PREFIX_EXCHANGE_DEFECT_COMMIT + ToolPattern.STAT.name().toLowerCase(Locale.ENGLISH);
    }

    @Override
    protected String getRecommitMQRoutingKey(CommitDefectVO vo) {
        return ConstantsKt.PREFIX_ROUTE_DEFECT_COMMIT + ToolPattern.STAT.name().toLowerCase(Locale.ENGLISH);
    }
}
