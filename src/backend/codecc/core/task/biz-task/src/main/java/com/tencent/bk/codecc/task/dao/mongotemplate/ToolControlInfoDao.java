package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.tencent.bk.codecc.task.model.ToolControlInfoEntity;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 操作 t_tool_control 的 Dao
 *
 * @date 2024/08/14
 */
@Repository
@Slf4j
public class ToolControlInfoDao {
    @Autowired
    private MongoTemplate mongoTemplate;
    private static final String COLLECTION_NAME = "t_tool_control";

    public Boolean save(ToolControlInfoEntity entity) {
        mongoTemplate.save(entity, COLLECTION_NAME);

        return true;
    }

    public List<ToolControlInfoEntity> findGrayToolNameIn(List<String> toolNames) {
        Criteria criteria = Criteria.where("tool_name").in(toolNames)
                .and("status").is(ComConstants.ToolIntegratedStatus.G.value());
        Query query = new Query(criteria);

        return mongoTemplate.find(query, ToolControlInfoEntity.class);
    }

    public ToolControlInfoEntity findByToolNameAndStatus(String toolName, Integer status) {
        Criteria criteria = Criteria.where("tool_name").is(toolName);
        if (status != null) {
            criteria.and("status").is(status);
        }

        Query query = new Query(criteria);

        return mongoTemplate.findOne(query, ToolControlInfoEntity.class);
    }

    public Boolean interruptRelease(String toolName) {
        Criteria criteria = Criteria.where("tool_name").is(toolName);
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("status", ComConstants.ToolIntegratedStatus.T.value());
        Set<String> emptySet = new HashSet<>();
        update.set("grayed_org_ids", emptySet);
        mongoTemplate.updateFirst(query, update, ToolControlInfoEntity.class);

        return true;
    }

    public Boolean updateTestVersion(String toolName, String testVersion, String updatedBy) {
        Criteria criteria = Criteria.where("tool_name").is(toolName);
        Query query = new Query(criteria);

        Update update = new Update();
        update.set("test_version", testVersion);
        update.set("updated_date", System.currentTimeMillis());
        update.set("updated_by", updatedBy);

        mongoTemplate.updateFirst(query, update, ToolControlInfoEntity.class);

        return true;
    }

    public List<ToolControlInfoEntity> findByGrayedOrgIdIn(List<String> orgIds, Set<String> excludeToolNames) {
        Criteria criteria = Criteria.where("grayed_org_ids").in(orgIds)
                .and("status").is(ComConstants.ToolIntegratedStatus.G.value())
                .and("tool_name").nin(excludeToolNames);

        Query query = new Query(criteria);

        return mongoTemplate.find(query, ToolControlInfoEntity.class);
    }

    public Boolean updateVisibleRangeByToolName(
            String toolName,
            Set<String> visibleProjects,
            Set<String> visibleOrgIds
    ) {
        Criteria criteria = Criteria.where("tool_name").is(toolName);
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("visible_projects", visibleProjects);
        update.set("visible_org_ids", visibleOrgIds);

        mongoTemplate.updateFirst(query, update, ToolControlInfoEntity.class);

        return true;
    }

}
