package com.tencent.bk.codecc.defect.dao.defect.mongotemplate.file;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.model.file.ScmFileInfoCacheEntity;
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

@Repository
@Slf4j
public class ScmFileInfoCacheDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 批量保存scm缓存信息
     *
     * @param fileInfoEntities
     */
    public void batchSave(List<ScmFileInfoCacheEntity> fileInfoEntities) {
        if (CollectionUtils.isEmpty(fileInfoEntities)) {
            return;
        }

        long changeRecordSum = fileInfoEntities.stream()
                .filter(x -> CollectionUtils.isNotEmpty(x.getChangeRecords()))
                .mapToLong(y -> y.getChangeRecords().size())
                .sum();
        int lowestPageSize = 100;
        int finalPageSize = changeRecordSum > fileInfoEntities.size() * 10L ? lowestPageSize
                : ComConstants.SCM_FILE_INFO_CACHE_BATCH_PAGE_SIZE;
        if (finalPageSize == lowestPageSize) {
            log.info("trigger lowest page size, task id: {}", fileInfoEntities.get(0).getTaskId());
        }

        List<List<ScmFileInfoCacheEntity>> partitionList = Lists.partition(fileInfoEntities, finalPageSize);

        for (List<ScmFileInfoCacheEntity> onePartition : partitionList) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ScmFileInfoCacheEntity.class);
            for (ScmFileInfoCacheEntity fileInfo : onePartition) {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id").is(fileInfo.getTaskId()));
                query.addCriteria(Criteria.where("tool_name").is(fileInfo.getToolName()));
                query.addCriteria(Criteria.where("file_rel_path").is(fileInfo.getFileRelPath()));

                Update update = new Update();
                update.set("file_md5", fileInfo.getMd5())
                        .set("file_update_time", fileInfo.getFileUpdateTime())
                        .set("file_author", fileInfo.getFileAuthor())
                        .set("branch", fileInfo.getBranch())
                        .set("file_path", fileInfo.getFilePath())
                        .set("file_rel_path", fileInfo.getFileRelPath())
                        .set("revision", fileInfo.getRevision())
                        .set("scm_type", fileInfo.getScmType())
                        .set("url", fileInfo.getUrl())
                        .set("root_url", fileInfo.getRootUrl())
                        .set("build_id", fileInfo.getBuildId())
                        .set("change_records", fileInfo.getChangeRecords());

                ops.upsert(query, update);
            }
            ops.execute();
        }
    }
}
