package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.BuildDefectV2Repository;
import com.tencent.bk.codecc.defect.model.BuildDefectV2Entity;
import com.tencent.devops.common.constant.ComConstants;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class BuildDefectV2Dao {


    @Autowired
    private MongoTemplate defectMongoTemplate;
    @Autowired
    private BuildDefectV2Repository buildDefectV2Repository;

    /**
     * 批量保存快照告警信息
     *
     * @param buildDefectEntityList
     */
    public void save(List<BuildDefectV2Entity> buildDefectEntityList) {
        if (CollectionUtils.isEmpty(buildDefectEntityList)) {
            return;
        }

        BuildDefectV2Entity firstEntity = buildDefectEntityList.get(0);
        long taskId = firstEntity.getTaskId();
        String buildId = firstEntity.getBuildId();
        String toolName = firstEntity.getToolName();
        boolean isDoUpsert =
                buildDefectV2Repository.findFirstByTaskIdAndBuildIdAndToolName(taskId, buildId, toolName) != null;
        long beginTime = System.currentTimeMillis();

        if (buildDefectEntityList.size() > ComConstants.COMMON_BATCH_PAGE_SIZE) {
            List<List<BuildDefectV2Entity>> partitionList = Lists.partition(buildDefectEntityList,
                    ComConstants.COMMON_BATCH_PAGE_SIZE);
            partitionList.forEach(onePartition -> doSave(isDoUpsert, onePartition));
        } else {
            doSave(isDoUpsert, buildDefectEntityList);
        }

        log.info("save build snapshot task id: {}, tool name: {}, build id: {}, cost: {}, upsert: {}, record size: {}",
                taskId,
                toolName,
                buildId,
                System.currentTimeMillis() - beginTime,
                isDoUpsert,
                buildDefectEntityList.size());
    }

    private void doSave(boolean isDoUpsert, List<BuildDefectV2Entity> saveList) {
        if (isDoUpsert) {
            doBatchUpsert(saveList);
        } else {
            doBatchInsert(saveList);
        }
    }

    private void doBatchInsert(List<BuildDefectV2Entity> buildDefectEntityList) {
        defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, BuildDefectV2Entity.class)
                .insert(buildDefectEntityList)
                .execute();
    }

    private void doBatchUpsert(List<BuildDefectV2Entity> buildDefectEntityList) {
        BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, BuildDefectV2Entity.class);

        for (BuildDefectV2Entity entity : buildDefectEntityList) {
            Query query = new Query();
            query.addCriteria(Criteria.where("task_id").is(entity.getTaskId())
                    .and("build_id").is(entity.getBuildId())
                    .and("tool_name").is(entity.getToolName())
                    .and("defect_id").is(entity.getDefectId()));
            Update update = new Update();
            update.set("task_id", entity.getTaskId())
                    .set("tool_name", entity.getToolName())
                    .set("build_id", entity.getBuildId())
                    .set("build_num", entity.getBuildNum())
                    .set("defect_id", entity.getDefectId())
                    .set("revision", entity.getRevision())
                    .set("branch", entity.getBranch())
                    .set("subModule", entity.getSubModule())
                    .set("line_num", entity.getLineNum())
                    .set("start_lines", entity.getStartLines())
                    .set("end_lines", entity.getEndLines());
            ops.upsert(query, update);
        }

        ops.execute();
    }
}
