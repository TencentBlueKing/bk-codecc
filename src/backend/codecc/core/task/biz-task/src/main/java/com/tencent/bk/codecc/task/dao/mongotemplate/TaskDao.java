/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.model.DeletedTaskInfoEntity;
import com.tencent.bk.codecc.task.model.TaskIdInfo;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.TaskOrgInfoEntity;
import com.tencent.bk.codecc.task.model.UserLogInfoStatEntity;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.bk.codecc.task.vo.TaskProjectCountVO;
import com.tencent.bk.codecc.task.vo.TaskStatisticVO;
import com.tencent.codecc.common.db.utils.TaskCreateFromUtils;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import com.tencent.devops.common.constant.ComConstants.Status;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.COMMON_PAGE_SIZE;

/**
 * 任务持久层代码
 */
@Repository
@Slf4j
public class TaskDao implements CommonTaskDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    public Boolean updateTask(long taskId, Long codeLang, String nameCn, List<String> taskOwner,
            List<String> taskMember, String disableTime, int status, String userName) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId))
                .addCriteria(Criteria.where("status").is(TaskConstants.TaskStatus.ENABLE.value()));
        Update update = new Update();
        if (null != codeLang) {
            update.set("code_lang", codeLang);
        }
        if (StringUtils.isNotBlank(nameCn)) {
            update.set("name_cn", nameCn);
        }
        if (!CollectionUtils.isEmpty(taskOwner)) {
            update.set("task_owner", taskOwner);
        }
        if (!CollectionUtils.isEmpty(taskMember)) {
            update.set("task_member", taskMember);
        }
        if (StringUtils.isNotBlank(disableTime)) {
            update.set("disable_time", disableTime);
        }
        update.set("status", status);
        update.set("updated_date", System.currentTimeMillis());
        update.set("updated_by", userName);
        return mongoTemplate.updateMulti(query, update, TaskInfoEntity.class).getModifiedCount() > 0;
    }

    /**
     * 更新仓库所有者
     *
     * @param taskId
     * @param repoOwners
     * @param userName
     * @return
     */
    public Boolean updateRepoOwner(Long taskId, List<String> repoOwners, String userName) {
        Update update = Update.update("repo_owner", repoOwners)
                .set("updated_date", System.currentTimeMillis())
                .set("updated_by", userName);
        Query query = new Query(Criteria.where("task_id").is(taskId));
        mongoTemplate.updateFirst(query, update, TaskInfoEntity.class);
        return true;
    }

    /**
     * 更新流水线插件信息
     *
     * @param taskId
     * @param pipelineTaskId
     * @param pipelineTaskName
     * @param userName
     * @return
     */
    public Boolean updatePipelineTaskInfo(Long taskId, String pipelineTaskId, String pipelineTaskName,
            String userName, Integer timeout, Boolean fileCacheEnable) {
        Update update = Update.update("pipeline_task_id", pipelineTaskId)
                .set("pipeline_task_name", pipelineTaskName)
                .set("timeout", timeout)
                .set("file_cache_enable", fileCacheEnable)
                .set("updated_date", System.currentTimeMillis())
                .set("updated_by", userName);
        Query query = new Query(Criteria.where("task_id").is(taskId));
        mongoTemplate.updateFirst(query, update, TaskInfoEntity.class);
        return true;
    }

    /**
     * 更新路径屏蔽
     *
     * @param pathInput
     * @param userName
     * @return
     */
    public Boolean updateFilterPath(FilterPathInputVO pathInput, String userName) {
        Update update = new Update();

        if (ComConstants.PATH_TYPE_DEFAULT.equalsIgnoreCase(pathInput.getPathType())) {
            update.set("default_filter_path", pathInput.getDefaultFilterPath());
        } else if (ComConstants.PATH_TYPE_CODE_YML.equalsIgnoreCase(pathInput.getPathType())) {
            update.set("test_source_filter_path", pathInput.getTestSourceFilterPath());
            update.set("auto_gen_filter_path", pathInput.getAutoGenFilterPath());
            update.set("third_party_filter_path", pathInput.getThirdPartyFilterPath());
            update.set("scan_test_source", pathInput.getScanTestSource());
        } else {
            update.set("filter_path", pathInput.getFilterDir());
        }

        update.set("updated_date", System.currentTimeMillis());
        update.set("updated_by", userName);
        Query query = new Query(Criteria.where("task_id").is(pathInput.getTaskId()));
        return mongoTemplate.updateMulti(query, update, TaskInfoEntity.class).getModifiedCount() > 0;
    }

    /**
     * 删除 taskInfoEntity
     *
     * @param taskInfoEntity
     * @param userName
     * @return boolean
     * @date 2023/10/31
     */
    public boolean deleteEntity(TaskInfoEntity taskInfoEntity, String userName) {
        DeletedTaskInfoEntity deletedTaskInfoEntity = new DeletedTaskInfoEntity();
        BeanUtils.copyProperties(taskInfoEntity, deletedTaskInfoEntity);
        deletedTaskInfoEntity.setDeleteBy(userName);
        deletedTaskInfoEntity.setDeleteDate(System.currentTimeMillis());
        try {
            mongoTemplate.save(deletedTaskInfoEntity);
        } catch (DataAccessException e) {
            log.error("error({}) happens when save data in t_deleted_task_detail(taskId: {})",
                    e.getLocalizedMessage(), taskInfoEntity.getTaskId());
            return false;
        } catch (Exception e) {
            log.error("unkown error happens when save data in t_deleted_task_detail(taskId: {}). message: {}, "
                    + "stack trace: {}", taskInfoEntity.getTaskId(), e.getMessage(), e.getStackTrace());
            return false;
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskInfoEntity.getTaskId()));
        try {
            mongoTemplate.remove(query, TaskInfoEntity.class);
        } catch (DataAccessException e) {
            log.error("error({}) happens when delete data from t_task_detail(taskId: {}).",
                    e.getLocalizedMessage(), taskInfoEntity.getTaskId());
            return false;
        } catch (Exception e) {
            log.error("unkown error happened when delete data from t_task_detail(taskId: {}). message: {}, "
                    + "stack trace: {}", taskInfoEntity.getTaskId(), e.getMessage(), e.getStackTrace());
            return false;
        }

        return true;
    }

    public Boolean updateEntity(TaskInfoEntity taskInfoEntity, String userName) {
        Update update = new Update();
        update.set("status", taskInfoEntity.getStatus());
        update.set("branch", taskInfoEntity.getBranch());
        update.set("repo_hash_id", taskInfoEntity.getRepoHashId());
        update.set("os_type", taskInfoEntity.getOsType());
        update.set("build_env", taskInfoEntity.getBuildEnv());
        update.set("alias_name", taskInfoEntity.getAliasName());
        update.set("project_build_type", taskInfoEntity.getProjectBuildType());
        update.set("project_build_command", taskInfoEntity.getProjectBuildCommand());
        update.set("execute_date", taskInfoEntity.getExecuteDate());
        update.set("execute_time", taskInfoEntity.getExecuteTime());
        update.set("disable_time", taskInfoEntity.getDisableTime());
        update.set("disable_reason", taskInfoEntity.getDisableReason());
        update.set("last_disable_task_info", taskInfoEntity.getLastDisableTaskInfo());
        update.set("updated_by", userName);
        update.set("updated_date", System.currentTimeMillis());
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskInfoEntity.getTaskId()));
        return mongoTemplate.updateFirst(query, update, TaskInfoEntity.class).getModifiedCount() > 0;
    }

    /**
     * 更新項目id或者流水线id
     *
     * @param projectId
     * @param pipelineId
     * @param taskId
     */
    public void updateProjectIdAndPipelineId(String projectId, String pipelineId, Long taskId) {
        if (StringUtils.isBlank(projectId) && StringUtils.isBlank(pipelineId)) {
            return;
        }
        Update update = new Update();
        if (StringUtils.isNotBlank(projectId)) {
            update.set("project_id", projectId);
        }
        if (StringUtils.isNotBlank(pipelineId)) {
            update.set("pipeline_id", pipelineId);
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId));
        mongoTemplate.updateFirst(query, update, TaskInfoEntity.class);
    }


    /**
     * 更新失效原因
     *
     * @param openSourceDisableReason
     * @param taskId
     */
    public void updateOpenSourceDisableReason(Integer openSourceDisableReason, Long taskId) {
        Update update = new Update();
        update.set("opensource_disable_reason", openSourceDisableReason);
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId));
        mongoTemplate.updateFirst(query, update, TaskInfoEntity.class);
    }


    /**
     * 触发扫描后更新动作
     *
     * @param nameCn
     * @param commitId
     * @param taskId
     */
    public void updateNameCnAndCommitId(String nameCn, String commitId, Long updatedDate, Long taskId) {
        if (StringUtils.isBlank(nameCn) && StringUtils.isBlank(commitId)) {
            return;
        }
        Update update = new Update();
        if (StringUtils.isNotBlank(nameCn)) {
            update.set("name_cn", nameCn);
        }
        if (StringUtils.isNotBlank(commitId)) {
            update.set("gongfeng_commit_id", commitId);
        }
        update.set("updated_date", updatedDate);
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId));
        mongoTemplate.updateFirst(query, update, TaskInfoEntity.class);
    }

    /**
     * 设置"通知"接收人类型
     *
     * @param taskIds
     * @param rtxReceiverType
     * @param emailReceiverType
     */
    public void updateNotifyReceiverType(Collection<Long> taskIds, String rtxReceiverType, String emailReceiverType) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").in(taskIds));

        Update update = new Update();
        if (StringUtils.isNotEmpty(rtxReceiverType)) {
            update.set("notify_custom_info.rtx_receiver_type", rtxReceiverType);
        }
        if (StringUtils.isNotEmpty(emailReceiverType)) {
            update.set("notify_custom_info.email_receiver_type", emailReceiverType);
        }

        mongoTemplate.updateMulti(query, update, TaskInfoEntity.class);
    }

    /**
     * 查询事业群下的部门ID集合
     *
     * @param bgId 事业群ID
     * @param createFrom 任务创建来源
     * @return deptList
     */
    public List<TaskInfoEntity> queryDeptId(Integer bgId, String createFrom) {
        Document fieldsObj = new Document();
        fieldsObj.put("dept_id", true);
        Query query = new BasicQuery(new Document(), fieldsObj);

        if (bgId != null) {
            query.addCriteria(Criteria.where("bg_id").is(bgId));
        }
        if (StringUtils.isNotEmpty(createFrom)) {
            query.addCriteria(Criteria.where("create_from").is(createFrom));
        }

        return mongoTemplate.find(query, TaskInfoEntity.class);
    }

    /**
     * 多条件查询任务列表(若所有条件都为空，则返回空，防止过载查询)
     *
     * @param status 任务状态
     * @param bgId 事业群ID
     * @param deptIds 部门ID列表（多选）
     * @param taskIds 任务ID列表（批量）
     * @param createFrom 创建来源（多选）
     * @return task list
     */
    public List<TaskInfoEntity> queryTaskInfoEntityList(Integer status, Integer bgId, Collection<Integer> deptIds,
            Collection<Long> taskIds, Collection<String> createFrom, String userId) {
        List<Criteria> criteriaList = Lists.newArrayList();
        if (status != null) {
            criteriaList.add(Criteria.where("status").is(status));
        }
        // 指定批量任务
        if (!CollectionUtils.isEmpty(taskIds)) {
            criteriaList.add(Criteria.where("task_id").in(taskIds));
        }
        // 事业群ID筛选
        if (bgId != null && bgId != 0) {
            criteriaList.add(Criteria.where("bg_id").is(bgId));
        }
        // 部门ID筛选
        if (!CollectionUtils.isEmpty(deptIds)) {
            criteriaList.add(Criteria.where("dept_id").in(deptIds));
        }
        // 创建来源筛选
        if (!CollectionUtils.isEmpty(createFrom)) {
            criteriaList.add(Criteria.where("create_from").in(createFrom));
        }
        if (StringUtils.isNotBlank(userId)) {
            criteriaList.add(Criteria.where("task_members").in(userId));
        }

        // 若所有条件都为空，则返回空，防止过载查询
        if (CollectionUtils.isEmpty(criteriaList)) {
            return Collections.emptyList();
        }
        Criteria criteria = new Criteria();
        criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        Query query = new Query(criteria);

        query.fields().exclude("execute_time","execute_date", "timer_expression", "last_disable_task_info",
                "default_filter_path", "tool_config_info_list", "test_source_filter_path",
                "white_paths", "project_build_command", "third_party_filter_path", "auto_gen_filter_path");
        return mongoTemplate.find(query, TaskInfoEntity.class);
    }


    /**
     * 更新组织架构信息
     *
     * @return result
     */
    public Boolean updateOrgInfo(Long taskId, List<String> taskOwner, Integer bgId, Integer businessLineId,
            Integer deptId, Integer centerId, Integer groupId) {
        if (null == taskId || null == bgId) {
            log.error("param id is null: taskId[{}], bgId[{}]", taskId, bgId);
            return false;
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId));

        Update update = new Update();
        update.set("bg_id", bgId);
        // 其它有可能为null，需要更新
        update.set("business_line_id", businessLineId);
        update.set("dept_id", deptId);
        update.set("center_id", centerId);
        update.set("group_id", groupId);

        if (CollectionUtils.isNotEmpty(taskOwner)) {
            update.set("task_owner", taskOwner);
        }

        return mongoTemplate.updateFirst(query, update, TaskInfoEntity.class).getModifiedCount() > 0;
    }

    /**
     * 根据自定义条件获取taskId信息
     *
     * @param customParam 匹配自定义参数（is(customParam) in(customParam)不能为 null 或者 empty
     * @param nCustomParam 不匹配自定义参数
     */
    public List<TaskInfoEntity> queryTaskInfoByCustomParam(Map<String, Object> customParam,
            Map<String, Object> nCustomParam) {
        if (customParam == null || customParam.isEmpty()) {
            throw new IllegalArgumentException("查询条件不能为空");
        }

        Criteria criteria;
        List<String> fields = Lists.newArrayList(customParam.keySet());
        if (customParam.get(fields.get(0)) instanceof Collection) {
            criteria = Criteria.where(fields.get(0)).in(customParam.get(fields.get(0)));
        } else {
            criteria = Criteria.where(fields.get(0)).is(customParam.get(fields.get(0)));
        }

        fields.stream()
                .skip(1)
                .forEach(field -> {
                    if (customParam.get(field) instanceof Collection) {
                        criteria.and(field).in(customParam.get(field));
                    } else {
                        criteria.and(field).is(customParam.get(field));
                    }
                });

        fields = Lists.newArrayList(nCustomParam.keySet());
        fields.forEach(field -> {
            if (nCustomParam.get(field) instanceof Collection) {
                criteria.and(field).nin(nCustomParam.get(field));
            } else {
                criteria.and(field).ne(nCustomParam.get(field));
            }
        });

        Query query = new Query();
        query.addCriteria(criteria);
        return mongoTemplate.find(query, TaskInfoEntity.class);
    }

    /**
     * 通过来源/状态/项目id/工具名查找
     *
     * @param createFrom
     * @param status
     * @param projectId
     * @param toolNames
     * @return
     */
    public List<TaskInfoEntity> findByCreateFromAndStatusAndProjectIdContainingAndToolNamesContaining(String createFrom,
            Integer status,
            String projectId,
            String toolNames,
            Integer skip,
            Integer limit) {
        Query query = new Query();
        query.addCriteria(Criteria.where("create_from").is(createFrom))
                .addCriteria(Criteria.where("status").is(status))
                .addCriteria(Criteria.where("project_id").regex(projectId))
                .addCriteria(Criteria.where("tool_names").regex(toolNames));
        query.skip(skip).limit(limit);
        return mongoTemplate.find(query, TaskInfoEntity.class);
    }

    /**
     * 通过来源/状态/项目id/语言查找
     *
     * @param createFrom
     * @param status
     * @param projectId
     * @return
     */
    public List<TaskInfoEntity> findByCreateFromAndStatusAndProjectIdContaining(String createFrom,
            Integer status,
            String projectId,
            Integer skip,
            Integer limit) {
        Query query = new Query();
        query.addCriteria(Criteria.where("create_from").is(createFrom))
                .addCriteria(Criteria.where("status").is(status))
                .addCriteria(Criteria.where("project_id").regex(projectId));
        query.skip(skip).limit(limit);
        return mongoTemplate.find(query, TaskInfoEntity.class);
    }

    /**
     * 查找工蜂开源扫描的任务
     *
     * @param skip
     * @param limit
     * @return
     */
    public List<TaskInfoEntity> getGongfengScanTask(Integer skip, Integer limit) {
        Query query = new Query();
        query.addCriteria(Criteria.where("create_from").is(BsTaskCreateFrom.GONGFENG_SCAN.value()))
                .addCriteria(Criteria.where("status").is(TaskConstants.TaskStatus.ENABLE.value()))
                .addCriteria(Criteria.where("project_id").regex("CODE_"));
        query.skip(skip).limit(limit);
        query.fields().include("code_lang", "gongfeng_project_id", "task_id");

        return mongoTemplate.find(query, TaskInfoEntity.class);
    }

    /**
     * 利用游标技术批量查找工蜂开源扫描任务
     *
     * @date 2024/8/1
     * @param skipTaskId 以 taskId 作为游标
     * @param limit
     * @return java.util.List<com.tencent.bk.codecc.task.model.TaskInfoEntity>
     */
    public List<TaskInfoEntity> getGongfengScanTaskByCursor(Long skipTaskId, Integer limit) {
        Criteria criteria = new Criteria();
        criteria.and("create_from").is(BsTaskCreateFrom.GONGFENG_SCAN.value());
        criteria.and("status").is(TaskConstants.TaskStatus.ENABLE.value());
        criteria.and("project_id").regex("^CODE_");
        if (skipTaskId != null) {
            criteria.and("task_id").gt(skipTaskId);
        }

        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Direction.ASC, "task_id"));
        query.limit(limit);
        query.fields().include("code_lang", "gongfeng_project_id", "task_id");

        return mongoTemplate.find(query, TaskInfoEntity.class);
    }

    /**
     * 更新路径白名单
     *
     * @param taskId
     * @param pathList
     */
    public boolean upsertPathOfTask(long taskId, List<String> pathList) {
        Query query = new Query(Criteria.where("task_id").is(taskId));
        Update update = new Update();
        update.set("white_paths", pathList);
        return mongoTemplate.upsert(query, update, TaskInfoEntity.class).getModifiedCount() > 0;
    }

    public List<TaskInfoEntity> findByCodeccNameCn(String projectId, String nameCn, Long offset, Long limit) {
        Query query = new Query();
        query.addCriteria(Criteria.where("project_id").regex(projectId));
        query.addCriteria(Criteria.where("nameCn").regex(nameCn));

        if (offset != null && limit != null) {
            query.skip(Math.toIntExact(offset)).limit(Math.toIntExact(limit));
        }

        return mongoTemplate.find(query, TaskInfoEntity.class, "t_task_detail");
    }

    /**
     * 按创建来源查询任务ID,排除指定项目id
     *
     * @param status 任务状态
     * @param createFrom 任务来源
     * @param projectIdSet 需排除的项目id
     * @return list
     */
    public List<TaskIdInfo> findTaskIdList(int status, List<String> createFrom, Set<String> projectIdSet,
            Pageable pageable) {
        Query query = new Query();
        query.fields().include("task_id");

        query.addCriteria(Criteria.where("status").is(status));

        Criteria criteriaByCreateFrom = TaskCreateFromUtils.getCriteriaByCreateFrom(createFrom);
        if (null != criteriaByCreateFrom) {
            query.addCriteria(criteriaByCreateFrom);
        }

        // 排除这些项目id的任务
        if (CollectionUtils.isNotEmpty(projectIdSet)) {
            query.addCriteria(Criteria.where("project_id").nin(projectIdSet));
        }

        if (null != pageable) {
            // 支持分页
            query.with(pageable);
        }
        return mongoTemplate.find(query, TaskIdInfo.class, "t_task_detail");
    }

    /**
     * 按创建来源查询任务ID
     *
     * @param status 任务状态
     * @param createFrom 任务来源
     * @return list
     */
    public List<TaskIdInfo> findTaskIdList(int status, List<String> createFrom) {
        return findTaskIdList(status, createFrom, null, null);
    }

    public List<TaskInfoEntity> findTaskIdByPage(int page, int pageSize, List<Integer> statusList) {
        Query query = Query.query(Criteria.where("status").in(statusList));
        query.fields().include("task_id", "project_id", "pipeline_id", "create_from");
        query.with(PageRequest.of(page, pageSize));
        return mongoTemplate.find(query, TaskInfoEntity.class, "t_task_detail");
    }

    /**
     * 设置task信息：notify_custom_info.report_job_name
     *
     * @param taskIdToJobNameMap
     */
    public void setDailyEmailJobName(Map<Long, String> taskIdToJobNameMap) {
        if (taskIdToJobNameMap == null || taskIdToJobNameMap.size() == 0) {
            return;
        }

        int batchSize = 1_0000;
        int counter = 0;
        BulkOperations ops = mongoTemplate.bulkOps(BulkMode.UNORDERED, TaskInfoEntity.class);

        for (Entry<Long, String> kv : taskIdToJobNameMap.entrySet()) {
            counter++;
            Long taskId = kv.getKey();
            String jobName = kv.getValue();
            Query query = new Query(Criteria.where("task_id").is(taskId));
            Update update = new Update();

            if (jobName != null) {
                update.set("notify_custom_info.report_job_name", jobName);
            } else {
                update.unset("notify_custom_info.report_job_name");
            }
            ops.updateOne(query, update);

            if (counter % batchSize == 0 || taskIdToJobNameMap.size() == counter) {
                ops.execute();
                ops = mongoTemplate.bulkOps(BulkMode.UNORDERED, TaskInfoEntity.class);
            }
        }
    }

    /**
     * 分页获取有效任务的项目id
     *
     * @param createFrom 来源
     * @param pageable 分页器
     * @return list
     */
    public List<String> findProjectIdPage(Set<String> createFrom, @NotNull Pageable pageable) {
        // 根据查询条件过滤
        Criteria criteria = Criteria.where("create_from").in(createFrom)
                .and("status").is(ComConstants.Status.ENABLE.value());
        MatchOperation match = Aggregation.match(criteria);

        // 以project_id进行分组
        GroupOperation group = Aggregation.group("project_id").first("project_id").as("project_id");

        SkipOperation skip = Aggregation.skip(Long.valueOf(pageable.getPageNumber() * pageable.getPageSize()));
        LimitOperation limit = Aggregation.limit(pageable.getPageSize());

        Aggregation agg = Aggregation.newAggregation(match, group, skip, limit)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());

        List<TaskInfoEntity> queryResult =
                mongoTemplate.aggregate(agg, "t_task_detail", TaskInfoEntity.class).getMappedResults();

        return CollectionUtils.isEmpty(queryResult) ? Collections.emptyList()
                : queryResult.stream().map(TaskInfoEntity::getProjectId).collect(Collectors.toList());
    }

    /**
     * 跟据项目id分页获取有效任务id
     *
     * @param projectId 项目id
     * @param pageable 分页器
     * @return list
     */
    public List<Long> findTaskIdPageByProjectId(String projectId, Pageable pageable) {
        Document fieldsObj = new Document();
        fieldsObj.put("task_id", true);

        Query query = new BasicQuery(new Document(), fieldsObj);
        query.addCriteria(
                Criteria.where("project_id").is(projectId)
                        .and("status").is(ComConstants.Status.ENABLE.value())
        );

        // 支持分页
        query.with(pageable);

        List<TaskIdInfo> taskIdInfoList = mongoTemplate.find(query, TaskIdInfo.class, "t_task_detail");
        return CollectionUtils.isEmpty(taskIdInfoList) ? Collections.emptyList()
                : taskIdInfoList.stream().map(TaskIdInfo::getTaskId).collect(Collectors.toList());
    }

    /**
     * 以"大于lastTaskId、升序"的形式分页获取task部分信息
     *
     * @param lastTaskId
     * @param limit
     * @return
     */
    public List<TaskInfoEntity> findTaskIdAndCreateFromByLastTaskIdWithPage(long lastTaskId, int limit) {
        Document fieldsObj = new Document();
        fieldsObj.put("task_id", true);
        fieldsObj.put("create_from", true);

        Query query = new BasicQuery(new Document(), fieldsObj);
        query.addCriteria(Criteria.where("task_id").gt(lastTaskId));
        query.with(Sort.by(Direction.ASC, "task_id"));
        query.limit(limit);

        return mongoTemplate.find(query, TaskInfoEntity.class);
    }

    /**
     * 更新任务信息
     */
    public void upsertOwnerAndOrgInfo(long taskId, List<String> ownerList, Long bgId, Long deptId, Long centerId,
            Long groupId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId));

        Update update = new Update();
        if (null != bgId && bgId > 0) {
            update.set("bg_id", bgId);
        }
        if (null != deptId && deptId > 0) {
            update.set("dept_id", deptId);
        }
        if (null != centerId && centerId > 0) {
            update.set("center_id", centerId);
        }
        if (null != groupId && groupId > 0) {
            update.set("group_id", groupId);
        }

        if (CollectionUtils.isNotEmpty(ownerList)) {
            update.set("task_owner", ownerList);
        }

        mongoTemplate.upsert(query, update, TaskInfoEntity.class);
    }

    /**
     * 分页查询任务详情组织架构信息
     *
     * @return page list
     */
    public List<TaskOrgInfoEntity> findByPage(Pageable pageable) {
        Query query = Query.query(Criteria.where("status").is(ComConstants.Status.ENABLE.value()));
        query.fields().include("task_id", "create_from", "project_id", "task_owner", "created_by", "bg_id", "dept_id",
                "business_line_id", "center_id", "group_id", "gongfeng_project_id");
        if (pageable != null) {
            query.with(pageable);
        }
        return mongoTemplate.find(query, TaskOrgInfoEntity.class, "t_task_detail");
    }

    /**
     * 批量更新任务详情表的组织架构信息
     *
     * @param needUpdateMap username, orgInfo
     */
    public void batchUpdateTaskOrgInfo(Map<Long, UserLogInfoStatEntity> needUpdateMap) {
        if (MapUtils.isNotEmpty(needUpdateMap)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, TaskInfoEntity.class);
            for (Map.Entry<Long, UserLogInfoStatEntity> entry : needUpdateMap.entrySet()) {

                UserLogInfoStatEntity userInfoEntity = entry.getValue();
                if (userInfoEntity == null || userInfoEntity.getBgId() == null) {
                    continue;
                }

                final Query query = Query.query(Criteria.where("task_id").is(entry.getKey()));

                Update update = new Update();
                update.set("bg_id", userInfoEntity.getBgId())
                        .set("dept_id", userInfoEntity.getDeptId())
                        .set("center_id", userInfoEntity.getCenterId())
                        .set("group_id", userInfoEntity.getGroupId());
                if (null != userInfoEntity.getBusinessLineId()) {
                    update.set("business_line_id", userInfoEntity.getBusinessLineId());
                }

                // 回写owner到任务详情表
                if (StringUtils.isNotEmpty(userInfoEntity.getUserName())) {
                    // update.push("task_owner").atPosition(0).value(userInfoEntity.getUserName());
                    update.set("task_owner", Arrays.asList(userInfoEntity.getUserName().split(ComConstants.COMMA)));
                }
                ops.updateOne(query, update);
            }
            ops.execute();
        }
    }

    /**
     * 根据projectId查询开源任务清单
     */
    public List<TaskInfoEntity> findByProjectId(String projectId, String createFrom, Pageable pageable) {
        Query query = new Query(Criteria.where("project_id").is(projectId).and("create_from").is(createFrom));
        query.fields().include("task_id", "name_cn");
        if (pageable != null) {
            query.with(pageable);
        }
        return mongoTemplate.find(query, TaskInfoEntity.class, "t_task_detail");
    }

    public Long findDataCount(List<Integer> bg,
            List<Integer> dept,
            List<Integer> center,
            List<String> projectIdList,
            List<String> createFrom) {
        Query query = new Query();
        int count = 0;
        if (CollectionUtils.isNotEmpty(bg)) {
            query.addCriteria(Criteria.where("bg_id").in(bg));
            count++;
        }
        if (CollectionUtils.isNotEmpty(dept)) {
            query.addCriteria(Criteria.where("dept_id").in(dept));
            count++;
        }
        if (CollectionUtils.isNotEmpty(center)) {
            query.addCriteria(Criteria.where("center_id").in(center));
            count++;
        }
        if (CollectionUtils.isNotEmpty(projectIdList)) {
            query.addCriteria(Criteria.where("project_id").in(projectIdList));
            count++;
        }
        if (CollectionUtils.isNotEmpty(createFrom)) {
            query.addCriteria(Criteria.where("create_from").in(createFrom));
            count++;
        }
        if (count == 0) {
            return -1L;
        }
        log.info("get task count sql: {}", query);
        return mongoTemplate.count(query, "t_task_detail");
    }

    @Override
    public List<Long> getTaskIdList(Long lastTaskId, Integer limit, String filterProjectId) {
        Query query = new Query();
        query.fields().include("task_id");
        query.addCriteria(Criteria.where("task_id").gt(lastTaskId));
        // 用来过滤机器自动创建任务
        if (StringUtils.isNotEmpty(filterProjectId)) {
            query.addCriteria(Criteria.where("project_id").ne(filterProjectId));
        }
        query.with(Sort.by(Direction.ASC, "task_id"));
        query.limit(limit);
        List<TaskIdInfo> taskIdInfoList = mongoTemplate.find(query, TaskIdInfo.class, "t_task_detail");
        return CollectionUtils.isEmpty(taskIdInfoList) ? Collections.emptyList()
                : taskIdInfoList.stream().map(TaskIdInfo::getTaskId).sorted().collect(Collectors.toList());
    }

    @Override
    public List<TaskProjectCountVO> getProjectCount(Set<Long> taskIds) {
        MatchOperation match = Aggregation
                .match(Criteria.where("task_id").in(taskIds));

        GroupOperation group = Aggregation.group("project_id")
                .first("project_id").as("projectId")
                .count().as("projectCount");
        Aggregation aggregation = Aggregation.newAggregation(match, group);
        AggregationResults<TaskProjectCountVO> queryResults =
                mongoTemplate.aggregate(aggregation, "t_task_detail", TaskProjectCountVO.class);
        return queryResults.getMappedResults();
    }

    @Override
    public List<TaskInfoEntity> getStopTask(Set<Long> taskIds, Long startTime, Long endTime) {
        Query query = new Query(Criteria.where("task_id").in(taskIds)
                .and("disable_time").gte(String.valueOf(startTime)).lte(String.valueOf(endTime))
                .and("status").is(TaskConstants.TaskStatus.DISABLE.value()));
        query.fields().include("task_id", "bg_id", "dept_id");
        return mongoTemplate.find(query, TaskInfoEntity.class, "t_task_detail");
    }

    @Override
    public List<TaskInfoEntity> getTaskByTaskIds(Set<Long> taskIds) {
        Query query = new Query(Criteria.where("task_id").in(taskIds));
        query.fields().include("task_id", "bg_id", "dept_id");
        return mongoTemplate.find(query, TaskInfoEntity.class, "t_task_detail");
    }

    public void setStatus(long taskId, int status) {
        Query query = new Query(Criteria.where("task_id").is(taskId));
        Update update = Update.update("status", status);
        mongoTemplate.updateFirst(query, update, TaskInfoEntity.class);
    }

    public List<TaskInfoEntity> getTaskIdListForHotColdDataSeparation(
            long lastTaskId, List<String> createFromList, List<Integer> statusList, int limit
    ) {
        Document fieldsObj = new Document();
        fieldsObj.put("task_id", true);

        Query query = new BasicQuery(new Document(), fieldsObj);
        query.addCriteria(Criteria.where("create_from").in(createFromList)
                .and("status").in(statusList)
                .and("task_id").gt(lastTaskId)
        );
        query.with(Sort.by(Direction.ASC, "task_id"));
        query.limit(limit);

        return mongoTemplate.find(query, TaskInfoEntity.class);
    }

    public void batchStopTask(List<Long> taskIds, String stopReason) {
        List<List<Long>> taskIdLists = Lists.partition(taskIds, COMMON_PAGE_SIZE);
        for (List<Long> taskIdList : taskIdLists) {
            Query updateQuery = Query.query(Criteria.where("task_id").in(taskIdList));
            Update update = Update.update("status", Status.DISABLE.value());
            update.set("disable_time", System.currentTimeMillis());
            update.set("disable_reason", stopReason);
            mongoTemplate.updateMulti(updateQuery, update, TaskInfoEntity.class);
        }
    }

    public void startTask(Long taskId) {
        Query updateQuery = Query.query(Criteria.where("task_id").is(taskId));
        Update update = Update.update("status", Status.ENABLE.value());
        update.set("disable_time", null);
        update.set("disable_reason", null);
        mongoTemplate.updateMulti(updateQuery, update, TaskInfoEntity.class);
    }

    public List<TaskStatisticVO> findTaskStatisticByIds(List<Long> taskIds) {
        MatchOperation match = Aggregation
                .match(Criteria.where("task_id").in(taskIds));

        GroupOperation group = Aggregation.group("bg_id", "dept_id")
                .first("bg_id").as("bgId")
                .first("dept_id").as("deptId")
                .count().as("taskCount");
        Aggregation aggregation = Aggregation.newAggregation(match, group);
        AggregationResults<TaskStatisticVO> queryResults =
                mongoTemplate.aggregate(aggregation, "t_task_detail", TaskStatisticVO.class);
        return queryResults.getMappedResults();
    }

    public List<Long> findTaskIdProjectIdWithPage(String filterProjectId, Pageable pageable) {

        Query query = new Query();
        query.fields().include("task_id");
        // 排除这些项目id的任务
        query.addCriteria(Criteria.where("project_id").ne(filterProjectId).and("status").is(
                ComConstants.Status.ENABLE.value()));
        query.with(pageable);
        return mongoTemplate.find(query, TaskIdInfo.class, "t_task_detail").stream().map(
                TaskIdInfo::getTaskId).collect(Collectors.toList());
    }
}
