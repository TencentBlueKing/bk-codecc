package com.tencent.bk.codecc.defect.dao.core.mongotemplate;

import com.tencent.devops.common.constant.IgnoreApprovalConstants;
import com.tencent.devops.common.constant.IgnoreApprovalConstants.ProjectScopeType;
import com.tencent.bk.codecc.defect.model.ignore.IgnoreApprovalConfigEntity;
import com.tencent.codecc.common.db.MongoPageHelper;
import com.tencent.devops.common.constant.ComConstants;
import java.util.ArrayList;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public class IgnoreApprovalConfigDao {

    @Autowired
    private MongoTemplate defectCoreMongoTemplate;


    public List<IgnoreApprovalConfigEntity> findSingleProjectMatchConfig(String projectId, List<String> dimensions,
            List<Integer> severities, List<Integer> ignoreTypeIds) {
        // 查询是否有条件存在重合
        Criteria criteria =
                Criteria.where("project_scope_type").is(IgnoreApprovalConstants.ProjectScopeType.SINGLE.type())
                        .and("project_id").is(projectId)
                        .and("dimensions").in(dimensions)
                        .and("severities").in(severities)
                        .and("ignore_type_ids").in(ignoreTypeIds)
                        .and("status").is(ComConstants.Status.ENABLE.value());
        return defectCoreMongoTemplate.find(Query.query(criteria), IgnoreApprovalConfigEntity.class);
    }

    public void save(IgnoreApprovalConfigEntity configEntity) {
        if (configEntity == null) {
            return;
        }
        defectCoreMongoTemplate.save(configEntity);
    }

    public void updateStatus(String approvalConfigId, Integer status) {
        Criteria criteria = Criteria.where("_id").is(new ObjectId(approvalConfigId));
        Update update = Update.update("status", status);
        defectCoreMongoTemplate.updateFirst(Query.query(criteria), update, IgnoreApprovalConfigEntity.class);
    }

    public List<IgnoreApprovalConfigEntity> findConfigByProjectScopeTypeByPage(ProjectScopeType projectScopeType,
            String projectId, Integer pageNum, Integer pageSize) {
        Criteria criteria = getVaildProjectScopeCriteria(projectScopeType, projectId);
        pageNum = pageNum == null || pageNum <= 0 ? MongoPageHelper.FIRST_PAGE_NUM : pageNum;
        pageSize = pageSize == null || pageSize <= 0 ? ComConstants.SMALL_PAGE_SIZE : pageSize;
        Query query = Query.query(criteria);
        query.skip((long) (pageNum - 1) * pageSize);
        query.limit(pageSize);
        return defectCoreMongoTemplate.find(query, IgnoreApprovalConfigEntity.class);
    }

    public Long getProjectScopeCount(ProjectScopeType projectScopeType, String projectId) {
        Criteria criteria = getVaildProjectScopeCriteria(projectScopeType, projectId);
        return defectCoreMongoTemplate.count(Query.query(criteria), IgnoreApprovalConfigEntity.class);
    }

    private Criteria getVaildProjectScopeCriteria(ProjectScopeType projectScopeType, String projectId) {
        List<Criteria> criList = new ArrayList<>();
        criList.add(Criteria.where("project_scope_type").is(projectScopeType.type())
                .and("status").is(ComConstants.Status.ENABLE.value()));
        if (projectScopeType == ProjectScopeType.SINGLE) {
            criList.add(Criteria.where("project_id").is(projectId));
        } else {
            criList.add(new Criteria().orOperator(
                    Criteria.where("limited_project_id").exists(false),
                    Criteria.where("limited_project_id").is(projectId)
            ));
        }
        return new Criteria().andOperator(criList.toArray(new Criteria[]{}));
    }

    public List<IgnoreApprovalConfigEntity> findProjectConfig(ProjectScopeType projectScopeType, String projectId,
            Integer ignoreTypeId, List<String> dimensions, List<Integer> severities) {
        Criteria criteria = getVaildProjectScopeCriteria(projectScopeType, projectId);
        if (ignoreTypeId != null) {
            criteria.and("ignore_type_ids").is(ignoreTypeId);
        }
        if (CollectionUtils.isNotEmpty(dimensions)) {
            criteria.and("dimensions").in(dimensions);
        }
        if (CollectionUtils.isNotEmpty(severities)) {
            criteria.and("severities").in(severities);
        }
        return defectCoreMongoTemplate.find(Query.query(criteria), IgnoreApprovalConfigEntity.class);
    }

    /**
     * 根据任务范围类型和项目编号分页查询忽略审批配置
     * @param taskScopeType 任务类型
     * @param projectId 项目编号
     * @param pageable 分页配置
     * @return page
     */
    public Page<IgnoreApprovalConfigEntity> findProjectConfigPage(String taskScopeType, String projectId,
                                                                  Pageable pageable) {
        Criteria criteria = new Criteria();
        if (StringUtils.isNotEmpty(taskScopeType)) {
            criteria.and("task_scope_type").is(taskScopeType);
        }
        if (StringUtils.isNotEmpty(projectId)) {
            criteria.and("project_id").is(projectId);
        }
        criteria.and("status").is(ComConstants.Status.ENABLE.value());
        Query query = Query.query(criteria);

        long totalCount = defectCoreMongoTemplate.count(query, IgnoreApprovalConfigEntity.class);
        // 分页排序
        query.with(pageable);

        List<IgnoreApprovalConfigEntity> result = defectCoreMongoTemplate.find(query, IgnoreApprovalConfigEntity.class);
        return new PageImpl<>(result, pageable, totalCount);
    }
}
