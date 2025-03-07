package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.model.ScanCodeSummaryEntity;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 扫描代码统计信息持久层
 */
@Repository
public class ScanCodeSummaryDao {

    private static final String COLLECTION_NAME = "t_scan_code_summary";

    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 批量查询指定任务构建id的代码量
     */
    public List<ScanCodeSummaryEntity> batchFindByTaskIdAndBuildId(Map<Long, String> taskIdBuildIdMap) {
        if (taskIdBuildIdMap == null || taskIdBuildIdMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<Criteria> orCriteriaList = Lists.newArrayList();
        taskIdBuildIdMap.forEach((taskId, buildId) -> {
            orCriteriaList.add(Criteria.where("task_id").is(taskId).and("build_id").is(buildId));
        });

        Criteria criteria = new Criteria();
        if (CollectionUtils.isNotEmpty(orCriteriaList)) {
            criteria.orOperator(orCriteriaList.toArray(new Criteria[0]));
        }
        Query query = new Query(criteria);

        query.fields().include("task_id", "total_line");
        return defectMongoTemplate.find(query, ScanCodeSummaryEntity.class, COLLECTION_NAME);
    }
}
