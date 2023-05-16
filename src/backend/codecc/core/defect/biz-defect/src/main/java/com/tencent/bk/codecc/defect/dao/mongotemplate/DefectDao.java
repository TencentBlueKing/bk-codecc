/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dto.IgnoreTypeStatModel;
import com.tencent.bk.codecc.defect.model.defect.CLOCDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.pojo.CommonDefectIssueQueryMultiCond;
import com.tencent.bk.codecc.defect.vo.CommonDefectGroupStatisticVO;
import com.tencent.bk.codecc.defect.vo.admin.DefectCountModel;
import com.tencent.codecc.common.db.MongoPageHelper;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.ComConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 告警持久类
 *
 * @version V1.0
 * @date 2019/9/29
 */
@Slf4j
@Repository
public class DefectDao {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private MongoPageHelper mongoPageHelper;

    /**
     * 批量更新告警状态的fixed位
     *
     * @param taskId
     * @param defectList
     */
    public void batchUpdateDefectStatusFixedBit(long taskId, List<CommonDefectEntity> defectList) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CommonDefectEntity.class);
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId()))
                        .and("task_id").is(taskId));
                Update update = new Update();
                update.set("status", defectEntity.getStatus());
                update.set("fixed_time", defectEntity.getFixedTime());
                update.set("fixed_build_number", defectEntity.getFixedBuildNumber());
                update.set("exclude_time", defectEntity.getExcludeTime());
                update.set("file_path_name", defectEntity.getFilePath());
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 批量更新告警状态的ignore位
     *
     * @param taskId
     * @param defectList
     * @param ignoreReasonType
     * @param ignoreReason
     * @param ignoreAuthor
     */
    public void batchUpdateDefectStatusIgnoreBit(long taskId, List<CommonDefectEntity> defectList, int ignoreReasonType,
            String ignoreReason, String ignoreAuthor) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CommonDefectEntity.class);
            long currTime = System.currentTimeMillis();
            defectList.forEach(defectEntity -> {
                Update update = new Update();
                update.set("status", defectEntity.getStatus());
                update.set("ignore_time", currTime);
                update.set("ignore_reason_type", ignoreReasonType);
                update.set("ignore_reason", ignoreReason);
                update.set("ignore_author", ignoreAuthor);

                Query query = new Query(
                        Criteria.where("task_id").is(taskId)
                                .and("_id").is(new ObjectId(defectEntity.getEntityId()))
                );

                ops.upsert(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 批量更新告警状态的ignore类型
     *
     * @param taskId
     * @param defectList
     * @param ignoreReasonType
     */
    public void batchChangeIgnoreType(long taskId, List<CommonDefectEntity> defectList,
            int ignoreReasonType, String ignoreReason) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CommonDefectEntity.class);
            long currTime = System.currentTimeMillis();
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId()))
                        .and("task_id").is(taskId));
                Update update = new Update();
                update.set("ignore_reason_type", ignoreReasonType);
                update.set("ignore_reason", ignoreReason);
                ops.upsert(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 批量标志告警
     *
     * @param taskId
     * @param defectList
     * @param markFlag
     */
    public void batchMarkDefect(long taskId, List<CommonDefectEntity> defectList, Integer markFlag) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CommonDefectEntity.class);
            long currTime = System.currentTimeMillis();
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId()))
                        .and("task_id").is(taskId));
                Update update = new Update();
                update.set("mark", markFlag);
                update.set("mark_time", currTime);
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 批量更新告警作者
     *
     * @param taskId
     * @param defectList
     * @param authorList
     */
    public void batchUpdateDefectAuthor(long taskId, List<CommonDefectEntity> defectList, Set<String> authorList) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CommonDefectEntity.class);
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId()))
                        .and("task_id").is(taskId));
                Update update = new Update();
                update.set("author_list", authorList);
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 多条件批量获取告警信息
     *
     * @param toolName 工具名称
     * @param taskIdSet 任务ID集合
     * @param checkerNameSet 规则名称集合
     * @param status 告警状态（非待修复状态需要加1）
     * @return defect list
     */
    public List<CommonDefectEntity> batchQueryDefect(String toolName, Collection<Long> taskIdSet,
            Set<String> checkerNameSet, Integer status) {
        Document fieldsObj = new Document();
        fieldsObj.put("stream_name", false);
        fieldsObj.put("defect_instances", false);
        fieldsObj.put("ext_bug_id", false);
        fieldsObj.put("platform_build_id", false);
        fieldsObj.put("platform_project_id", false);

        Query query = new BasicQuery(new Document(), fieldsObj);
        if (StringUtils.isNotBlank(toolName)) {
            query.addCriteria(Criteria.where("tool_name").is(toolName));
        }
        if (CollectionUtils.isNotEmpty(taskIdSet)) {
            query.addCriteria(Criteria.where("task_id").in(taskIdSet));
        }
        if (CollectionUtils.isNotEmpty(checkerNameSet)) {
            query.addCriteria(Criteria.where("checker_name").in(checkerNameSet));
        }
        if (status != null && status != 0) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        return mongoTemplate.find(query, CommonDefectEntity.class);
    }

    /**
     * 批量查询cloc告警
     *
     * @param toolName
     * @param taskIdSet
     * @return
     */
    public List<CLOCDefectEntity> batchQueryClocDefect(String toolName, Collection<Long> taskIdSet) {
        Query query = new Query();
        if (StringUtils.isNotBlank(toolName)) {
            query.addCriteria(Criteria.where("tool_name").is(toolName));
        }
        if (CollectionUtils.isNotEmpty(taskIdSet)) {
            query.addCriteria(Criteria.where("task_id").in(taskIdSet));
        }

        return mongoTemplate.find(query, CLOCDefectEntity.class);
    }

    /**
     * 批量更新告警详情
     *
     * @param taskId
     * @param toolName
     * @param defectList
     */
    public void batchUpdateDefectDetail(Long taskId, String toolName, List<CommonDefectEntity> defectList) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CommonDefectEntity.class);
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id").is(taskId)
                        .and("tool_name").is(toolName)
                        .and("id").is(defectEntity.getId()));
                Update update = new Update();
                update.set("line_number", defectEntity.getLineNum());
                update.set("message", defectEntity.getMessage());
                update.set("severity", defectEntity.getSeverity());
                update.set("display_type", defectEntity.getDisplayType());
                update.set("display_category", defectEntity.getDisplayCategory());
                update.set("defect_instances", defectEntity.getDefectInstances());
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 逐个更新告警详情
     *
     * @param taskId
     * @param toolName
     * @param defectList
     */
    public void updateDefectDetailOneByOne(Long taskId, String toolName, List<CommonDefectEntity> defectList) {
        defectList.forEach(defectEntity -> {
            try {
                Query query = new Query();
                query.addCriteria(Criteria.where("task_id").is(taskId)
                        .and("tool_name").is(toolName)
                        .and("id").is(defectEntity.getId()));
                Update update = new Update();
                update.set("line_number", defectEntity.getLineNum());
                update.set("message", defectEntity.getMessage());
                update.set("severity", defectEntity.getSeverity());
                update.set("display_type", defectEntity.getDisplayType());
                update.set("display_category", defectEntity.getDisplayCategory());
                update.set("defect_instances", defectEntity.getDefectInstances());
                mongoTemplate.updateFirst(query, update, CommonDefectEntity.class);
            } catch (Exception e) {
                log.error("fail to update defect: {}" + defectEntity.getId());
            }
        });
    }

    /**
     * 批量统计任务待修复的告警个数
     *
     * @param taskIdList 任务id
     * @param toolName 工具名
     * @return list
     */
    public List<DefectCountModel> statisticCommonDefect(Set<Long> taskIdList, String toolName) {
        Criteria criteria = new Criteria();
        criteria.and("task_id").in(taskIdList)
                .and("tool_name").is(toolName)
                .and("status").is(ComConstants.DefectStatus.NEW.value());
        MatchOperation match = Aggregation.match(criteria);

        // 以task_id进行分组
        GroupOperation group = Aggregation.group("task_id")
                .last("task_id").as("taskId")
                .count().as("defectCount");

        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<DefectCountModel> queryResult =
                mongoTemplate.aggregate(agg, "t_defect", DefectCountModel.class);

        return queryResult.getMappedResults();
    }

    public Page<CommonDefectEntity> findByMultiCond(Long taskId, Collection<String> toolNames, Integer pageNum,
            Integer pageSize, String sortField, Sort.Direction sortType,
            CommonDefectIssueQueryMultiCond multiCond) {
        Criteria cri = getCommonDefectMultiCondCri(taskId, toolNames, multiCond);
        Query query = Query.query(cri);
        query.fields().exclude("defect_instances");
        long count = mongoTemplate.count(query, CommonDefectEntity.class);
        query.skip((pageNum - 1) * pageSize).limit(pageSize);
        query.with(Sort.by(getCommonDefectSortOrder(sortField, sortType)));
        List<CommonDefectEntity> entities = mongoTemplate.find(query, CommonDefectEntity.class);
        return new Page<>(pageNum, pageSize, count, entities);
    }

    private Criteria getCommonDefectMultiCondCri(Long taskId, Collection<String> toolNames,
            CommonDefectIssueQueryMultiCond multiCond) {
        List<Criteria> andCriList = new ArrayList<>();
        Criteria cri = Criteria.where("task_id").is(taskId).and("tool_name").in(toolNames);
        //构建ID查询
        if (StringUtils.isNotEmpty(multiCond.getBuildId()) && multiCond.getDefectIds() != null) {
            cri.and("id").in(multiCond.getDefectIds());
        }
        //规则查询
        if (CollectionUtils.isNotEmpty(multiCond.getCheckers())) {
            cri.and("checker_name").in(multiCond.getCheckers());
        }
        //作者查询
        if (StringUtils.isNotBlank(multiCond.getAuthor())) {
            cri.and("author_list").is(multiCond.getAuthor());
        }
        //路径匹配
        if (CollectionUtils.isNotEmpty(multiCond.getFilePathRegexes())) {
            List<Criteria> criteriaList = new ArrayList<>();
            multiCond.getFilePathRegexes().forEach(file ->
                    criteriaList.add(Criteria.where("file_path_name").regex(file)));
            andCriList.add(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
        }
        //创建时间
        if (multiCond.getStartCreateTime() != null && multiCond.getEndCreateTime() != null) {
            cri.and("create_time").gte(multiCond.getStartCreateTime()).lt(multiCond.getEndCreateTime());
        } else if (multiCond.getStartCreateTime() != null) {
            cri.and("create_time").gte(multiCond.getStartCreateTime());
        } else if (multiCond.getEndCreateTime() != null) {
            cri.and("create_time").lt(multiCond.getEndCreateTime());
        }
        //修复时间
        if (multiCond.getStartFixTime() != null && multiCond.getEndFixTime() != null) {
            cri.and("fixed_time").gte(multiCond.getStartFixTime()).lt(multiCond.getEndFixTime());
        } else if (multiCond.getStartFixTime() != null) {
            cri.and("fixed_time").gte(multiCond.getStartFixTime());
        } else if (multiCond.getEndFixTime() != null) {
            cri.and("fixed_time").lt(multiCond.getEndFixTime());
        }

        //严重程度过滤
        if (CollectionUtils.isNotEmpty(multiCond.getSeverities())) {
            if (!multiCond.getSeverities().containsAll(
                    Sets.newHashSet(ComConstants.SERIOUS, ComConstants.NORMAL, ComConstants.PROMPT_IN_DB))) {
                cri.and("severity").in(multiCond.getSeverities());
            }
        }
        andCriList.add(getStatusesQueryBasicDBObject(multiCond.getStatuses(), multiCond.getIgnoreReasonTypes()));
        return cri.andOperator(andCriList.toArray(new Criteria[]{}));
    }

    private Criteria getStatusesQueryBasicDBObject(Set<Integer> statusSet) {
        return getStatusesQueryBasicDBObject(statusSet, null);
    }

    private Criteria getStatusesQueryBasicDBObject(Set<Integer> statusSet, Set<Integer> ignoreReasonTypes) {
        // 快照查，对于查询传递过来的statusSet做补偿或者删除处理；若在这为空，则mock魔法值以返回空
        if (CollectionUtils.isEmpty(statusSet)) {
            return Criteria.where("status").is(-1);
        }
        List<Criteria> cris = new ArrayList<>();
        Set<Integer> statusFilters = new HashSet<>(statusSet);
        Integer newStatusFilterValue = Integer.numberOfTrailingZeros(ComConstants.DefectStatus.NEW.value());
        if (statusFilters.contains(newStatusFilterValue)) {
            cris.add(Criteria.where("status").is(ComConstants.DefectStatus.NEW.value()));
            statusFilters.remove(newStatusFilterValue);
        }
        Integer ignoreStatusFilterValue = Integer.numberOfTrailingZeros(ComConstants.DefectStatus.IGNORE.value());
        if (statusFilters.contains(ignoreStatusFilterValue) && CollectionUtils.isNotEmpty(ignoreReasonTypes)) {
            cris.add(Criteria.where("status").bits()
                    .anySet(Collections.singletonList(ignoreStatusFilterValue))
                    .and("ignore_reason_type").in(ignoreReasonTypes));
            statusFilters.remove(ignoreStatusFilterValue);
        }
        if (CollectionUtils.isNotEmpty(statusFilters)) {
            cris.add(Criteria.where("status").bits().anySet(new ArrayList<>(statusFilters)));
        }
        return cris.size() > 1 ? new Criteria().orOperator(cris.toArray(new Criteria[]{})) : cris.get(0);
    }


    private List<Sort.Order> getCommonDefectSortOrder(String sortField, Sort.Direction sortType) {
        if (StringUtils.isEmpty(sortField)) {
            sortField = "severity";
        }
        if (null == sortType) {
            sortType = Sort.Direction.ASC;
        }
        // createBuildNumber在mongodb中是String类型在没有collation支持下，无法正常敏感规则排序
        // 目前暂不考虑升级jar以支持collation特性
        if ("createBuildNumber".equals(sortField)) {
            sortField = "createTime";
        }
        // 严重程度要跟前端传入的排序类型相反
        if ("severity".equals(sortField)) {
            if (sortType.isAscending()) {
                sortType = Sort.Direction.DESC;
            } else {
                sortType = Sort.Direction.ASC;
            }
        }
        // 把前端传入的小驼峰排序字段转换为小写下划线的数据库字段名
        String sortFieldInDb = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, sortField);
        return Lists.newArrayList(new Sort.Order(sortType, sortFieldInDb));
    }

    /**
     * 根据状态过滤后获取规则，处理人、文件路径
     *
     * @param taskId
     * @param toolNameSet
     * @return
     */
    public List<CommonDefectGroupStatisticVO> statisticByFilePath(long taskId, List<String> toolNameSet,
            Set<Integer> statusSet, String buildId, Set<String> idSet) {
        MatchOperation match = getMatchByTaskIdAndToolNameAndStatus(taskId, toolNameSet, statusSet, buildId, idSet);
        GroupOperation group = Aggregation.group("file_path_name")
                .count().as("defectCount")
                .last("file_path_name").as("filePathName");
        Aggregation agg = Aggregation.newAggregation(match, group);
        AggregationResults<CommonDefectGroupStatisticVO> queryResult =
                mongoTemplate.aggregate(agg, "t_defect", CommonDefectGroupStatisticVO.class);
        return queryResult.getMappedResults();
    }

    /**
     * 根据状态过滤后获取规则，处理人、文件路径
     *
     * @param taskId
     * @param toolNameSet
     * @return
     */
    public List<CommonDefectGroupStatisticVO> statisticByChecker(long taskId, List<String> toolNameSet,
            Set<Integer> statusSet, String buildId, Set<String> idSet) {
        MatchOperation match = getMatchByTaskIdAndToolNameAndStatus(taskId, toolNameSet, statusSet, buildId, idSet);
        GroupOperation group = Aggregation.group("checker_name")
                .count().as("defectCount")
                .last("checker_name").as("checker");
        Aggregation agg = Aggregation.newAggregation(match, group);
        AggregationResults<CommonDefectGroupStatisticVO> queryResult =
                mongoTemplate.aggregate(agg, "t_defect", CommonDefectGroupStatisticVO.class);
        return queryResult.getMappedResults();
    }

    /**
     * 根据状态过滤后获取规则，处理人、文件路径
     *
     * @param taskId
     * @param toolNameSet
     * @return
     */
    public List<CommonDefectGroupStatisticVO> statisticByAuthor(long taskId, List<String> toolNameSet,
            Set<Integer> statusSet, String buildId, Set<String> idSet) {
        MatchOperation match = getMatchByTaskIdAndToolNameAndStatus(taskId, toolNameSet, statusSet, buildId, idSet);
        UnwindOperation unwind = Aggregation.unwind("author_list");
        GroupOperation group = Aggregation.group("author_list")
                .count().as("defectCount")
                .last("author_list").as("author");
        Aggregation agg = Aggregation.newAggregation(match, unwind, group);
        AggregationResults<CommonDefectGroupStatisticVO> queryResult =
                mongoTemplate.aggregate(agg, "t_defect", CommonDefectGroupStatisticVO.class);
        return queryResult.getMappedResults();
    }

    private MatchOperation getMatchByTaskIdAndToolNameAndStatus(long taskId, List<String> toolNameSet,
            Set<Integer> statusSet, String buildId, Set<String> idSet) {
        if (StringUtils.isNotEmpty(buildId) && CollectionUtils.isEmpty(idSet)) {
            return Aggregation.match(Criteria.where("task_id").is(-1));
        }

        List<Criteria> cris = new ArrayList<>();
        cris.add(Criteria.where("task_id").is(taskId));
        cris.add(Criteria.where("tool_name").in(toolNameSet));
        if (CollectionUtils.isNotEmpty(idSet)) {
            cris.add(Criteria.where("id").in(idSet));
        }
        cris.add(getStatusesQueryBasicDBObject(statusSet));
        Criteria cri = new Criteria();
        if (!cris.isEmpty()) {
            cri.andOperator(cris.toArray(new Criteria[]{}));
        }
        return Aggregation.match(cri);
    }


    public List<CommonDefectGroupStatisticVO> statisticByStatus(Long taskId, Collection<String> toolNames,
            CommonDefectIssueQueryMultiCond multiCond) {
        MatchOperation match = Aggregation.match(getCommonDefectMultiCondCri(taskId, toolNames, multiCond));
        GroupOperation group = Aggregation.group("status")
                .count().as("defectCount")
                .last("status").as("status");
        Aggregation agg = Aggregation.newAggregation(match, group);
        AggregationResults<CommonDefectGroupStatisticVO> queryResult =
                mongoTemplate.aggregate(agg, "t_defect", CommonDefectGroupStatisticVO.class);
        return queryResult.getMappedResults();
    }

    public List<CommonDefectGroupStatisticVO> statisticBySeverity(Long taskId, Collection<String> toolNames,
            CommonDefectIssueQueryMultiCond multiCond) {
        MatchOperation match = Aggregation.match(getCommonDefectMultiCondCri(taskId, toolNames, multiCond));
        GroupOperation group = Aggregation.group("severity")
                .count().as("defectCount")
                .last("severity").as("severity");
        Aggregation agg = Aggregation.newAggregation(match, group);
        AggregationResults<CommonDefectGroupStatisticVO> queryResult =
                mongoTemplate.aggregate(agg, "t_defect", CommonDefectGroupStatisticVO.class);
        return queryResult.getMappedResults();
    }

    public Long statisticByDefectType(Long taskId, Collection<String> toolNames,
            CommonDefectIssueQueryMultiCond multiCond) {
        Criteria cri = getCommonDefectMultiCondCri(taskId, toolNames, multiCond);
        return mongoTemplate.count(Query.query(cri), CommonDefectEntity.class);
    }

    public List<CommonDefectEntity> findDataReportFieldByTaskIdAndToolName(long taskId, String toolName) {
        Query query = Query.query(Criteria.where("task_id").is(taskId).and("tool_name").is(toolName));
        query.fields().include("task_id", "tool_name", "status", "author_list",
                "severity", "create_time", "fixed_time", "ignore_time", "exclude_time");
        return mongoTemplate.find(query, CommonDefectEntity.class);
    }

    /**
     * 组装查询条件
     *
     * @return criteria
     */
    @Nullable
    private Criteria generateQueryCondition(@NotNull Map<Long, Set<String>> taskToolsMap, int ignoreTypeId,
            int ignoreStatus) {
        List<Criteria> orCriteriaList = Lists.newArrayList();

        for (Map.Entry<Long, Set<String>> entry : taskToolsMap.entrySet()) {
            Long taskId = entry.getKey();
            entry.getValue().forEach(
                    tool -> orCriteriaList.add(Criteria.where("task_id").is(taskId).and("tool_name").is(tool)));
        }

        Criteria criteria = new Criteria();
        // 如果没有以上条件,则不查询
        if (CollectionUtils.isEmpty(orCriteriaList)) {
            return null;
        }
        criteria.orOperator(orCriteriaList.toArray(new Criteria[0]));
        criteria.and("status").is(ignoreStatus).and("ignore_reason_type").is(ignoreTypeId);

        return criteria;
    }

    /**
     * 统计指定忽略类型的告警数
     */
    public List<IgnoreTypeStatModel> statisticIgnoreDefect(Map<Long, Set<String>> taskToolsMap,
            int ignoreTypeId, int ignoreStatus) {
        Criteria criteria = generateQueryCondition(taskToolsMap, ignoreTypeId, ignoreStatus);
        if (null == criteria) {
            return Collections.emptyList();
        }

        MatchOperation match = Aggregation.match(criteria);
        GroupOperation group = Aggregation.group("task_id", "ignore_author")
                .first("task_id").as("taskId")
                .first("ignore_author").as("ignoreAuthor")
                .count().as("defectCount");

        Aggregation agg = Aggregation.newAggregation(match, group);
        AggregationResults<IgnoreTypeStatModel> queryResult =
                mongoTemplate.aggregate(agg, "t_defect", IgnoreTypeStatModel.class);
        return queryResult.getMappedResults();
    }

    /**
     * 统计指定忽略类型的告警作者
     */
    public List<IgnoreTypeStatModel> findIgnoreDefectAuthor(Map<Long, Set<String>> taskToolsMap,
            int ignoreTypeId, int ignoreStatus) {
        Criteria criteria = generateQueryCondition(taskToolsMap, ignoreTypeId, ignoreStatus);
        if (null == criteria) {
            return Collections.emptyList();
        }

        MatchOperation match = Aggregation.match(criteria);
        UnwindOperation unwind = Aggregation.unwind("author_list");
        GroupOperation group = Aggregation.group("author_list")
                .first("author_list").as("ignoreAuthor");

        Aggregation agg = Aggregation.newAggregation(match, unwind, group);
        AggregationResults<IgnoreTypeStatModel> queryResult =
                mongoTemplate.aggregate(agg, "t_defect", IgnoreTypeStatModel.class);
        return queryResult.getMappedResults();
    }

    public List<CommonDefectEntity> findIgnoreDefectForSnapshot(Long taskId, String toolName,
            Integer ignoreReasonType) {
        Criteria cri = new Criteria();
        cri.andOperator(Criteria.where("task_id").is(taskId).and("tool_name").is(toolName)
                        .and("ignore_reason_type").is(ignoreReasonType),
                Criteria.where("status").bits().allSet(ComConstants.DefectStatus.IGNORE.value()),
                Criteria.where("status").bits().allClear(ComConstants.DefectStatus.FIXED.value()));
        Query query = Query.query(cri);
        query.fields().include("id", "revision", "line_number", "status", "checker_name",
                "severity", "create_time");
        return mongoTemplate.find(query, CommonDefectEntity.class);
    }

    public List<Long> findTaskIdsByEntityIds(Set<String> entityIds) {
        Criteria cri = Criteria.where("_id").in(entityIds.stream().map(ObjectId::new).collect(Collectors.toSet()));
        MatchOperation match = Aggregation.match(cri);
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id");
        Aggregation agg = Aggregation.newAggregation(match, group);
        AggregationResults<CommonDefectEntity> queryResult =
                mongoTemplate.aggregate(agg, "t_defect", CommonDefectEntity.class);
        if (CollectionUtils.isEmpty(queryResult.getMappedResults())) {
            return Collections.emptyList();
        }
        return queryResult.getMappedResults().stream().map(CommonDefectEntity::getTaskId).collect(Collectors.toList());
    }

}
