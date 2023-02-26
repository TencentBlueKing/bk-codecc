package com.tencent.bk.codecc.defect.dao.mongotemplate.file;

import com.tencent.bk.codecc.defect.model.file.ScmFileInfoSnapshotEntity;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
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

import java.util.List;

@Repository
@Slf4j
public class ScmFileInfoSnapshotDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 批量保存scm快照信息
     * @param entities
     */
    public void batchSave(List<ScmFileInfoSnapshotEntity> entities) {
        if (CollectionUtils.isNotEmpty(entities)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED,
                    ScmFileInfoSnapshotEntity.class);
            for (ScmFileInfoSnapshotEntity fileInfo : entities) {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id").is(fileInfo.getTaskId()));
                query.addCriteria(Criteria.where("build_id").is(fileInfo.getBuildId()));
                query.addCriteria(Criteria.where("file_path").is(fileInfo.getFilePath()));

                Update update = new Update();
                update.setOnInsert("rel_path", fileInfo.getRelPath())
                        .setOnInsert("md5", fileInfo.getMd5())
                        .setOnInsert("update_time", fileInfo.getUpdateTime())
                        .setOnInsert("repo_id", fileInfo.getRepoId())
                        .setOnInsert("scm_type", fileInfo.getScmType())
                        .setOnInsert("url", fileInfo.getUrl())
                        .setOnInsert("branch", fileInfo.getBranch())
                        .setOnInsert("revision", fileInfo.getRevision())
                        .setOnInsert("root_url", fileInfo.getRootUrl())
                        .setOnInsert("sub_module", fileInfo.getSubModule());
                ops.upsert(query, update);
            }
            ops.execute();
        }
    }

    /**
     * 根据条件聚合出最新的SCM快照信息
     *
     * @param taskId
     * @param filePaths
     * @return
     */
    public List<ScmFileInfoSnapshotEntity> aggByTaskIdAndFilePathInOrderByUpdateTimeDesc(
            long taskId,
            Set<String> filePaths
    ) {
        MatchOperation match = Aggregation.match(
                Criteria.where("task_id").is(taskId)
                        .and("file_path").in(filePaths)
        );
        GroupOperation group = Aggregation.group("task_id", "file_path")
                .first("task_id").as("task_id")
                .first("build_id").as("build_id")
                .first("file_path").as("file_path")
                .first("rel_path").as("rel_path")
                .first("md5").as("md5")
                .first("update_time").as("update_time")
                .first("repo_id").as("repo_id")
                .first("scm_type").as("scm_type")
                .first("url").as("url")
                .first("branch").as("branch")
                .first("revision").as("revision")
                .first("root_url").as("root_url")
                .first("sub_module").as("sub_module");
        SortOperation sort = Aggregation.sort(Direction.DESC, "update_time");
        AggregationResults<ScmFileInfoSnapshotEntity> aggregate = mongoTemplate.aggregate(
                Aggregation.newAggregation(match, sort, group),
                "t_scm_file_info_snapshot",
                ScmFileInfoSnapshotEntity.class
        );

        return aggregate.getMappedResults();
    }
}
