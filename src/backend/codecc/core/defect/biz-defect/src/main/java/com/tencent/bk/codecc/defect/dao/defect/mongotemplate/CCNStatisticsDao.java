package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.tencent.bk.codecc.defect.model.statistic.CCNStatisticEntity;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class CCNStatisticsDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 根据tool_name、time、task_id范围CCN工具扫描信息
     * @param toolName 工具名
     * @param taskIds 任务id列表
     * @param startTime 起始时间区间
     * @param endTime 结束时间区间
     * @return 工具扫描信息报告
     */
    public List<CCNStatisticEntity> getCCNScanStatisticList(
            String toolName,
            List<Long> taskIds,
            Long startTime,
            Long endTime,
            List<String> fields
    ) {
        List<CCNStatisticEntity> ccnDefectList = new ArrayList<>();
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
            List<CCNStatisticEntity> batchResult =
                    defectMongoTemplate.find(query, CCNStatisticEntity.class);
            if (batchResult.isEmpty()) {
                break;
            }
            ccnDefectList.addAll(batchResult);
            page++;
        }
        return ccnDefectList;
    }
}
