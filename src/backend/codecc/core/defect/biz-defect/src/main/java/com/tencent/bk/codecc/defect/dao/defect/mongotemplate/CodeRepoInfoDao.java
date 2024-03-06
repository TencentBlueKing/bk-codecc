package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoInfoEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 代码仓库信息DAO
 *
 * @version V4.0
 * @date 2019/10/16
 */
@Repository
@Slf4j
public class CodeRepoInfoDao {
    @Autowired
    private MongoTemplate defectMongoTemplate;


    public List<CodeRepoInfoEntity> findFirstByTaskIdOrderByCreatedDate(Set<Long> taskIds) {
        //以taskid进行过滤
        MatchOperation match = Aggregation.match(Criteria.where("task_id").in(taskIds));
        //根据创建时间排序
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "create_date");

        //以toolName进行分组，并且取第一个的endTime字段
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .first("repo_list").as("repo_list");
        Aggregation agg = Aggregation.newAggregation(match, sort, group);
        AggregationResults<CodeRepoInfoEntity> queryResult = defectMongoTemplate.aggregate(agg, "t_code_repo_info",
                CodeRepoInfoEntity.class);
        return queryResult.getMappedResults();
    }

    /**
     * 批量获取待更新的仓库对象
     *
     * @return list
     */
    public List<CodeRepoInfoEntity> findByTaskIdAndBuildId(Map<Long, String> taskBuildIdMap) {
        if (MapUtils.isEmpty(taskBuildIdMap)) {
            return Collections.emptyList();
        }

        List<Criteria> orCriteria = Lists.newArrayList();
        taskBuildIdMap.forEach(
                (taskId, buildId) -> orCriteria.add(Criteria.where("task_id").is(taskId).and("build_id").is(buildId)));
        Criteria criteria = new Criteria();
        criteria.orOperator(orCriteria.toArray(new Criteria[0]));
        return defectMongoTemplate.find(new Query(criteria), CodeRepoInfoEntity.class);
    }

    /**
     * 批量更新代码仓库信息
     * @param needUpdateMap taskId entity
     */
    public void batchSetCommitTime(Map<Long, CodeRepoInfoEntity> needUpdateMap) {
        if (MapUtils.isNotEmpty(needUpdateMap)) {
            log.info("needUpdateMap size: {}", needUpdateMap.size());
            BulkOperations bulkOps =
                    defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CodeRepoInfoEntity.class);
            needUpdateMap.forEach((taskId, codeRepoInfo) -> {
                Query query =
                        Query.query(Criteria.where("task_id").is(taskId).and("build_id").is(codeRepoInfo.getBuildId()));

                Update update = new Update();
                update.set("repo_list", codeRepoInfo.getRepoList());
                bulkOps.updateOne(query, update);
            });
            bulkOps.execute();
        }
    }
}
