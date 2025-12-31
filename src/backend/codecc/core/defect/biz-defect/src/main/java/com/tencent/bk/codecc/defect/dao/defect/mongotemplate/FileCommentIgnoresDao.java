package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.mongodb.client.result.DeleteResult;
import com.tencent.bk.codecc.defect.model.ignore.FileCommentIgnoresEntity;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * FileCommentIgnoresEntity 的 DAO
 *
 * @date 2025/05/27
 */
@Repository
public class FileCommentIgnoresDao {
    private static final int BATCH_SIZE = 2000;
    @Autowired
    private MongoTemplate defectMongoTemplate;

    public void insertAll(List<FileCommentIgnoresEntity> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return;
        }

        if (entities.size() <= BATCH_SIZE) {
            defectMongoTemplate.insert(entities, FileCommentIgnoresEntity.class);
            return;
        }

        for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, entities.size());
            List<FileCommentIgnoresEntity> batch = entities.subList(i, end);
            defectMongoTemplate.insert(batch, FileCommentIgnoresEntity.class);
        }
    }

    public Long countByTaskId(Long taskId) {
        if (taskId == null) {
            return null;
        }

        Criteria criteria = Criteria.where("task_id").is(taskId);
        Query query = new Query(criteria);
        return defectMongoTemplate.count(query, FileCommentIgnoresEntity.class);
    }

    public List<FileCommentIgnoresEntity> findByTaskId(Long taskId) {
        Criteria criteria = Criteria.where("task_id").is(taskId);
        Query query = new Query(criteria);
        return defectMongoTemplate.find(query, FileCommentIgnoresEntity.class);
    }

    public List<FileCommentIgnoresEntity> findByTaskIdAndEntityIdBiggerThan(
            Long taskId,
            String startId,
            int batchSize
    ) {
        Criteria criteria = Criteria.where("task_id").is(taskId);
        if (StringUtils.isNotBlank(startId)) {
            criteria.and("_id").gt(startId);
        }
        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Direction.ASC, "_id"));
        query.limit(batchSize);

        return defectMongoTemplate.find(query, FileCommentIgnoresEntity.class);
    }

    public long deleteByTaskIdAndFilePathIn(Long taskId, Set<String> filePaths) {
        if (taskId == null) {
            return 0;
        }
        Criteria criteria = Criteria.where("task_id").is(taskId);
        if (CollectionUtils.isNotEmpty(filePaths)) {
            criteria.and("file_path").in(filePaths);
        }

        Query query = new Query(criteria);

        DeleteResult result = defectMongoTemplate.remove(query, FileCommentIgnoresEntity.class);
        return result.getDeletedCount();
    }

}
