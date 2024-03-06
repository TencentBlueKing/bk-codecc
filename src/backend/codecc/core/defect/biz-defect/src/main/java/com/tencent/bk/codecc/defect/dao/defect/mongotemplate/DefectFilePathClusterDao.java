package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.model.DefectFilePathClusterEntity;
import com.tencent.devops.common.constant.ComConstants;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

@Slf4j
@Repository
public class DefectFilePathClusterDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;


    /**
     * 保存DefectFilePath对象，如果存在重试使用更新，否则直接插入
     * @param taskId
     * @param toolName
     * @param buildId
     * @param entities
     */
    public void save(Long taskId, String toolName, String buildId, Integer status,
            List<DefectFilePathClusterEntity> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return;
        }
        boolean isDoUpsert = findFirstByTasIdAndToolNameAndBuildIdAndStatus(taskId, toolName, buildId, status) != null;
        long beginTime = System.currentTimeMillis();
        if (entities.size() > ComConstants.COMMON_BATCH_PAGE_SIZE) {
            List<List<DefectFilePathClusterEntity>> partitionList = Lists.partition(entities,
                    ComConstants.COMMON_BATCH_PAGE_SIZE);
            partitionList.forEach(onePartition -> doSave(isDoUpsert, onePartition));
        } else {
            doSave(isDoUpsert, entities);
        }

        log.info("save defect file path task id: {}, tool name: {}, build id: {},"
                        + " cost: {}, upsert: {}, record size: {}",
                taskId,
                toolName,
                buildId,
                System.currentTimeMillis() - beginTime,
                isDoUpsert,
                entities.size());

    }

    public DefectFilePathClusterEntity findFirstByTasIdAndToolNameAndBuildIdAndStatus(Long taskId, String toolName,
            String buildId, Integer status) {
        Query query = Query.query(Criteria.where("task_id").is(taskId)
                .and("tool_name").is(toolName)
                .and("build_id").is(buildId)
                .and("status").is(status));
        return defectMongoTemplate.findOne(query, DefectFilePathClusterEntity.class);
    }

    /**
     * 查找FilePath列表
     *
     * @param taskId
     * @param toolName
     * @param buildId
     * @param status
     * @param pageNum
     * @param pageSize
     * @return
     */
    public List<DefectFilePathClusterEntity> findFilePathByPage(Long taskId, String toolName,
            String buildId, Integer status, Integer pageNum, Integer pageSize) {
        //查询条件
        Query query = Query.query(Criteria.where("task_id").is(taskId)
                .and("tool_name").is(toolName)
                .and("build_id").is(buildId)
                .and("status").is(status));
        //字段过滤
        query.fields().include("file_path").include("rel_path").exclude("_id");
        // 分页
        pageNum = pageNum == null ? Integer.valueOf(0) : pageNum;
        pageSize = pageSize == null ? Integer.valueOf(ComConstants.COMMON_PAGE_SIZE) : pageSize;
        query.skip(pageNum.longValue() * pageSize.longValue());
        query.limit(pageSize);
        return defectMongoTemplate.find(query, DefectFilePathClusterEntity.class);
    }

    private void doSave(boolean isDoUpsert, List<DefectFilePathClusterEntity> entities) {
        if (isDoUpsert) {
            doBatchUpsert(entities);
        } else {
            doBatchInsert(entities);
        }
    }

    private void doBatchInsert(List<DefectFilePathClusterEntity> entities) {
        defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, DefectFilePathClusterEntity.class)
                .insert(entities)
                .execute();
    }

    private void doBatchUpsert(List<DefectFilePathClusterEntity> entities) {
        BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED,
                DefectFilePathClusterEntity.class);

        Date now = new Date();
        for (DefectFilePathClusterEntity entity : entities) {
            Query query = new Query();
            query.addCriteria(Criteria.where("task_id").is(entity.getTaskId())
                    .and("build_id").is(entity.getBuildId())
                    .and("tool_name").is(entity.getToolName())
                    .and("file_path").is(entity.getFilePath())
                    .and("status").is(entity.getStatus()));
            Update update = new Update();
            update.set("task_id", entity.getTaskId())
                    .set("tool_name", entity.getToolName())
                    .set("build_id", entity.getBuildId())
                    .set("file_path", entity.getFilePath())
                    .set("rel_path", entity.getRelPath())
                    .set("status", entity.getStatus())
                    .set("create_at", now);
            ops.upsert(query, update);
        }
        ops.execute();
    }

}
