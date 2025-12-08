package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.IgnoredNegativeDefectRepository;
import com.tencent.bk.codecc.defect.model.defect.IgnoredNegativeDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.ListNegativeDefectReqVO;
import com.tencent.bk.codecc.defect.vo.ProcessNegativeDefectReqVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 以 "误报" 为理由被忽略的告警的持久层代码
 *
 * @date 2024/01/14
 */
@Slf4j
@Repository
public class IgnoredNegativeDefectDao {

    @Autowired
    IgnoredNegativeDefectRepository ignoredNegativeDefectRepository;

    @Autowired
    private MongoTemplate defectMongoTemplate;

    private final List<String> ORG_FIELDS = Arrays.asList("bg_id", "dept_id", "center_id");

    /**
     * 更新误报告警的处理进展
     *
     * @date 2024/3/9
     * @param entityId
     * @param processNegativeDefectReq
     * @return java.lang.Boolean
     */
    public Boolean updateProcessProgressByDefectId(
            String entityId,
            ProcessNegativeDefectReqVO processNegativeDefectReq
    ) {
        Update update = new Update().set("process_progress", processNegativeDefectReq.getProcessProgress());

        if (processNegativeDefectReq.getProcessProgress().equals(ComConstants.ProcessProgressType.NONEED.value())
                || processNegativeDefectReq.getProcessProgress().equals(ComConstants.ProcessProgressType.OTHER.value())
        ) {
            update.set("process_reason_type", processNegativeDefectReq.getProcessReasonType());
        }

        List<Integer> needIssueLink = Arrays.asList(
                ComConstants.ProcessProgressType.FIXED.value(),
                ComConstants.ProcessProgressType.FOLLOWING.value()
        );
        if (needIssueLink.contains(processNegativeDefectReq.getProcessProgress())
                && processNegativeDefectReq.getIssueLink() != null) {
            update.set("issue_link", processNegativeDefectReq.getIssueLink());
        }

        if (processNegativeDefectReq.getProcessReason() != null) {
            update.set("process_reason", processNegativeDefectReq.getProcessReason());
        }

        Criteria criteria = Criteria.where("_id").is(entityId);
        Query query = Query.query(criteria);
        UpdateResult result = defectMongoTemplate.updateFirst(query, update, IgnoredNegativeDefectEntity.class);

        return result.getModifiedCount() > 0;
    }

    /**
     * 生成 mongodb 的过滤条件 (比较复杂的进阶条件)
     *
     * @date 2024/2/20
     * @param criteria
     * @param listNegativeDefectReq
     * @return void
     */
    private void genCriteria(Criteria criteria, ListNegativeDefectReqVO listNegativeDefectReq) {
        if (CollectionUtils.isNotEmpty(listNegativeDefectReq.getCheckerNames())) {
            criteria.and("checker_name").in(listNegativeDefectReq.getCheckerNames());
        }

        if (CollectionUtils.isNotEmpty(listNegativeDefectReq.getSeverities())) {
            criteria.and("severity").in(listNegativeDefectReq.getSeverities());
        }

        if (CollectionUtils.isNotEmpty(listNegativeDefectReq.getCreateFroms())) {
            criteria.and("create_from").in(listNegativeDefectReq.getCreateFroms());
        }

        if (CollectionUtils.isNotEmpty(listNegativeDefectReq.getProcessProgresses())) {
            criteria.and("process_progress").in(listNegativeDefectReq.getProcessProgresses());
        }

        if (listNegativeDefectReq.getOrganizationIds() != null
                && !listNegativeDefectReq.getOrganizationIds().isEmpty()) {
            List<Criteria> orCriList = new ArrayList<>();
            for (List<Integer> ids : listNegativeDefectReq.getOrganizationIds()) {
                Criteria andCri = new Criteria();
                for (int i = 0; i < ids.size() && i < ORG_FIELDS.size(); i++) {
                    andCri.and(ORG_FIELDS.get(i)).is(ids.get(i));
                }
                if (ids.size() > 0) {
                    orCriList.add(andCri);
                }
            }

            Criteria subCri = new Criteria();
            subCri.orOperator(orCriList);

            criteria.andOperator(subCri);
        }
    }

    public Long countEntitiesAfterFilter(
            String toolName,
            Long startTime,
            Long endTime,
            ListNegativeDefectReqVO listNegativeDefectReq
    ) {
        Criteria criteria = new Criteria();
        criteria.and("tool_name").is(toolName).and("ignore_time").gt(startTime).lt(endTime);
        genCriteria(criteria, listNegativeDefectReq);

        Query query = Query.query(criteria);

        return defectMongoTemplate.count(query, IgnoredNegativeDefectEntity.class);
    }

    /**
     * 列表系列接口的 DAO 层过滤主逻辑
     *
     * @date 2024/3/11
     * @param toolName
     * @param startTime
     * @param endTime
     * @param lastInd
     * @param pageSize
     * @param orderBy
     * @param orderDirection
     * @param listNegativeDefectReq
     * @return java.util.List<com.tencent.bk.codecc.defect.model.defect.IgnoredNegativeDefectEntity>
     */
    public List<IgnoredNegativeDefectEntity> queryEntitiesAfterFilter(
            String toolName,
            Long startTime,
            Long endTime,
            String lastInd,
            Integer pageSize,
            String orderBy,
            String orderDirection,
            ListNegativeDefectReqVO listNegativeDefectReq
    ) {
        Criteria criteria = new Criteria();
        criteria.and("tool_name").is(toolName).and("ignore_time").gt(startTime).lt(endTime);
        if (StringUtils.isNotBlank(lastInd)) {
            if (orderBy.equals("ignoreTime") && orderDirection.equals("ASC")) {
                criteria.and("_id").gt(new ObjectId(lastInd));
            } else {
                criteria.and("_id").lt(new ObjectId(lastInd));
            }
        }
        genCriteria(criteria, listNegativeDefectReq);

        Query query = Query.query(criteria);
        if (StringUtils.isNotBlank(orderBy) && orderBy.equals("ignoreTime") && orderDirection.equals("ASC")) {
            query.with(Sort.by(Sort.Direction.ASC, "_id"));
        } else {
            query.with(Sort.by(Sort.Direction.DESC, "_id"));
        }

        if (pageSize == null || pageSize < 0) {
            pageSize = ComConstants.SMALL_PAGE_SIZE;
        } else if (pageSize > ComConstants.COMMON_PAGE_SIZE) {
            pageSize = ComConstants.COMMON_PAGE_SIZE;
        }
        query.limit(pageSize);

        return defectMongoTemplate.find(query, IgnoredNegativeDefectEntity.class);
    }

    public List<IgnoredNegativeDefectEntity> queryByToolNameAndPeriod(
            String toolName,
            Long startTime,
            Long endTime,
            List<String> includeFields
    ) {
        Criteria criteria = new Criteria();
        criteria.and("tool_name").is(toolName)
                .and("ignore_time").gt(startTime).lt(endTime);
        Query query = Query.query(criteria);

        if (CollectionUtils.isNotEmpty(includeFields)) {
            includeFields.forEach(field -> query.fields().include(field));
        }

        return defectMongoTemplate.find(query, IgnoredNegativeDefectEntity.class);
    }

    public long batchDelete(Set<String> defectKeySet) {
        Criteria criteria = Criteria.where("defect_id").in(defectKeySet);
        Query query = Query.query(criteria);

        DeleteResult deleteResult = defectMongoTemplate.remove(query, IgnoredNegativeDefectEntity.class);

        return deleteResult.getDeletedCount();
    }

    /**
     * 批量插入
     *
     * @date 2024/3/11
     * @param defectList
     * @param batchDefectProcessReqVO
     * @param taskDetail
     * @return void
     */
    public void batchInsert(
            List<LintDefectV2Entity> defectList,
            BatchDefectProcessReqVO batchDefectProcessReqVO,
            TaskDetailVO taskDetail
    ) {
        List<IgnoredNegativeDefectEntity> ignoredEntities = new ArrayList<>();
        for (LintDefectV2Entity entity : defectList) {
            IgnoredNegativeDefectEntity newEntity = new IgnoredNegativeDefectEntity();

            newEntity.setDefectId(entity.getEntityId());
            newEntity.setToolName(entity.getToolName());
            newEntity.setProjectName(batchDefectProcessReqVO.getProjectId());
            newEntity.setTaskNameCn(taskDetail.getNameCn());
            newEntity.setTaskNameEn(taskDetail.getNameEn());
            newEntity.setUrl(entity.getUrl());
            newEntity.setLineNum(entity.getLineNum());
            newEntity.setChecker(entity.getChecker());
            newEntity.setMessage(entity.getMessage());
            newEntity.setSeverity(entity.getSeverity());
            newEntity.setIgnoreAuthor(batchDefectProcessReqVO.getIgnoreAuthor());
            newEntity.setIgnoreTime(System.currentTimeMillis());
            newEntity.setIgnoreReason(batchDefectProcessReqVO.getIgnoreReason());
            newEntity.setIgnoreReasonType(batchDefectProcessReqVO.getIgnoreReasonType());
            newEntity.setCreateFrom(taskDetail.getTaskType());
            newEntity.setFilePath(entity.getFilePath());
            newEntity.setFileName(entity.getFileName());
            newEntity.setDefectInstances(entity.getDefectInstances());
            newEntity.setRelPath(entity.getRelPath());
            newEntity.setBranch(entity.getBranch());
            newEntity.setBgId(taskDetail.getBgId());
            newEntity.setDeptId(taskDetail.getDeptId());
            newEntity.setCenterId(taskDetail.getCenterId());
            newEntity.setFileLink(PathUtils.getFileUrl(newEntity.getUrl(), newEntity.getBranch(),
                    newEntity.getRelPath()));

            ignoredEntities.add(newEntity);
        }

        ignoredNegativeDefectRepository.saveAll(ignoredEntities);
    }

}
