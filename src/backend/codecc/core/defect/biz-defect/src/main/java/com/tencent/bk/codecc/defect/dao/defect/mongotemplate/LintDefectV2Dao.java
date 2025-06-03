/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.IgnoreApprovalConstants.ApproverStatus;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.dto.IgnoreTypeStatModel;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.defect.code.gen.LintDefectV2EntityTracking;
import com.tencent.bk.codecc.defect.vo.LintDefectGroupStatisticVO;
import com.tencent.bk.codecc.defect.vo.LintFileVO;
import com.tencent.bk.codecc.defect.vo.LintStatisticVO;
import com.tencent.bk.codecc.defect.vo.StatisticVO;
import com.tencent.bk.codecc.defect.vo.admin.DefectCountModel;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVOBase.CheckerSet;
import com.tencent.codecc.common.db.MongoPageHelper;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.DateTimeUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.CountOperation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;
import com.mongodb.client.MongoCursor;

/**
 * lint类工具持久层代码
 *
 * @version V1.0
 * @date 2019/5/10
 */
@Slf4j
@Repository
public class LintDefectV2Dao {

    public static final String COLLECTION_NAME = "t_lint_defect_v2";

    // 公共的分页大小
    private static final int PAGE_SIZE = 1000;

    @Autowired
    private MongoTemplate defectMongoTemplate;
    @Autowired
    private MongoPageHelper mongoPageHelper;
    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;

    public boolean saveAll(List<LintDefectV2Entity> entities) {
        lintDefectV2Repository.saveAll(entities);
        return true;
    }

    /**
     * 为了降低代码重复率，抽象出一个函数用于 add 重复使用的条件。
     * add 如下条件：
     * task_id = taskId AND tool_name = toolName AND (rel_path in relPathSet OR
     * ((rel_path == NULL OR rel_path not exists OR rel_path == "") AND file_path in absPathSet))
     *
     * @param criteria 待添加的条件
     * @param taskId
     * @param toolName
     * @param relPathSet
     * @param absPathSet
     * @return void
     * @date 2024/2/28
     */
    private void addCommonCriteria(
            Criteria criteria,
            long taskId,
            String toolName,
            Set<String> relPathSet,
            Set<String> absPathSet
    ) {
        List<Criteria> orCriteriaList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(relPathSet)) {
            orCriteriaList.add(Criteria.where("rel_path").in(relPathSet));
        }

        if (CollectionUtils.isNotEmpty(absPathSet)) {
            List<Criteria> subOrCriteriaList = new ArrayList<>();
            subOrCriteriaList.add(Criteria.where("rel_path").isNull());
            subOrCriteriaList.add(Criteria.where("rel_path").is(""));
            Criteria subOrCriteria = new Criteria().orOperator(subOrCriteriaList);

            Criteria subAndCriteria = new Criteria();
            subAndCriteria.andOperator(subOrCriteria, Criteria.where("file_path").in(absPathSet));

            orCriteriaList.add(subAndCriteria);
        }

        criteria.and("task_id").is(taskId)
                .and("tool_name").is(toolName);

        if (CollectionUtils.isNotEmpty(orCriteriaList)) {
            Criteria subCriteria = new Criteria().orOperator(orCriteriaList);
            criteria.andOperator(subCriteria);
        }
    }

    /**
     * 查询满足:
     * task_id = taskId AND tool_name = toolName AND (rel_path in relPathSet OR
     * ((rel_path == NULL OR rel_path not exists OR rel_path == "") AND file_path in absPathSet))
     * 的所有 LintDefectV2Entity
     *
     * @param taskId
     * @param toolName
     * @param relPathSet
     * @param absPathSet
     * @return java.util.List<com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity>
     * @date 2024/2/27
     */
    public List<LintDefectV2EntityTracking> findByTaskIdAndToolNameAndPath(
            long taskId,
            String toolName,
            Set<String> relPathSet,
            Set<String> absPathSet
    ) {
        Criteria criteria = new Criteria();
        addCommonCriteria(criteria, taskId, toolName, relPathSet, absPathSet);

        Query query = new Query();
        query.addCriteria(criteria);

        return defectMongoTemplate.find(
                query,
                LintDefectV2EntityTracking.class,
                COLLECTION_NAME
        );
    }

    /**
     * 查询满足:
     * task_id = taskId AND tool_name = toolName AND (rel_path in relPathSet OR
     * ((rel_path == NULL OR rel_path not exists OR rel_path == "") AND file_path in absPathSet))
     * AND line_update_time >= lineUpdateTime
     * 的所有 LintDefectV2Entity
     *
     * @param taskId
     * @param toolName
     * @param relPathSet
     * @param absPathSet
     * @param lineUpdateTime
     * @return java.util.List<com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity>
     * @date 2024/2/27
     */
    public List<LintDefectV2Entity> findByTaskIdAndToolNameAndPathAndLineUpdateTime(
            long taskId,
            String toolName,
            Set<String> relPathSet,
            Set<String> absPathSet,
            long lineUpdateTime
    ) {
        Criteria criteria = new Criteria();
        criteria.and("line_update_time").gte(lineUpdateTime);
        addCommonCriteria(criteria, taskId, toolName, relPathSet, absPathSet);

        Query query = new Query();
        query.addCriteria(criteria);

        return defectMongoTemplate.find(query, LintDefectV2Entity.class);
    }

    /**
     * 根据条件查询告警列表
     *
     * @param taskToolMap
     * @param defectQueryReqVO
     * @param defectMongoIdSet
     * @param filedMap 设置需要返回或者过滤的字段
     * @param defectThirdPartyIdSet 第三方平台主键Id
     * @return
     */
    public List<LintDefectV2Entity> findDefectByCondition(
            Map<Long, List<String>> taskToolMap, DefectQueryReqVO defectQueryReqVO,
            Set<String> defectMongoIdSet, Set<String> pkgChecker,
            Map<String, Boolean> filedMap, Set<String> defectThirdPartyIdSet
    ) {
        Query query = getQueryByCondition(
                taskToolMap,
                defectQueryReqVO,
                defectMongoIdSet,
                pkgChecker,
                filedMap,
                defectThirdPartyIdSet
        );

        return defectMongoTemplate.find(query, LintDefectV2Entity.class);
    }

    /**
     * 根据条件查询告警列表
     *
     * @param taskToolMap
     * @param defectQueryReqVO
     * @param defectMongoIdSet
     * @param filedMap 设置需要返回或者过滤的字段
     * @param defectThirdPartyIdSet 第三方平台主键Id
     * @return
     */
    public Long countDefectByCondition(
            Map<Long, List<String>> taskToolMap, DefectQueryReqVO defectQueryReqVO,
            Set<String> defectMongoIdSet, Set<String> pkgChecker,
            Map<String, Boolean> filedMap, Set<String> defectThirdPartyIdSet
    ) {
        Query query = getQueryByCondition(
                taskToolMap,
                defectQueryReqVO,
                defectMongoIdSet,
                pkgChecker,
                filedMap,
                defectThirdPartyIdSet
        );

        return defectMongoTemplate.count(query, LintDefectV2Entity.class);
    }

    /**
     * 根据条件查询告警列表 - 文件路径分页
     *
     * @param taskToolMap
     * @param defectQueryReqVO
     * @param defectMongoIdSet
     * @param pkgChecker
     * @param filedMap
     * @param defectThirdPartyIdSet
     * @param startFilePath
     * @param skip
     * @param pageSize
     * @return
     */
    public List<LintDefectV2Entity> findDefectByConditionWithFilePathPage(
            Map<Long, List<String>> taskToolMap, DefectQueryReqVO defectQueryReqVO,
            Set<String> defectMongoIdSet, Set<String> pkgChecker,
            Map<String, Boolean> filedMap, Set<String> defectThirdPartyIdSet,
            String startFilePath, Long skip, Integer pageSize
    ) {
        Query query = new Query();
        if (MapUtils.isNotEmpty(filedMap)) {
            Document fieldsObj = new Document();
            filedMap.forEach((filed, isNeedReturn) -> fieldsObj.put(filed, isNeedReturn));
            query = new BasicQuery(new Document(), fieldsObj);
        }

        Criteria criteria = getQueryCriteria(
                taskToolMap,
                defectQueryReqVO,
                defectMongoIdSet,
                pkgChecker,
                defectThirdPartyIdSet,
                false
        );
        if (startFilePath != null) {
            criteria.and("file_path").gte(startFilePath);
        }
        query.addCriteria(criteria);
        int pageSizeValue = pageSize == null || pageSize <= 0 ? 100 : pageSize;
        long skipValue = skip == null || skip < 0 ? 0L : skip;
        query.with(Sort.by(Sort.Direction.ASC, "file_path"));
        query.limit(pageSizeValue);
        if (skipValue > 0) {
            query.skip(skipValue);
        }

        return defectMongoTemplate.find(query, LintDefectV2Entity.class);
    }

    /**
     * 根据条件查询文件列表
     *
     * @param taskToolMap
     * @param defectQueryReqVO
     * @param defectMongoIdSet mongodb主键标识
     * @param filedMap 设置需要返回或者过滤的字段
     * @param defectThirdPartyIdSet 第三方平台的告警主键标识
     * @return
     */
    public Page<LintDefectV2Entity> findDefectPageByCondition(
            Map<Long, List<String>> taskToolMap, DefectQueryReqVO defectQueryReqVO, Set<String> defectMongoIdSet,
            Set<String> pkgChecker, Map<String, Boolean> filedMap,
            Integer pageNum, Integer pageSize, String sortField, Sort.Direction sortType,
            Set<String> defectThirdPartyIdSet, String projectId, String userId
    ) {
        // NOCC:VariableDeclarationUsageDistance(设计如此:)
        Query query = getQueryByCondition(
                taskToolMap,
                defectQueryReqVO,
                defectMongoIdSet,
                pkgChecker,
                filedMap,
                defectThirdPartyIdSet
        );

        if (StringUtils.isEmpty(sortField)) {
            sortField = "severity";
        }
        if (null == sortType) {
            sortType = Sort.Direction.ASC;
        }

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
        List<Sort.Order> sortList = Lists.newArrayList(new Sort.Order(sortType, sortFieldInDb));

        // 如果按文件名排序，那么还要再按行号排序
        if ("fileName".equals(sortField)) {
            sortList.add(new Sort.Order(sortType, "line_num"));
        }

        // 若是跨任务查询
        Duration timeout = null;
        if (MapUtils.isNotEmpty(taskToolMap) && taskToolMap.size() > 1) {
            timeout = Duration.ofSeconds(60L);
        }

        long beginTime = System.currentTimeMillis();
        Page<LintDefectV2Entity> pageResult = mongoPageHelper.pageQuery(
                query, LintDefectV2Entity.class, pageSize,
                pageNum, sortList, timeout
        );

        if (MapUtils.isNotEmpty(taskToolMap) && taskToolMap.size() > 1) {
            log.info("Multi-Task-Query list cost: {}, project id: {}, user id: {}",
                    System.currentTimeMillis() - beginTime, projectId, userId);
        }

        return pageResult;
    }

    @NotNull
    protected Query getQueryByCondition(
            Map<Long, List<String>> taskToolMap,
            DefectQueryReqVO defectQueryReqVO,
            Set<String> defectMongoIdSet,
            Set<String> pkgChecker,
            Map<String, Boolean> filedMap,
            Set<String> defectThirdPartyIdSet
    ) {
        Query query = new Query();

        if (MapUtils.isNotEmpty(filedMap)) {
            Document fieldsObj = new Document();
            filedMap.forEach((filed, isNeedReturn) -> fieldsObj.put(filed, isNeedReturn));
            query = new BasicQuery(new Document(), fieldsObj);
        }

        Criteria criteria = getQueryCriteria(
                taskToolMap,
                defectQueryReqVO,
                defectMongoIdSet,
                pkgChecker,
                defectThirdPartyIdSet,
                false
        );
        query.addCriteria(criteria);

        return query;
    }

    @NotNull
    private Criteria getQueryCriteria(
            Map<Long, List<String>> taskToolMap,
            DefectQueryReqVO defectQueryReqVO,
            Set<String> defectMongoIdSet,
            Set<String> pkgChecker,
            Set<String> defectThirdPartyIdSet,
            boolean isAggregate
    ) {
        String checker = defectQueryReqVO.getChecker();
        CheckerSet checkerSet = defectQueryReqVO.getCheckerSet();
        String author = defectQueryReqVO.getAuthor();
        Set<String> fileList = defectQueryReqVO.getFileList();
        Set<String> conditionSeverity = defectQueryReqVO.getSeverity();
        // NOCC:VariableDeclarationUsageDistance(设计如此:)
        final Set<String> conditionStatusStrSet = defectQueryReqVO.getStatus();
        String buildId = defectQueryReqVO.getBuildId();
        Set<Integer> ignoreReasonTypes = defectQueryReqVO.getIgnoreReasonTypes();

        Criteria magicEmptyCriteria = Criteria.where("task_id").is(-1L);

        // 若是快照查，但快照中记录的告警为空，则返回一个带索引"魔法值"，以mock回空集
        if (StringUtils.isNotEmpty(buildId)
                && CollectionUtils.isEmpty(defectMongoIdSet)
                && CollectionUtils.isEmpty(defectThirdPartyIdSet)) {
            return magicEmptyCriteria;
        }

        // 传入规则查，但db没有匹配的
        if ((StringUtils.isNotEmpty(checker) || checkerSet != null) && CollectionUtils.isEmpty(pkgChecker)) {
            return magicEmptyCriteria;
        }

        if (MapUtils.isEmpty(taskToolMap)) {
            return magicEmptyCriteria;
        }

        /*
         * 注意事项：
         * 1、or查询会影响索引命中率
         * 2、aggregate()因SDK版本不支持hint，应重构查询语句，减少or的影响
         * 3、find()因数据库版本不支持allowDiskUse=> "Sort operation used more than the maximum 33554432 bytes of RAM"
         */
        Criteria rootCriteria = null;

        // 优化掉or查询
        if (taskToolMap.size() == 1) {
            Entry<Long, List<String>> kv = taskToolMap.entrySet().stream().findFirst().get();
            rootCriteria = Criteria.where("task_id").is(kv.getKey()).and("tool_name").in(kv.getValue());
        } else {
            // 若任务对应的工具均是一致的
            boolean isEqual = true;
            Set<String> toolNameSet = Sets.newHashSet(taskToolMap.values().iterator().next());
            for (List<String> toolNameList : taskToolMap.values()) {
                if (toolNameList.size() != toolNameSet.size() || !toolNameSet.containsAll(toolNameList)) {
                    isEqual = false;
                    break;
                }
            }

            if (isEqual) {
                rootCriteria = Criteria.where("task_id").in(taskToolMap.keySet())
                        .and("tool_name").in(toolNameSet);
            }
        }

        // 若前置的or优化均未能命中时
        if (rootCriteria == null) {
            Set<String> allToolNameSet = taskToolMap.values().stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());

            // agg()操作sdk层面不支持指定索引，通过提取关键条件到外层，降低or的干扰
            if (isAggregate) {
                rootCriteria = Criteria.where("task_id").in(taskToolMap.keySet())
                        .and("tool_name").in(allToolNameSet);
            } else {
                rootCriteria = new Criteria();
            }

            List<Criteria> innerOrOpList = Lists.newArrayList();
            for (Entry<Long, List<String>> entry : taskToolMap.entrySet()) {
                Long taskId = entry.getKey();
                List<String> toolNameList = entry.getValue();
                innerOrOpList.add(
                        Criteria.where("task_id").is(taskId)
                                .and("tool_name").in(toolNameList)
                );
            }

            rootCriteria.orOperator(innerOrOpList.toArray(new Criteria[]{}));
        }

        List<Criteria> andOpList = Lists.newArrayList();

        // buildId对应的告警ID集合过滤
        if (CollectionUtils.isNotEmpty(defectMongoIdSet) && CollectionUtils.isNotEmpty(defectThirdPartyIdSet)) {
            Criteria orCriteria = new Criteria().orOperator(
                    Criteria.where("_id").in(
                            defectMongoIdSet.stream().map(ObjectId::new).collect(Collectors.toSet())
                    ),
                    Criteria.where("id").in(defectThirdPartyIdSet)
            );
            andOpList.add(orCriteria);
        } else if (CollectionUtils.isNotEmpty(defectMongoIdSet) && CollectionUtils.isEmpty(defectThirdPartyIdSet)) {
            rootCriteria.and("_id").in(defectMongoIdSet.stream().map(ObjectId::new).collect(Collectors.toSet()));
        } else if (CollectionUtils.isEmpty(defectMongoIdSet) && CollectionUtils.isNotEmpty(defectThirdPartyIdSet)) {
            rootCriteria.and("id").in(defectThirdPartyIdSet);
        }

        // 状态过滤
        if (CollectionUtils.isNotEmpty(conditionStatusStrSet)) {
            Criteria statusCriteria = getStatusCriteria(conditionStatusStrSet, buildId, ignoreReasonTypes);
            if (statusCriteria != null) {
                andOpList.add(statusCriteria);
            } else {
                return magicEmptyCriteria;
            }
        }

        // 规则类型过滤
        if (CollectionUtils.isNotEmpty(pkgChecker)) {
            rootCriteria.and("checker").in(pkgChecker);
        }

        // 告警作者过滤
        if (StringUtils.isNotEmpty(author)) {
            rootCriteria.and("author").in(Lists.newArrayList(author));
        }

        // 严重程度过滤
        if (CollectionUtils.isNotEmpty(conditionSeverity)) {
            if (conditionSeverity.contains(String.valueOf(ComConstants.PROMPT))) {
                conditionSeverity.add(String.valueOf(ComConstants.PROMPT_IN_DB));
            }

            if (!conditionSeverity.containsAll(Sets.newHashSet(String.valueOf(ComConstants.SERIOUS),
                    String.valueOf(ComConstants.NORMAL), String.valueOf(ComConstants.PROMPT_IN_DB)))) {
                Set<Integer> severitySet = conditionSeverity.stream().map(it -> Integer.valueOf(it))
                        .collect(Collectors.toSet());
                rootCriteria.and("severity").in(severitySet);
            }
        }

        // 路径过滤
        if (CollectionUtils.isNotEmpty(fileList)) {
            List<Criteria> criteriaList = new ArrayList<>();
            fileList.forEach(file -> criteriaList.add(Criteria.where("file_path").regex(file)));
            andOpList.add(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
        }

        // 按创建日期过滤
        Criteria creatTimeCri = getStartEndTimeStampCri("line_update_time",
                defectQueryReqVO.getStartCreateTime(), defectQueryReqVO.getEndCreateTime());
        if (creatTimeCri != null) {
            andOpList.add(creatTimeCri);
        }

        //按修复日期过滤
        Criteria fixTimeCri = getStartEndTimeStampCri("fixed_time",
                defectQueryReqVO.getStartFixTime(), defectQueryReqVO.getEndFixTime());
        if (fixTimeCri != null) {
            andOpList.add(fixTimeCri);
        }

        // 需要过滤Aproval状态
        Criteria approvalCri = getIgnoreApprovalCri(defectQueryReqVO.getIgnoreApprovalStatus(),
                defectQueryReqVO.getNeedFilterApprovalDefect());
        if (approvalCri != null) {
            andOpList.add(approvalCri);
        }

        // AprovalId过滤
        if (StringUtils.isNotEmpty(defectQueryReqVO.getIgnoreApprovalId())) {
            rootCriteria.and("ignore_approval_id").is(defectQueryReqVO.getIgnoreApprovalId());
        }

        long langValue = defectQueryReqVO.getCheckerSet() != null ? defectQueryReqVO.getCheckerSet().getCodeLang() : 0L;

        // 按选定规则集的语言过滤
        if (langValue > 0) {
            Criteria langValueCri = new Criteria();
            langValueCri.orOperator(Criteria.where("lang_value").is(langValue),
                    Criteria.where("lang_value").exists(false),
                    Criteria.where("lang_value").is(null));
            andOpList.add(langValueCri);
        }

        if (CollectionUtils.isNotEmpty(defectQueryReqVO.getOperates())) {
            Criteria operateCri = getOperateCri(defectQueryReqVO.getOperates(), defectMongoIdSet,
                    defectQueryReqVO.getSubmitDefectIds(), conditionStatusStrSet);
            if (operateCri == null) {
                return magicEmptyCriteria;
            } else {
                andOpList.add(operateCri);
            }
        }

        if (!andOpList.isEmpty()) {
            rootCriteria.andOperator(andOpList.toArray(new Criteria[]{}));
        }

        return rootCriteria;
    }

    /**
     * 获取忽略审核过滤条件
     * @param ignoreApprovalStatus
     * @param needFilterApprovalDefect
     * @return
     */
    private Criteria getIgnoreApprovalCri(List<Integer> ignoreApprovalStatus, Boolean needFilterApprovalDefect) {
        boolean needFilter = needFilterApprovalDefect != null && needFilterApprovalDefect;
        if (CollectionUtils.isEmpty(ignoreApprovalStatus) && !needFilter) {
            return null;
        } else if (CollectionUtils.isNotEmpty(ignoreApprovalStatus) && !needFilter) {
            return Criteria.where("ignore_approval_status").in(ignoreApprovalStatus);
        } else if (CollectionUtils.isEmpty(ignoreApprovalStatus) && needFilter) {
            Criteria criteria = new Criteria();
            criteria.orOperator(
                    // 审核完成的待修复告警
                    Criteria.where("ignore_approval_status").in(ApproverStatus.APPROVAL_FINISH_STATUS)
                            .and("ignore_approval_id").exists(true),
                    // 没有设置
                    Criteria.where("ignore_approval_id").exists(false),
                    Criteria.where("ignore_approval_id").is(null),
                    Criteria.where("ignore_approval_id").is(ComConstants.EMPTY_STRING)
            );
            return criteria;
        } else {
            ignoreApprovalStatus.retainAll(ApproverStatus.APPROVAL_FINISH_STATUS);
            if (CollectionUtils.isEmpty(ignoreApprovalStatus)) {
                return null;
            }
            return Criteria.where("ignore_approval_status").in(ignoreApprovalStatus);
        }
    }

    private Criteria getOperateCri(List<String> operates, Set<String> defectIds, Set<String> submitDefectIds,
            Set<String> status) {
        List<Criteria> orCri = new ArrayList<>();
        for (String operate : operates) {
            ComConstants.CodeCCDefectOpsType opsType = ComConstants.CodeCCDefectOpsType.getByName(operate);
            if (opsType == null) {
                continue;
            }
            switch (opsType) {
                case MARK:
                    if (CollectionUtils.isEmpty(status)
                            || status.contains(String.valueOf(DefectStatus.NEW.value()))) {
                        orCri.add(Criteria.where("mark").gt(ComConstants.MarkStatus.NOT_MARKED.value())
                                .and("status").is(DefectStatus.NEW.value()));
                    }
                    break;
                case COMMENT:
                    orCri.add(Criteria.where("code_comment").exists(true));
                    break;
                case NO_OPS:
                    List<Criteria> andCri = new ArrayList<>();
                    andCri.add(Criteria.where("code_comment").exists(false));
                    andCri.add(new Criteria().orOperator(
                            Criteria.where("status").ne(DefectStatus.NEW.value()),
                            Criteria.where("mark_but_no_fixed").is(false),
                            Criteria.where("mark_but_no_fixed").exists(false)
                    ));
                    andCri.add(new Criteria().orOperator(
                            Criteria.where("status").ne(DefectStatus.NEW.value()),
                            Criteria.where("mark").is(ComConstants.MarkStatus.NOT_MARKED.value()),
                            Criteria.where("mark").exists(false)
                    ));
                    if (CollectionUtils.isNotEmpty(submitDefectIds)) {
                        andCri.add(Criteria.where("_id").nin(
                                submitDefectIds.stream().map(ObjectId::new).collect(Collectors.toSet())
                        ));
                    }
                    orCri.add(new Criteria().andOperator(andCri.toArray(new Criteria[]{})));
                    break;
                case ISSUE_SUBMIT:
                    if (CollectionUtils.isNotEmpty(submitDefectIds)) {
                        orCri.add(Criteria.where("_id").in(
                                submitDefectIds.stream()
                                        .filter(it -> CollectionUtils.isEmpty(defectIds) || defectIds.contains(it))
                                        .map(ObjectId::new).collect(Collectors.toSet())));
                    }
                    break;
                case MARK_NOT_FIXED:
                    if (CollectionUtils.isEmpty(status)
                            || status.contains(String.valueOf(DefectStatus.NEW.value()))) {
                        orCri.add(Criteria.where("mark_but_no_fixed").is(true)
                                .and("status").is(DefectStatus.NEW.value()));
                    }
                    break;
                default:
                    break;
            }
        }

        if (CollectionUtils.isNotEmpty(orCri)) {
            return orCri.size() == 1 ? orCri.get(0) : new Criteria().orOperator(orCri.toArray(new Criteria[]{}));
        }
        return null;
    }

    /**
     * 根据查询字段和时间范围定制查询规则
     *
     * @param field 查询字段
     * @param startTimeStr 查询起始时间
     * @param endTimeStr 查询结束时间
     * @return 查询规则
     */
    private Criteria getStartEndTimeStampCri(String field, String startTimeStr, String endTimeStr) {
        long minTime = 0L;
        long maxTime = 0L;
        if (StringUtils.isNotEmpty(startTimeStr)) {
            minTime = DateTimeUtils.getTimeStamp(startTimeStr + " 00:00:00");
            maxTime = StringUtils.isEmpty(endTimeStr) ? System.currentTimeMillis()
                    : DateTimeUtils.getTimeStamp(endTimeStr + " 23:59:59");
        }

        if (minTime != 0 && maxTime == 0) {
            return new Criteria().and(field).gte(minTime);
        } else if (minTime == 0 && maxTime != 0) {
            return new Criteria().and(field).lt(maxTime).gt(0);
        } else if (minTime != 0 && maxTime != 0) {
            return new Criteria().and(field).lt(maxTime).gte(minTime);
        }
        return null;
    }

    /**
     * 获取状态过滤条件
     *
     * @param statusFilter 状态过滤列表
     * @param buildId 快照ID
     * @param ignoreReasonTypes 已忽略的子查询条件，忽略类型
     * @return
     */
    private Criteria getStatusCriteria(Set<String> statusFilter, String buildId, Set<Integer> ignoreReasonTypes) {
        if (CollectionUtils.isEmpty(statusFilter)) {
            return new Criteria();
        }
        // 若是快照查，选中待修复就必须补偿上已修复；真实状态会在postHandleLintDefect()后置处理
        if (StringUtils.isNotEmpty(buildId)) {
            String newStatusStr = String.valueOf(DefectStatus.NEW.value());
            String fixedStatusStr = String.valueOf(DefectStatus.FIXED.value());
            if (statusFilter.contains(newStatusStr)) {
                statusFilter.add(newStatusStr);
                statusFilter.add(fixedStatusStr);
            } else {
                // 快照查，不存在已修复
                statusFilter.remove(fixedStatusStr);
            }
        }

        if (CollectionUtils.isEmpty(statusFilter)) {
            return null;
        }

        LinkedList<Criteria> criteriaList = Lists.newLinkedList();
        // 如果查询已忽略告警，且已忽略类型不为空，需添加忽略类型查询
        boolean hasIgnore = statusFilter.contains(String.valueOf(DefectStatus.IGNORE.value()));

        if (hasIgnore) {
            if (CollectionUtils.isNotEmpty(ignoreReasonTypes)) {
                criteriaList.addLast(
                        Criteria.where("status").bits().allSet(DefectStatus.IGNORE.value())
                                .and("ignore_reason_type").in(ignoreReasonTypes)
                );
            } else {
                criteriaList.addLast(
                        Criteria.where("status").bits().allSet(DefectStatus.IGNORE.value())
                );
            }

            statusFilter.remove(String.valueOf(DefectStatus.IGNORE.value()));
            statusFilter.remove(String.valueOf(DefectStatus.IGNORE.value() | DefectStatus.NEW.value()));
        }

        if (CollectionUtils.isNotEmpty(statusFilter)) {
            Set<Integer> condStatusSet = statusFilter.stream()
                    .map(it -> Integer.parseInt(it) | DefectStatus.NEW.value())
                    .collect(Collectors.toSet());
            criteriaList.addFirst(Criteria.where("status").in(condStatusSet));
        }

        // x == 1 or x > 1
        return criteriaList.size() == 1 ? criteriaList.get(0)
                : new Criteria().orOperator(criteriaList.toArray(new Criteria[]{}));
    }


    public void batchUpdateDefectAuthor(long taskId, List<LintDefectV2Entity> defectList, String newAuthor) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED,
                    LintDefectV2Entity.class);
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(
                        Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId())).and("task_id").is(taskId));
                Update update = new Update();
                update.set("author", newAuthor);
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 批量更新告警状态的ignore位
     *
     * @param defectList
     * @param ignoreReasonType
     * @param ignoreReason
     * @param ignoreAuthor
     */
    public void batchUpdateDefectStatusIgnoreBit(
            long taskId, List<LintDefectV2Entity> defectList, int ignoreReasonType,
            String ignoreReason, String ignoreAuthor
    ) {
        batchUpdateDefectStatusIgnoreBit(taskId, defectList, ignoreReasonType, ignoreReason, ignoreAuthor, false);
    }

    /**
     * 批量更新告警状态的ignore位
     *
     * @param defectList
     * @param ignoreReasonType
     * @param ignoreReason
     * @param ignoreAuthor
     */
    public void batchUpdateDefectStatusIgnoreBit(
            long taskId, List<LintDefectV2Entity> defectList, int ignoreReasonType,
            String ignoreReason, String ignoreAuthor, Boolean unsetIgnoreApproval
    ) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED,
                    LintDefectV2Entity.class);
            long currTime = System.currentTimeMillis();
            defectList.forEach(defectEntity -> {
                Update update = new Update();
                update.set("status", defectEntity.getStatus());
                update.set("ignore_time", currTime);
                update.set("ignore_reason_type", ignoreReasonType);
                update.set("ignore_reason", ignoreReason);
                update.set("ignore_author", ignoreAuthor);

                if (BooleanUtils.isTrue(unsetIgnoreApproval)) {
                    update.unset("ignore_approval_id");
                    update.unset("ignore_approval_status");
                }

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
     * 批量更新忽略类型
     *
     * @param taskId
     * @param defectList
     * @param ignoreReasonType
     * @param ignoreReason
     */
    public void batchUpdateIgnoreType(long taskId, List<LintDefectV2Entity> defectList,
            int ignoreReasonType, String ignoreReason) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED,
                    LintDefectV2Entity.class);
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").
                        is(new ObjectId(defectEntity.getEntityId())).and("task_id").is(taskId));
                Update update = new Update();
                update.set("ignore_reason_type", ignoreReasonType);
                update.set("ignore_reason", ignoreReason);
                ops.upsert(query, update);
            });
            ops.execute();
        }
    }

    public void batchIgnoreDefect(long taskId, List<LintDefectV2Entity> defectList,
            int ignoreReasonType, String ignoreReason, String ignoreAuthor) {
        if (CollectionUtils.isEmpty(defectList)) {
            return;
        }
        List<String> ids = defectList.stream().map(LintDefectV2Entity::getEntityId).collect(Collectors.toList());
        Query query = Query.query(Criteria.where("task_id").is(taskId).and("_id").in(ids));
        long curTime = System.currentTimeMillis();
        Update update = new Update();
        update.set("status", ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value());
        update.set("ignore_reason_type", ignoreReasonType);
        update.set("ignore_reason", ignoreReason);
        update.set("ignore_time", curTime);
        update.set("ignore_author", ignoreAuthor);
        defectMongoTemplate.updateMulti(query, update, LintDefectV2Entity.class);
    }

    public long batchExcludeToolNewDefect(Long taskId, String toolName) {
        if (taskId == null || StringUtils.isBlank(toolName)) {
            return 0L;
        }
        Query query = Query.query(Criteria.where("task_id").is(taskId)
                .and("tool_name").in(toolName)
                .and("status").is(DefectStatus.NEW.value()));
        long curTime = System.currentTimeMillis();
        Update update = new Update();
        update.set("status", ComConstants.DefectStatus.NEW.value() | DefectStatus.CHECKER_MASK.value());
        update.set("exclude_time", curTime);
        update.set("updated_date", curTime);
        return defectMongoTemplate.updateMulti(query, update, LintDefectV2Entity.class).getModifiedCount();
    }

    public void batchRollbackIgnoreDefect(long taskId, List<LintDefectV2Entity> defectList) {
        if (CollectionUtils.isEmpty(defectList)) {
            return;
        }
        List<String> ids = defectList.stream().map(LintDefectV2Entity::getEntityId).collect(Collectors.toList());
        Query query = Query.query(Criteria.where("task_id").is(taskId).and("_id").in(ids));
        Update update = new Update();
        update.set("status", ComConstants.DefectStatus.NEW.value());
        update.unset("ignore_reason_type");
        update.unset("ignore_reason");
        update.unset("ignore_time");
        update.unset("ignore_author");
        defectMongoTemplate.updateMulti(query, update, LintDefectV2Entity.class);
    }

    public void batchMarkDefect(long taskId, List<LintDefectV2Entity> defectList, Integer markFlag) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED,
                    LintDefectV2Entity.class);
            long currTime = System.currentTimeMillis();
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(
                        Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId())).and("task_id").is(taskId));
                Update update = new Update();
                update.set("mark", markFlag);
                update.set("mark_time", currTime);
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 前端已经没有按文件聚类查看告警
     *
     * @param queryWarningReq
     * @param defectIdSet
     * @param pkgChecker
     * @param pageNum
     * @param pageSize
     * @param sortField
     * @param sortType
     * @return
     */
    @Deprecated
    public Page<LintFileVO> findDefectFilePageByCondition(
            Map<Long, List<String>> taskToolMap,
            DefectQueryReqVO queryWarningReq,
            Set<String> defectIdSet,
            Set<String> pkgChecker,
            int pageNum,
            int pageSize,
            String sortField,
            Sort.Direction sortType
    ) {
        // 根据查询条件过滤
        Criteria criteria = getQueryCriteria(
                taskToolMap, queryWarningReq, defectIdSet,
                pkgChecker, Sets.newHashSet(), true
        );
        MatchOperation match = Aggregation.match(criteria);
        // 以filePath进行分组，计算文件总数
        GroupOperation group = Aggregation.group("task_id", "tool_name", "file_path");
        CountOperation count = Aggregation.count().as("fileCount");
        Aggregation agg1 = Aggregation.newAggregation(match, group, count)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<LintStatisticVO> result = defectMongoTemplate.aggregate(agg1, "t_lint_defect_v2",
                LintStatisticVO.class);
        List<LintStatisticVO> totalResult = result.getMappedResults();

        if (CollectionUtils.isEmpty(totalResult)) {
            return new Page<>(0, pageNum, pageSize, 0, new ArrayList<>());
        }

        int total = totalResult.get(0).getFileCount();

        // 以filePath进行分组
        GroupOperation group1 = Aggregation.group("task_id", "tool_name", "file_path")
                .last("file_name").as("fileName")
                .last("file_path").as("filePath")
                .last("file_update_time").as("fileUpdateTime")
                .count().as("defectCount")
                .addToSet("checker").as("checkerList")
                .addToSet("severity").as("severityList")
                .addToSet("author").as("authorList");

        // 默认按文件名排列
        if (StringUtils.isEmpty(sortField)) {
            sortField = "fileName";
        }
        if (null == sortType) {
            sortType = Sort.Direction.ASC;
        }
        SortOperation sort = Aggregation.sort(sortType, sortField);
        SkipOperation skip = Aggregation.skip(
                (long) pageSize * ((pageNum <= 0 ? MongoPageHelper.FIRST_PAGE_NUM : pageNum) - 1));
        LimitOperation limit = Aggregation.limit(pageSize);

        Aggregation agg2 = Aggregation.newAggregation(match, group1, sort, skip, limit)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<LintFileVO> queryResult = defectMongoTemplate.aggregate(agg2, "t_lint_defect_v2",
                LintFileVO.class);
        List<LintFileVO> filelist = queryResult.getMappedResults();
        filelist.forEach(lintFileVO -> {
            Set<Integer> severityList = lintFileVO.getSeverityList();
            if (CollectionUtils.isNotEmpty(severityList) && severityList.remove(ComConstants.PROMPT_IN_DB)) {
                severityList.add(ComConstants.PROMPT);
            }
        });
        final Integer pages = (int) Math.ceil(total / (double) pageSize);
        final Page<LintFileVO> pageResult = new Page<>(total, pageNum, pageSize, pages, filelist);
        return pageResult;
    }

    /**
     * 根据状态过滤后获取规则，处理人、文件路径
     */
    public List<LintFileVO> getCheckerAndAuthorAndPath(
            long taskId,
            List<String> toolNameList,
            Set<String> statusSet,
            Set<String> pkgChecker
    ) {
        Criteria rootCriteria = Criteria.where("task_id").is(taskId).and("tool_name").in(toolNameList);

        // 状态过滤
        if (CollectionUtils.isNotEmpty(statusSet)) {
            Set<Integer> condStatusSet = statusSet.stream().map(it -> Integer.valueOf(it) | DefectStatus.NEW.value())
                    .collect(Collectors.toSet());
            rootCriteria.and("status").in(condStatusSet);
        }

        if (CollectionUtils.isNotEmpty(pkgChecker)) {
            rootCriteria.and("checker").in(pkgChecker);
        }

        MatchOperation match = Aggregation.match(rootCriteria);
        GroupOperation group = Aggregation.group("task_id", "tool_name", "file_path")
                .last("file_path").as("filePath")
                .last("url").as("url")
                .last("rel_path").as("relPath")
                .addToSet("checker").as("checkerList")
                .addToSet("author").as("authorList");
        AggregationOptions options = Aggregation.newAggregationOptions().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, group).withOptions(options);
        AggregationResults<LintFileVO> queryResult = defectMongoTemplate.aggregate(agg, "t_lint_defect_v2",
                LintFileVO.class);

        return queryResult.getMappedResults();
    }

    /**
     * 根据传入条件统计文件数
     *
     * @param taskId
     * @param toolNameList
     * @param statusSet
     * @param author
     * @param checker
     * @return
     */
    public long countFileByCondition(
            long taskId,
            List<String> toolNameList,
            Set<Integer> statusSet,
            String author,
            String checker,
            List<Integer> severityList
    ) {
        DefectQueryReqVO request = new DefectQueryReqVO();
        request.setAuthor(author);

        if (CollectionUtils.isNotEmpty(severityList)) {
            request.setSeverity(
                    severityList.stream().map(String::valueOf).collect(Collectors.toSet())
            );
        }

        if (CollectionUtils.isNotEmpty(statusSet)) {
            request.setStatus(statusSet.stream().map(String::valueOf).collect(Collectors.toSet()));
        }

        // TODO
        Criteria criteria = new Criteria();
        MatchOperation match = Aggregation.match(criteria);
        GroupOperation group = Aggregation.group("task_id", "file_path");
        CountOperation count = Aggregation.count().as("fileCount");
        AggregationOptions allowDiskUse = Aggregation.newAggregationOptions().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, group, count).withOptions(allowDiskUse);
        AggregationResults<LintStatisticVO> result =
                defectMongoTemplate.aggregate(agg, "t_lint_defect_v2", LintStatisticVO.class);

        if (CollectionUtils.isNotEmpty(result.getMappedResults())) {
            return result.getMappedResults().get(0).getFileCount();
        }

        return 0L;
    }

    /**
     * 根据规则、处理人、快照、路径、日期过滤后计算各状态告警数
     *
     * @param queryWarningReq
     * @param defectIdsPair first -> mongodb主键, second -> 第三方平台主键id
     * @param pkgChecker
     * @return
     */
    public List<LintDefectGroupStatisticVO> statisticByStatus(
            Map<Long, List<String>> taskToolMap,
            DefectQueryReqVO queryWarningReq,
            Pair<Set<String>, Set<String>> defectIdsPair,
            Set<String> pkgChecker, String projectId, String userId
    ) {
        queryWarningReq.setStatus(null);

        // 根据查询条件过滤
        Criteria criteria = getQueryCriteria(
                taskToolMap,
                queryWarningReq,
                defectIdsPair.getFirst(),
                pkgChecker,
                defectIdsPair.getSecond(),
                true
        );

        MatchOperation match = Aggregation.match(criteria);
        // 以status进行分组
        GroupOperation group = Aggregation.group("task_id", "tool_name", "status")
                .last("status").as("status")
                .count().as("defectCount");

        AggregationOptions options = Aggregation.newAggregationOptions().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, group).withOptions(options);
        long beginTime = System.currentTimeMillis();
        AggregationResults<LintDefectGroupStatisticVO> queryResult = defectMongoTemplate.aggregate(agg,
                "t_lint_defect_v2",
                LintDefectGroupStatisticVO.class);

        if (MapUtils.isNotEmpty(taskToolMap) && taskToolMap.size() > 1) {
            log.info("Multi-Task-Query statistic by status cost: {}, project id: {}, user id: {}",
                    System.currentTimeMillis() - beginTime, projectId, userId);
        }

        return queryResult.getMappedResults();
    }


    /**
     * 根据规则、处理人、快照、路径、日期、状态过滤后计算各严重级别告警数
     *
     * @param queryWarningReq
     * @param defectIdsPair first -> mongodb主键, second -> 第三方平台主键id
     * @param pkgChecker
     * @return
     */
    public List<LintDefectGroupStatisticVO> statisticBySeverity(
            Map<Long, List<String>> taskToolMap, DefectQueryReqVO queryWarningReq,
            Pair<Set<String>, Set<String>> defectIdsPair,
            Set<String> pkgChecker, String projectId, String userId
    ) {
        // 根据查询条件过滤
        Criteria criteria = getQueryCriteria(
                taskToolMap,
                queryWarningReq,
                defectIdsPair.getFirst(),
                pkgChecker,
                defectIdsPair.getSecond(),
                true
        );
        MatchOperation match = Aggregation.match(criteria);

        // 以status进行分组
        GroupOperation group = Aggregation.group("task_id", "tool_name", "severity")
                .last("severity").as("severity")
                .count().as("defectCount");

        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());

        long beginTime = System.currentTimeMillis();
        AggregationResults<LintDefectGroupStatisticVO> queryResult = defectMongoTemplate.aggregate(agg,
                "t_lint_defect_v2",
                LintDefectGroupStatisticVO.class);

        if (MapUtils.isNotEmpty(taskToolMap) && taskToolMap.size() > 1) {
            log.info("Multi-Task-Query statistic by severity cost: {}, project id: {}, user id: {}",
                    System.currentTimeMillis() - beginTime, projectId, userId);
        }

        return queryResult.getMappedResults();
    }

    /**
     * 根据规则、处理人、快照、路径、日期、状态过滤后计算各严重级别告警数
     *
     * @param taskToolMap
     * @param queryWarningReq
     * @param defectIdsPair first -> mongodb主键, second -> 第三方平台主键id
     * @param pkgChecker
     * @return
     */
    @Deprecated
    public List<LintDefectGroupStatisticVO> statisticByDefectType(
            Map<Long, List<String>> taskToolMap, DefectQueryReqVO queryWarningReq, Pair<Set<String>,
            Set<String>> defectIdsPair, Set<String> pkgChecker,
            int defectType
    ) {
        // 根据查询条件过滤
        Set<String> oldDefectType = queryWarningReq.getDefectType();
        queryWarningReq.setDefectType(Sets.newHashSet(String.valueOf(defectType)));
        Criteria criteria = getQueryCriteria(
                taskToolMap,
                queryWarningReq,
                defectIdsPair.getFirst(),
                pkgChecker,
                defectIdsPair.getSecond(),
                true
        );
        queryWarningReq.setDefectType(oldDefectType);

        return statisticByCriteria(criteria);
    }


    public List<LintDefectGroupStatisticVO> statisticOps(
            Map<Long, List<String>> taskToolMap, DefectQueryReqVO queryWarningReq, Pair<Set<String>,
            Set<String>> defectIdsPair, Set<String> pkgChecker, ComConstants.CodeCCDefectOpsType opsType
    ) {
        DefectQueryReqVO defectQueryReqVO = new DefectQueryReqVO();
        BeanUtils.copyProperties(queryWarningReq, defectQueryReqVO);
        // 查询过程中status会被改变，所以需要使用新的实例，避免queryWarningReq被变更
        defectQueryReqVO.setStatus(Sets.newHashSet(queryWarningReq.getStatus()));
        defectQueryReqVO.setOperates(Collections.singletonList(opsType.name()));
        // 根据查询条件过滤
        Criteria criteria = getQueryCriteria(
                taskToolMap,
                defectQueryReqVO,
                defectIdsPair.getFirst(),
                pkgChecker,
                defectIdsPair.getSecond(),
                true
        );
        return statisticByCriteria(criteria);
    }


    private List<LintDefectGroupStatisticVO> statisticByCriteria(Criteria criteria) {
        MatchOperation match = Aggregation.match(criteria);

        // 以status进行分组
        GroupOperation group = Aggregation.group("task_id", "tool_name")
                .count().as("defectCount");

        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<LintDefectGroupStatisticVO> queryResult = defectMongoTemplate
                .aggregate(agg, "t_lint_defect_v2", LintDefectGroupStatisticVO.class);

        return queryResult.getMappedResults();
    }

    /**
     * 批量统计指点任务待修复的告警个数
     *
     * @param taskIdList 任务id
     * @param toolName 工具名
     * @return list
     */
    public List<DefectCountModel> statisticLintDefect(Set<Long> taskIdList, String toolName) {
        Criteria criteria = new Criteria();
        criteria.and("task_id").in(taskIdList).and("status").is(DefectStatus.NEW.value());
        if (StringUtils.isNotEmpty(toolName)) {
            criteria.and("tool_name").is(toolName);
        }
        MatchOperation match = Aggregation.match(criteria);

        GroupOperation group = Aggregation.group("task_id")
                .last("task_id").as("taskId")
                .count().as("defectCount");

        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<DefectCountModel> queryResult =
                defectMongoTemplate.aggregate(agg, "t_lint_defect_v2", DefectCountModel.class);

        return queryResult.getMappedResults();
    }

    /**
     * 批量插入，无序
     *
     * @param insertList
     * @param batchSize 每一批大小
     */
    public void doBatchInsert(List<LintDefectV2Entity> insertList, int batchSize) {
        if (CollectionUtils.isEmpty(insertList)) {
            return;
        }

        if (insertList.size() > batchSize) {
            List<List<LintDefectV2Entity>> partitionList = Lists.partition(insertList, batchSize);
            partitionList.forEach(onePartition ->
                    defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, LintDefectV2Entity.class)
                            .insert(onePartition)
                            .execute()
            );
        } else {
            defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, LintDefectV2Entity.class)
                    .insert(insertList)
                    .execute();
        }
    }

    /**
     * 批量更新忽略审批告警状态
     *
     * @param taskToDefects
     * @param approvalId
     * @param approvalStatus
     */
    public void batchUpdateApprovalInfo(Map<Long, List<String>> taskToDefects, String approvalId,
            Integer approvalStatus) {
        if (taskToDefects == null || taskToDefects.isEmpty()) {
            return;
        }
        List<Criteria> orCriList = new ArrayList<>();
        for (Entry<Long, List<String>> taskToDefect : taskToDefects.entrySet()) {
            if (taskToDefect.getKey() != null && CollectionUtils.isNotEmpty(taskToDefect.getValue())) {
                orCriList.add(Criteria.where("task_id").is(taskToDefect.getKey())
                        .and("_id").in(taskToDefect.getValue()));
            }
        }
        if (orCriList.isEmpty()) {
            return;
        }
        Criteria criteria = new Criteria();
        criteria.orOperator(orCriList.toArray(new Criteria[]{}));
        long curTime = System.currentTimeMillis();
        Update update = new Update();
        update.set("ignore_approval_id", approvalId);
        update.set("ignore_approval_status", approvalStatus);
        update.set("updated_date", curTime);
        Query query = Query.query(criteria);
        defectMongoTemplate.updateMulti(query, update, LintDefectV2Entity.class);
    }

    /**
     * 组装查询条件
     *
     * @return criteria
     */
    @Nullable
    private Criteria generateQueryCondition(@NotNull Map<Long, Set<String>> taskToolsMap, int ignoreTypeId,
            int ignoreStatus) {
        Criteria criteria = getTaskToolsMapQueryCondition(taskToolsMap);
        // 如果没有以上条件,则不查询
        if (criteria == null) {
            return null;
        }
        criteria.and("status").is(ignoreStatus).and("ignore_reason_type").is(ignoreTypeId);
        return criteria;
    }

    private Criteria getTaskToolsMapQueryCondition(@NotNull Map<Long, Set<String>> taskToolsMap) {
        List<Criteria> orCriteriaList = Lists.newArrayList();
        for (Map.Entry<Long, Set<String>> entry : taskToolsMap.entrySet()) {
            Long taskId = entry.getKey();
            orCriteriaList.add(Criteria.where("task_id").is(taskId).and("tool_name").in(entry.getValue()));
        }

        Criteria criteria = new Criteria();
        // 如果没有以上条件,则不查询
        if (CollectionUtils.isEmpty(orCriteriaList)) {
            return null;
        }
        return criteria.orOperator(orCriteriaList.toArray(new Criteria[0]));
    }

    /**
     * 统计指定忽略类型的告警数
     */
    public List<IgnoreTypeStatModel> statisticLintIgnoreDefect(Map<Long, Set<String>> taskToolsMap,
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

        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<IgnoreTypeStatModel> queryResult =
                defectMongoTemplate.aggregate(agg, "t_lint_defect_v2", IgnoreTypeStatModel.class);
        return queryResult.getMappedResults();
    }

    /**
     * 统计指定忽略类型的告警数
     */
    public Long getIgnoreTypeDefectCount(Map<Long, Set<String>> taskToolsMap,
            Integer ignoreTypeId, Integer ignoreStatus) {
        Criteria criteria = generateQueryCondition(taskToolsMap, ignoreTypeId, ignoreStatus);
        if (criteria == null) {
            return 0L;
        }
        Query query = Query.query(criteria);

        return defectMongoTemplate.count(query, LintDefectV2Entity.class);
    }

    /**
     * 统计忽略类型的告警数量
     *
     * @param taskToolsMap
     * @param ignoreTypeIds
     * @return
     */
    public List<IgnoreTypeStatModel> statisticIgnoreDefectByIgnoreTypeId(Map<Long, Set<String>> taskToolsMap,
            Set<Integer> ignoreTypeIds, Integer ignoreStatus) {
        Criteria criteria = getTaskToolsMapQueryCondition(taskToolsMap);
        if (criteria == null) {
            return Collections.emptyList();
        }
        criteria.and("status").is(ignoreStatus)
                .and("ignore_reason_type").in(ignoreTypeIds);

        MatchOperation match = Aggregation.match(criteria);
        GroupOperation group = Aggregation.group("ignore_reason_type")
                .first("ignore_reason_type").as("ignoreTypeId")
                .addToSet("task_id").as("taskIdSet")
                .count().as("defectCount");
        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<IgnoreTypeStatModel> queryResult =
                defectMongoTemplate.aggregate(agg, "t_lint_defect_v2", IgnoreTypeStatModel.class);
        return queryResult.getMappedResults();
    }

    public List<Long> statisticTaskIdByNewDefect(Map<Long, Set<String>> taskToolsMap) {
        Criteria criteria = getTaskToolsMapQueryCondition(taskToolsMap);
        if (criteria == null) {
            return Collections.emptyList();
        }
        criteria.and("status").is(DefectStatus.NEW.value());
        return statisticTaskIdByCri(criteria);
    }

    public List<Long> statisticTaskIdByNewDefectId(Set<String> entityIds) {
        Criteria criteria = Criteria.where("_id").in(entityIds.stream().map(ObjectId::new).collect(Collectors.toSet()))
                .and("status").is(DefectStatus.NEW.value());
        return statisticTaskIdByCri(criteria);
    }

    private List<Long> statisticTaskIdByCri(Criteria criteria) {
        MatchOperation match = Aggregation.match(criteria);
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("taskId")
                .count().as("defectCount");
        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<IgnoreTypeStatModel> queryResult =
                defectMongoTemplate.aggregate(agg, "t_lint_defect_v2", IgnoreTypeStatModel.class);
        List<IgnoreTypeStatModel> results = queryResult.getMappedResults();
        if (CollectionUtils.isEmpty(results)) {
            return Collections.emptyList();
        }
        return results.stream().map(IgnoreTypeStatModel::getTaskId).collect(Collectors.toList());
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
        GroupOperation group = Aggregation.group("author")
                .first("author").as("ignoreAuthor");

        Aggregation agg = Aggregation.newAggregation(match, group)
                .withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
        AggregationResults<IgnoreTypeStatModel> queryResult =
                defectMongoTemplate.aggregate(agg, "t_lint_defect_v2", IgnoreTypeStatModel.class);
        return queryResult.getMappedResults();
    }

    /**
     * 分页获取待修复的告警
     */
    public List<LintDefectV2Entity> findHistoryNewDefectByPageNoSkip(Long taskId, String toolName,
            Long newDefectJudgeTime, Integer pageSize) {
        List<Criteria> criList = new ArrayList<>();
        criList.add(Criteria.where("task_id").is(taskId).and("status").is(DefectStatus.NEW.value())
                .and("tool_name").is(toolName));
        criList.add(Criteria.where("line_update_time").lt(newDefectJudgeTime).gt(0));
        Query query = Query.query(new Criteria().andOperator(criList.toArray(new Criteria[]{})));
        query.limit(pageSize);
        return defectMongoTemplate.find(query, LintDefectV2Entity.class);
    }

    public List<LintDefectV2Entity> findIgnoreDefectByPageNoSkip(Long taskId, String toolName,
            Integer ignoreType, Integer pageSize) {
        Query query = Query.query(Criteria.where("task_id").is(taskId)
                .and("tool_name").is(toolName)
                .and("status").is(ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value())
                .and("ignore_reason_type").is(ignoreType));
        query.limit(pageSize);
        return defectMongoTemplate.find(query, LintDefectV2Entity.class);
    }

    /**
     * 查询存量告警
     *
     * @param taskId
     * @param toolName
     * @param buildId
     * @param ignoreReasonType
     * @return
     */
    public List<LintDefectV2Entity> findIgnoreDefect(Long taskId, String toolName, String buildId,
            Integer ignoreReasonType, Boolean isFilterPath) {

        Criteria cri = getCommonIgnoreTypeCri(taskId, toolName, ignoreReasonType);
        if (isFilterPath != null && isFilterPath) {
            cri.and("ignore_build_id").is(buildId);
        }
        Query query = Query.query(cri);
        query.fields().include("entityId", "task_id", "status", "checker", "ignore_reason_type", "severity",
                "revision", "branch", "sub_module", "line_num");
        return defectMongoTemplate.find(query, LintDefectV2Entity.class);
    }

    private Criteria getCommonIgnoreTypeCri(Long taskId, String toolName, Integer ignoreReasonType) {
        return Criteria.where("task_id").is(taskId)
                .and("tool_name").is(toolName)
                .and("ignore_reason_type").is(ignoreReasonType)
                .and("status").bits().allSet(DefectStatus.IGNORE.value());
    }

    public List<LintDefectV2Entity> findAllIgnoreDefectForSnapshot(Long taskId, String toolName,
            Integer ignoreReasonType) {
        Query query = Query.query(getCommonIgnoreTypeCri(taskId, toolName, ignoreReasonType));
        query.fields().include("entityId", "task_id", "status", "checker", "ignore_reason_type", "severity",
                "revision", "branch", "sub_module", "line_num");
        return defectMongoTemplate.find(query, LintDefectV2Entity.class);
    }


    /**
     * 批量更新告警状态
     *
     * @param taskId
     * @param defectList
     */
    public void batchUpdateDefectStatusFixedBit(long taskId, List<LintDefectV2Entity> defectList) {
        if (CollectionUtils.isEmpty(defectList)) {
            return;
        }

        BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, LintDefectV2Entity.class);

        for (LintDefectV2Entity entity : defectList) {
            Update update = new Update();
            update.set("status", entity.getStatus());
            update.set("fixed_time", entity.getFixedTime());
            update.set("fixed_build_number", entity.getFixedBuildNumber());
            update.set("exclude_time", entity.getExcludeTime());
            update.set("file_path", entity.getFilePath());
            Query query = Query.query(
                    Criteria.where("_id").is(entity.getEntityId()).and("task_id").is(taskId)
            );

            ops.updateOne(query, update);
        }

        ops.execute();
    }

    public void batchUpdateDefectDetail(long taskId, String toolName, List<LintDefectV2Entity> defectList) {
        if (CollectionUtils.isEmpty(defectList)) {
            return;
        }

        BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, LintDefectV2Entity.class);

        for (LintDefectV2Entity entity : defectList) {
            Pair<Query, Update> pair = getDefectDetailUpdateInfo(taskId, toolName, entity);
            Query query = pair.getFirst();
            Update update = pair.getSecond();
            ops.updateOne(query, update);
        }

        ops.execute();
    }

    public void updateDefectDetailOneByOne(Long taskId, String toolName, List<LintDefectV2Entity> defectList) {
        if (CollectionUtils.isEmpty(defectList)) {
            return;
        }

        for (LintDefectV2Entity entity : defectList) {
            try {
                Pair<Query, Update> pair = getDefectDetailUpdateInfo(taskId, toolName, entity);
                Query query = pair.getFirst();
                Update update = pair.getSecond();
                defectMongoTemplate.updateFirst(query, update, LintDefectV2Entity.class);
            } catch (Exception e) {
                log.error("fail to update defect: {}, {}, {}", taskId, toolName, entity.getId(), e);
            }
        }
    }

    private Pair<Query, Update> getDefectDetailUpdateInfo(Long taskId, String toolName, LintDefectV2Entity entity) {
        Update update = new Update();
        update.set("line_num", entity.getLineNum());
        update.set("message", entity.getMessage());
        update.set("severity", entity.getSeverity());
        update.set("display_type", entity.getDisplayType());
        update.set("display_category", entity.getDisplayCategory());
        update.set("defect_instances", entity.getDefectInstances());
        Query query = Query.query(
                Criteria.where("task_id").is(taskId)
                        .and("tool_name").is(toolName)
                        .and("id").is(entity.getId())
        );

        return Pair.of(query, update);
    }


    public List<LintDefectV2Entity> getDefectWithMarkInfo(Long taskId, String toolName, Set<String> ids) {
        Query query = Query.query(
                Criteria.where("task_id").is(taskId)
                        .and("tool_name").is(toolName)
                        .and("id").in(ids)
        );
        query.fields().include("mark", "id");
        return defectMongoTemplate.find(query, LintDefectV2Entity.class);
    }

    public List<LintDefectV2Entity> findDataReportFieldByTaskIdAndToolName(long taskId, String toolName) {
        Query query = Query.query(Criteria.where("task_id").is(taskId).and("tool_name").is(toolName));
        query.fields().include("task_id", "tool_name", "status", "author",
                "severity", "create_time", "fixed_time", "ignore_time", "exclude_time");
        return defectMongoTemplate.find(query, LintDefectV2Entity.class);
    }

    public List<Long> findTaskIdsByEntityIds(Set<String> entityIds) {
        Criteria cri = Criteria.where("_id").in(entityIds.stream().map(ObjectId::new).collect(Collectors.toSet()));
        MatchOperation match = Aggregation.match(cri);
        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id");
        Aggregation agg = Aggregation.newAggregation(match, group);
        AggregationResults<LintDefectV2Entity> queryResult =
                defectMongoTemplate.aggregate(agg, "t_lint_defect_v2", LintDefectV2Entity.class);
        if (CollectionUtils.isEmpty(queryResult.getMappedResults())) {
            return Collections.emptyList();
        }
        return queryResult.getMappedResults().stream().map(LintDefectV2Entity::getTaskId).collect(Collectors.toList());
    }

    /**
     * 聚类后根据文件信息更新告警
     *
     * @param defectList
     */
    public void batchUpdateByFile(List<LintDefectV2Entity> defectList) {
        if (CollectionUtils.isEmpty(defectList)) {
            return;
        }

        long dtNow = System.currentTimeMillis();
        List<List<LintDefectV2Entity>> partitionList = Lists.partition(defectList, 3_0000);

        for (List<LintDefectV2Entity> onePartition : partitionList) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED,
                    LintDefectV2Entity.class);

            for (LintDefectV2Entity defect : onePartition) {
                Update update = new Update();
                update.set("updated_date", dtNow);
                update.set("status", defect.getStatus());
                update.set("severity", defect.getSeverity());
                updateSetWithNullCheck("fixed_time", defect.getFixedTime(), update);
                updateSetWithNullCheck("fixed_build_number", defect.getFixedBuildNumber(), update);
                updateSetWithNullCheck("branch", defect.getBranch(), update);
                Query query = Query.query(Criteria.where("_id").is(defect.getEntityId()));
                ops.updateOne(query, update);
            }

            ops.execute();
        }
    }

    /**
     * 按指定条件获取忽略状态的告警列表
     *
     * @param taskId
     * @param toolName
     * @param pageable
     * @param fieldMap
     * @return
     */
    public List<LintDefectV2Entity> findIgnoreDefects(
            Long taskId, String toolName, Pageable pageable,
            Map<String, Boolean> fieldMap
    ) {
        Query query;
        if (MapUtils.isNotEmpty(fieldMap)) {
            Document fieldsObj = new Document();
            fieldMap.putAll(fieldMap);
            query = new BasicQuery(new Document(), fieldsObj);
        } else {
            query = new Query();
        }

        query.addCriteria(
                Criteria.where("task_id").is(taskId)
                        .and("tool_name").is(toolName)
                        .and("status").is(DefectStatus.NEW.value() | DefectStatus.IGNORE.value())
        );
        query.with(pageable);

        return defectMongoTemplate.find(query, LintDefectV2Entity.class);
    }

    private void updateSetWithNullCheck(String key, Object value, Update update) {
        if (value == null) {
            update.unset(key);
        } else {
            update.set(key, value);
        }
    }

    /**
     * 根据修复时间查询修复过告警的任务id集合
     *
     * @param startTime 起始时间
     * @param endTime 结束时间
     * @param taskIds 任务id集合
     * @param toolName 工具名称
     * @return 修复过告警的任务id
     */
    public Set<Long> findStatisticWithFixedTime(Long startTime, Long endTime,
            Set<Long> taskIds, String toolName) {
        int fixed = ComConstants.DefectStatus.FIXED.value() | ComConstants.DefectStatus.NEW.value();
        Criteria criteria = Criteria.where("task_id").in(taskIds)
                .and("tool_name").is(toolName)
                .and("fixed_time").gte(startTime).lte(endTime)
                .and("status").is(fixed);

        MatchOperation match = Aggregation.match(criteria);

        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("taskId")
                .count().as("defectCount");
        Aggregation aggregation = Aggregation.newAggregation(match, group);
        AggregationResults<StatisticVO> queryResults = defectMongoTemplate.aggregate(aggregation,
                "t_lint_defect_v2", StatisticVO.class);
        return queryResults.getMappedResults().stream().map(StatisticVO::getTaskId).collect(Collectors.toSet());

    }

    /**
     * 根据忽略告警类型是误报类型查询每个任务对应的误报问题数
     *
     * @param startTime 起始时间
     * @param endTime 结束时间
     * @param taskIds 任务ids集合
     * @param toolName 工具名称
     * @param ignoreReasonType 忽略类型
     * @return 每个任务对应的问题数map
     */
    public List<StatisticVO> findPositiveStatistic(Long startTime, Long endTime, Set<Long> taskIds, String toolName,
            Integer ignoreReasonType) {
        int ignore = ComConstants.DefectStatus.IGNORE.value() | ComConstants.DefectStatus.NEW.value();
        Criteria criteria = Criteria.where("task_id").in(taskIds)
                .and("tool_name").is(toolName)
                .and("ignore_time").gte(startTime).lte(endTime)
                .and("ignore_reason_type").is(ignoreReasonType)
                .and("status").is(ignore);

        MatchOperation match = Aggregation.match(criteria);

        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("taskId")
                .count().as("defectCount");
        Aggregation aggregation = Aggregation.newAggregation(match, group);
        AggregationResults<StatisticVO> queryResults = defectMongoTemplate.aggregate(aggregation,
                "t_lint_defect_v2", StatisticVO.class);
        return queryResults.getMappedResults();
    }


    /**
     * 根据TaskId与EntityId查询告警列表
     *
     * @param taskId
     * @param defectMongoIdSet
     * @return
     */
    public List<LintDefectV2Entity> findMajorFieldByTaskIdAndEntityIds(Long taskId, Set<String> defectMongoIdSet,
                                                                       List<Integer> statuses) {
        Query query = Query.query(Criteria.where("task_id").is(taskId).and("_id").in(
                defectMongoIdSet.stream().map(ObjectId::new).collect(Collectors.toSet()))
                .and("status").in(statuses));
        query.fields().include("_id", "task_id", "tool_name", "rel_path", "file_path", "checker",
                "line_num", "message", "revision", "author");
        return defectMongoTemplate.find(query, LintDefectV2Entity.class);
    }

    /**
     * 更新审批信息
     * @param taskIds
     * @param ignoreApprovalId
     * @param ignoreApprovalStatus
     * @param ignoreTypeId
     * @param ignoreReason
     * @param userName
     * @return
     */
    public Long updateIgnoreApprovalResultByTaskIdsAndApprovalId(List<Long> taskIds, String ignoreApprovalId,
            Integer ignoreApprovalStatus, Integer ignoreTypeId, String ignoreReason, String userName) {
        if (CollectionUtils.isEmpty(taskIds) || StringUtils.isEmpty(ignoreApprovalId)) {
            return 0L;
        }
        Query query = Query.query(Criteria.where("task_id").in(taskIds)
                .and("ignore_approval_id").is(ignoreApprovalId));
        Update update = Update.update("ignore_approval_status", ignoreApprovalStatus)
                .set("updated_date", System.currentTimeMillis())
                .set("updated_by", userName);
        if (ignoreApprovalStatus != null && ignoreApprovalStatus == ApproverStatus.SUBMIT_SUCC.status()) {
            // 更新忽略状态
            update.set("status", DefectStatus.NEW.value() | DefectStatus.IGNORE.value())
                    .set("ignore_reason_type", ignoreTypeId)
                    .set("ignore_time", System.currentTimeMillis())
                    .set("ignore_reason", ignoreReason)
                    .set("ignore_author", userName);
        }
        return defectMongoTemplate.updateMulti(query, update, LintDefectV2Entity.class).getModifiedCount();
    }

    public Set<String> filterHasCodeCommonDefectId(List<Long> taskIds, List<String> defectIds) {
        if (CollectionUtils.isEmpty(taskIds) || CollectionUtils.isEmpty(defectIds)) {
            return Collections.emptySet();
        }
        List<ObjectId> objectIds = defectIds.stream().map(ObjectId::new).collect(Collectors.toList());
        Query query = Query.query(Criteria.where("task_id").in(taskIds)
                .and("_id").in(objectIds).and("code_comment").exists(true));
        query.fields().include("_id");
        List<LintDefectV2Entity> entities = defectMongoTemplate.find(query, LintDefectV2Entity.class);
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptySet();
        }
        return entities.stream().map(LintDefectV2Entity::getEntityId).collect(Collectors.toSet());
    }

    public void deleteCommentRelated(String defectId, String userName) {
        Query query = Query.query(Criteria.where("_id").in(new ObjectId(defectId)));
        Update update = Update.update("updated_date", System.currentTimeMillis())
                .set("updated_by", userName)
                .unset("code_comment");
        defectMongoTemplate.updateFirst(query, update, LintDefectV2Entity.class);
    }

    /**
     * 在指定taskIds范围下查询有安全漏洞的任务
     * @param taskIdSet 任务范围
     * @return list
     */
    public List<Long> getUnsafeTaskIdListByTaskIds(Collection<Long> taskIdSet, List<String> securityToolNameList) {
        ArrayList<Long> result = new ArrayList<>();
        List<List<Long>> taskIdsList = Lists.partition(new ArrayList<>(taskIdSet), ComConstants.COMMON_NUM_10000);
        for (List<Long> taskIds : taskIdsList) {
            Query query = Query.query(Criteria.where("task_id").in(taskIds));
            query.addCriteria(Criteria.where("tool_name").in(securityToolNameList));
            query.addCriteria(Criteria.where("status").in(ComConstants.DefectStatus.NEW.value()));
            List<Long> unsafeTaskId = defectMongoTemplate.findDistinct(query, "task_id", COLLECTION_NAME, Long.class);
            result.addAll(unsafeTaskId);
        }
        return result;
    }
}
