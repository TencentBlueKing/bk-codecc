package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.statistic.DUPCStatisticEntity;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class DUPCStatisticsDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 根据工具名称，任务编号范围，时间范围获取DUPCStatistic分析报告
     * @param toolName 工具名称
     * @param taskIds 任务编号范围
     * @param startTime 起始时间
     * @param endTime 结束时间
     * @return 分析报告
     */
    public List<DUPCStatisticEntity> getDupcScanStatisticList(
            String toolName,
            List<Long> taskIds,
            Long startTime,
            Long endTime,
            List<String> fields
    ) {
        List<DUPCStatisticEntity> results = new ArrayList<>();

        Query query = Query.query(Criteria.where("tool_name").is(toolName));
        query.addCriteria(Criteria.where("task_id").in(taskIds));
        query.addCriteria(Criteria.where("time").gte(startTime).lt(endTime));
        if (fields != null && !fields.isEmpty()) {
            query.fields().include(fields.toArray(new String[0]));
        }

        long page = 0;
        query.limit(ComConstants.COMMON_PAGE_SIZE);
        while (true) {
            query.skip(page * ComConstants.COMMON_PAGE_SIZE);
            List<DUPCStatisticEntity> batchResult =
                    defectMongoTemplate.find(query, DUPCStatisticEntity.class);
            if (batchResult.isEmpty()) {
                break;
            }
            results.addAll(batchResult);
            page++;
        }
        return results;
    }
}
