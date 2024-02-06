package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.vo.ToolBuildInfoReqVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 工具构建信息DAO
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Repository
@Slf4j
public class ToolBuildInfoDao {
    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 更新
     *
     * @param taskId
     * @param toolName
     * @param forceFullScan
     * @param baseBuildId
     */
    public void upsert(long taskId, String toolName, String forceFullScan, String baseBuildId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId)
                .and("tool_name").is(toolName));

        Update update = new Update();
        update.set("task_id", taskId)
                .set("tool_name", toolName);
        if (StringUtils.isNotEmpty(forceFullScan)) {
            update.set("force_full_scan", forceFullScan);
        }
        if (StringUtils.isNotEmpty(baseBuildId)) {
            update.set("defect_base_build_id", baseBuildId);
            update.set("updated_date", System.currentTimeMillis());
        }

        defectMongoTemplate.upsert(query, update, ToolBuildInfoEntity.class);
    }

    /**
     * 设置强制全量扫描标志
     *
     * @param taskId
     * @param toolName
     */
    public void setForceFullScan(long taskId, String toolName) {
        upsert(taskId, toolName, ComConstants.CommonJudge.COMMON_Y.value(), null);
    }

    /**
     * 设置强制全量扫描标志
     *
     * @param taskId
     * @param toolName
     */
    public void clearForceFullScan(long taskId, String toolName) {
        upsert(taskId, toolName, ComConstants.CommonJudge.COMMON_N.value(), null);
    }

    /**
     * 更新告警快照基准构建ID
     *
     * @param taskId
     * @param toolName
     * @param buildId
     */
    public void updateDefectBaseBuildId(long taskId, String toolName, String buildId) {
        upsert(taskId, toolName, null, buildId);
    }


    /**
     * 批量编辑工具构建信息
     *
     * @param taskIdList 任务id集合
     * @param toolNameList 请求体
     */
    public void editToolBuildInfo(Collection<Long> taskIdList, List<String> toolNameList) {
        Query query = new Query();
        // 任务id
        if (CollectionUtils.isNotEmpty(taskIdList)) {
            query.addCriteria(Criteria.where("task_id").in(taskIdList));
        }
        // 工具
        if (CollectionUtils.isNotEmpty(toolNameList)) {
            query.addCriteria(Criteria.where("tool_name").in(toolNameList));
        }
        Update update = new Update();
        update.set("force_full_scan", ComConstants.CommonJudge.COMMON_Y.value());

        defectMongoTemplate.updateMulti(query, update, ToolBuildInfoEntity.class);
    }

    /**
     * 编辑单条工具构建信息
     *
     * @param reqVO 请求体
     */
    public void editOneToolBuildInfo(ToolBuildInfoReqVO reqVO) {
        Query query = new Query();
        // 任务id
        if (reqVO.getTaskId() != null && reqVO.getTaskId() != 0) {
            query.addCriteria(Criteria.where("task_id").is(reqVO.getTaskId()));
        }
        // 工具
        if (StringUtils.isNotEmpty(reqVO.getToolName())) {
            query.addCriteria(Criteria.where("tool_name").is(reqVO.getToolName()));
        }
        // 构建ID
        if (StringUtils.isNotEmpty(reqVO.getDefectBaseBuildId())) {
            query.addCriteria(Criteria.where("defect_base_build_id").is(reqVO.getDefectBaseBuildId()));
        }

        if (StringUtils.isNotEmpty(reqVO.getForceFullScan())) {
            Update update = new Update();
            update.set("force_full_scan", reqVO.getForceFullScan());
            defectMongoTemplate.updateMulti(query, update, ToolBuildInfoEntity.class);
        }
    }

    /**
     * 查询最新build id
     *
     * @param taskIdSet 任务id集合
     * @return list
     */
    public List<ToolBuildInfoEntity> findLatestBuildIdByTaskIdSet(Collection<Long> taskIdSet) {
        long start = System.currentTimeMillis();

        Criteria criteria = new Criteria();
        if (CollectionUtils.isNotEmpty(taskIdSet)) {
            criteria.and("task_id").in(taskIdSet);
        }
//        criteria.and("updated_date").exists(true);

        MatchOperation match = Aggregation.match(criteria);
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "updated_date");

        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("defect_base_build_id").as("defect_base_build_id");

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, sort, group).withOptions(options);
        AggregationResults<ToolBuildInfoEntity> results =
                defectMongoTemplate.aggregate(agg, "t_tool_build_info", ToolBuildInfoEntity.class);

        log.info("task id count: {} ,elapse time: {}ms", taskIdSet.size(), System.currentTimeMillis() - start);
        return results.getMappedResults();
    }
}
