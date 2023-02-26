package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.defect.GithubIssueDefectEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Github Issueu数据表操作类
 *
 * @author warmli
 * @date 2020/06/08
 */
@Repository
public class GithubStatDefectDao {
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据任务 ID 和 Issue 的 ID，更新 Github Issue 数据
     *
     * @param githubStatDefectList 待更新列表
     */
    public void upsertDefectList(List<GithubIssueDefectEntity> githubStatDefectList) {
        if (githubStatDefectList.isEmpty()) {
            return;
        }
        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, GithubIssueDefectEntity.class);
        for (GithubIssueDefectEntity githubIssueDefectEntity : githubStatDefectList) {
            Query query = new Query();
            query.addCriteria(Criteria.where("task_id").is(githubIssueDefectEntity.getTaskId())
                    .and("id").is(githubIssueDefectEntity.getId()));

            Update update = new Update();
            update.set("task_id", githubIssueDefectEntity.getTaskId());
            update.set("id", githubIssueDefectEntity.getId());
            update.set("url", githubIssueDefectEntity.getUrl());
            update.set("repository_url", githubIssueDefectEntity.getRepositoryUrl());
            update.set("title", githubIssueDefectEntity.getTitle());
            update.set("state", githubIssueDefectEntity.getState());
            update.set("created_at", githubIssueDefectEntity.getCreatedAt());
            update.set("closed_at", githubIssueDefectEntity.getClosedAt());
            update.set("updated_at", githubIssueDefectEntity.getUpdatedAt());
            update.set("created_timestamp", githubIssueDefectEntity.getCreatedTimestamp());
            update.set("closed_timestamp", githubIssueDefectEntity.getClosedTimestamp());
            update.set("updated_timestamp", githubIssueDefectEntity.getUpdatedTimestamp());
            update.set("creator", githubIssueDefectEntity.getCreator());
            update.set("closed_by", githubIssueDefectEntity.getClosedBy());
            update.set("assignees", githubIssueDefectEntity.getAssignees());
            ops.upsert(query, update);
        }
        ops.execute();
    }
}
