/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 Tencent. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.dao.core.mongotemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.AddFieldOperation;
import com.tencent.bk.codecc.defect.dao.CheckerListQueryParams;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CustomCheckerProjectRelationshipRepository;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.CustomCheckerProjectRelationshipEntity;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.CheckerDetailListQueryReqVO;
import com.tencent.bk.codecc.defect.vo.enums.CheckerListSortType;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSource;
import com.tencent.devops.common.api.checkerset.CheckerPropVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * 规则详情持久类
 *
 * @version V1.0
 * @date 2019/12/26
 */
@Repository
public class CheckerDetailDao {

    @Autowired
    private MongoTemplate defectCoreMongoTemplate;

    @Autowired
    private CustomCheckerProjectRelationshipRepository customCheckerProjectRelationshipRepository;

    public Long countByToolName(String toolName) {
        Criteria criteria = Criteria.where("tool_name").is(toolName);
        Query query = new Query(criteria);

        return defectCoreMongoTemplate.count(query, CheckerDetailEntity.class);
    }

    /**
     * 按照复合条件查询规则，使用时传入projectId将过滤项目不可见的用户自定义规则
     *
     * @param checkerListQueryParams
     * @return
     */
    // NOCC:CCN_threshold(设计如此:)
    public List<CheckerDetailEntity> findByComplexCheckerCondition(CheckerListQueryParams checkerListQueryParams,
                                                                   Integer pageNum, Integer pageSize,
                                                                   Sort.Direction sortType,
                                                                   CheckerListSortType sortField) {

        //由于需要根据是否选中排序，所以采用聚合查询
        Criteria criteria = new Criteria();
        List<Criteria> andCriteria = new ArrayList<>();
        //去除多余的严重等级数据
        if (StringUtils.isNotBlank(checkerListQueryParams.getKeyWord())) {
            List<Criteria> keywordCriteria = new ArrayList<>();
            keywordCriteria.add(Criteria.where("checker_desc").regex(checkerListQueryParams.getKeyWord()));
            keywordCriteria.add(Criteria.where("checker_key").regex(checkerListQueryParams.getKeyWord()));
            andCriteria.add(new Criteria().orOperator(keywordCriteria.toArray(new Criteria[0])));
        }
        if (CollectionUtils.isNotEmpty(checkerListQueryParams.getCheckerLanguage())) {
            andCriteria.add(Criteria.where("checker_language")
                .elemMatch(new Criteria().in(checkerListQueryParams.getCheckerLanguage())));
        }
        if (CollectionUtils.isNotEmpty(checkerListQueryParams.getCheckerCategory())) {
            andCriteria.add(Criteria.where("checker_category")
                .in(checkerListQueryParams.getCheckerCategory().stream().map(Enum::name).collect(Collectors.toSet())));
        }
        if (CollectionUtils.isNotEmpty(checkerListQueryParams.getTag())) {
            andCriteria.add(Criteria.where("checker_tag")
                .elemMatch(new Criteria().in(checkerListQueryParams.getTag())));
        }
        if (CollectionUtils.isNotEmpty(checkerListQueryParams.getSeverity())) {
            andCriteria.add(Criteria.where("severity").in(checkerListQueryParams.getSeverity().stream().map(sev -> {
                if (Integer.parseInt(sev) == ComConstants.PROMPT) {
                    return ComConstants.PROMPT_IN_DB;
                } else {
                    return Integer.valueOf(sev);
                }
            }).collect(Collectors.toList())));
        } else {
            andCriteria.add(Criteria.where("severity").in(Arrays.asList(ComConstants.SERIOUS, ComConstants.NORMAL,
                ComConstants.PROMPT, ComConstants.PROMPT_IN_DB)));
        }
        if (CollectionUtils.isNotEmpty(checkerListQueryParams.getEditable())) {
            andCriteria.add(Criteria.where("editable").in(checkerListQueryParams.getEditable()));
        }
        if (CollectionUtils.isNotEmpty(checkerListQueryParams.getCheckerRecommend())) {
            andCriteria.add(Criteria.where("checker_recommend")
                .in(checkerListQueryParams.getCheckerRecommend().stream().map(Enum::name).collect(Collectors.toSet())));
        }
        /*
         * 过滤项目不可见规则:如果是op的请求，则无需添加过滤条件;如果是用户请求则需要添加过滤条件
         * 根据projectId查询项目关联可见的用户自定义规则列表
         * 添加查询筛选条件：非用户自定义规则或项目可见的用户自定义规则
         */
        String projectId = checkerListQueryParams.getProjectId();
        if (!checkerListQueryParams.getIsOp() && StringUtils.isNotBlank(projectId)) {
            // 查询项目可见的所有用户自定义规则列表
            List<CustomCheckerProjectRelationshipEntity> customCheckerProjectRelationshipEntityList =
                customCheckerProjectRelationshipRepository.findByProjectId(projectId);
            // 获取项目可见的用户自定义规则名称列表
            Set<String> projectCustomCheckerNames = customCheckerProjectRelationshipEntityList.stream()
                .map(CustomCheckerProjectRelationshipEntity::getCheckerName)
                .collect(Collectors.toSet());

            // 添加过滤条件
            Criteria checkerVisibleOrCriteria = new Criteria().orOperator(
                Criteria.where("checker_source").ne(CheckerSource.CUSTOM.name()),
                Criteria.where("checker_key").in(projectCustomCheckerNames)
            );
            andCriteria.add(checkerVisibleOrCriteria);
        }
        if (CollectionUtils.isNotEmpty(checkerListQueryParams.getCheckerSource())) {
            List<Criteria> checkerSourceCriteria = new ArrayList<>();
            // 检查是否包含 CUSTOM
            if (checkerListQueryParams.getCheckerSource().contains(CheckerSource.CUSTOM)) {
                checkerSourceCriteria.add(Criteria.where("checker_source").is(CheckerSource.CUSTOM.name()));
            }

            // 检查是否包含 DEFAULT 或者为 null 或者不存在
            if (checkerListQueryParams.getCheckerSource().contains(CheckerSource.DEFAULT)) {
                checkerSourceCriteria.add(Criteria.where("checker_source").is(CheckerSource.DEFAULT.name()));
                checkerSourceCriteria.add(Criteria.where("checker_source").exists(false));
                checkerSourceCriteria.add(Criteria.where("checker_source").is(null));
            }

            // 组合checkerSource查询条件
            Criteria checkerSourceOrCriteria = new Criteria().orOperator(
                checkerSourceCriteria.toArray(new Criteria[0])
            );
            andCriteria.add(checkerSourceOrCriteria);
        }
        if (CollectionUtils.isNotEmpty(checkerListQueryParams.getCheckerSetSelected())
            && CollectionUtils.isNotEmpty(checkerListQueryParams.getSelectedCheckerKey())) {
            List<Criteria> criteriaList = new ArrayList<>();
            if (checkerListQueryParams.getCheckerSetSelected().contains(true)) {
                criteriaList.add(Criteria.where("checker_key").in(checkerListQueryParams.getSelectedCheckerKey()));
            }
            if (checkerListQueryParams.getCheckerSetSelected().contains(false)) {
                criteriaList.add(Criteria.where("checker_key").nin(checkerListQueryParams.getSelectedCheckerKey()));
            }
            andCriteria.add(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
        }

        // 如果工具筛选不为空
        if (CollectionUtils.isNotEmpty(checkerListQueryParams.getToolName())) {
            // 若工具灰度配置不为空
            if (MapUtils.isNotEmpty(checkerListQueryParams.getToolIntegratedStatusMap())) {
                List<Criteria> secondCriteria = Lists.newArrayList();
                // 则遍历工具
                for (String tool : checkerListQueryParams.getToolName()) {
                    // 优先取灰度配置，默认是生产状态(P)
                    Integer toolStatus = checkerListQueryParams.getToolIntegratedStatusMap()
                        .getOrDefault(tool, ToolIntegratedStatus.P.value());
                    List<Integer> checkerVersions = ParamUtils.getCheckerVersionListByToolStatus(toolStatus);
                    secondCriteria.add(Criteria.where("tool_name").is(tool).and("checker_version").in(checkerVersions));
                }
                if (CollectionUtils.isNotEmpty(secondCriteria)) {
                    andCriteria.add(new Criteria().orOperator(secondCriteria.toArray(new Criteria[0])));
                }
            } else {
                // 若灰度配置也为空，默认筛选指定工具的生产状态规则
                andCriteria.add(Criteria.where("tool_name")
                    .in(checkerListQueryParams.getToolName()).and("checker_version")
                    .in(ParamUtils.getCheckerVersionListByToolStatus(ToolIntegratedStatus.P.value())));
            }
        } else {
            // 若工具筛选为空，但灰度配置不为空
            if (MapUtils.isNotEmpty(checkerListQueryParams.getToolIntegratedStatusMap())) {
                // 没工具筛选，只按灰度配置的版本来，默认生产状态，再额外加上灰度配置
                List<Criteria> secondCriteria =
                    Lists.newArrayList(Criteria.where("checker_version").is(ToolIntegratedStatus.P.value()));
                for (Map.Entry<String, Integer> entry :
                    checkerListQueryParams.getToolIntegratedStatusMap().entrySet()) {
                    Integer status = entry.getValue();
                    if (status < 0) {
                        List<Integer> checkerVersions = ParamUtils.getCheckerVersionListByToolStatus(status);
                        secondCriteria.add(Criteria.where("tool_name").is(entry.getKey()).and("checker_version")
                            .in(checkerVersions));
                    }
                }
                andCriteria.add(new Criteria().orOperator(secondCriteria.toArray(new Criteria[0])));
            } else {
                // 若灰度配置也为空，默认生产状态
                andCriteria.add(Criteria.where("checker_version")
                    .in(ParamUtils.getCheckerVersionListByToolStatus(ToolIntegratedStatus.P.value())));
            }
        }

        if (CollectionUtils.isNotEmpty(andCriteria)) {
            criteria.andOperator(andCriteria.toArray(new Criteria[0]));
        }

        MatchOperation match = Aggregation.match(criteria);

        //查询
        Aggregation agg;

        if (CollectionUtils.isNotEmpty(checkerListQueryParams.getSelectedCheckerKey())) {
            //添加字段
            AddFieldOperation addField = new AddFieldOperation(new Document("checkerSetSelected",
                new Document("$in", Arrays.asList("$checker_key", checkerListQueryParams.getSelectedCheckerKey()))));
            if (null != pageNum || null != pageSize || null != sortType || null != sortField) {
                Integer queryPageNum = pageNum == null || pageNum - 1 < 0 ? 0 : pageNum - 1;
                Integer queryPageSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;
                Sort.Direction querySortType = null == sortType ? Sort.Direction.ASC : sortType;
                String querySortField = null == sortField ? CheckerListSortType.checkerKey.getName() :
                    sortField.getName();
                //根据是否选中排序
                SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "checkerSetSelected").and(querySortType,
                    querySortField);
                SkipOperation skip = Aggregation.skip(Long.valueOf(queryPageNum * queryPageSize));
                LimitOperation limit = Aggregation.limit(queryPageSize);
                agg = Aggregation.newAggregation(match, addField, sort, skip, limit);
            } else {
                SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "checkerSetSelected");
                agg = Aggregation.newAggregation(match, addField, sort);
            }
        } else {
            if (null != pageNum || null != pageSize || null != sortType || null != sortField) {
                Integer queryPageNum = pageNum == null || pageNum - 1 < 0 ? 0 : pageNum - 1;
                Integer queryPageSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;
                Sort.Direction querySortType = null == sortType ? Sort.Direction.ASC : sortType;
                String querySortField = null == sortField ? CheckerListSortType.checkerKey.getName() :
                    sortField.getName();
                SortOperation sort = Aggregation.sort(querySortType, querySortField);
                SkipOperation skip = Aggregation.skip(Long.valueOf(queryPageNum * queryPageSize));
                LimitOperation limit = Aggregation.limit(queryPageSize);
                agg = Aggregation.newAggregation(match, sort, skip, limit);
            } else {
                agg = Aggregation.newAggregation(match);
            }
        }

        AggregationResults<CheckerDetailEntity> queryResult = defectCoreMongoTemplate.aggregate(agg, "t_checker_detail",
            CheckerDetailEntity.class);

        return queryResult.getMappedResults();

    }

    /**
     * 根据工具名查找CheckerDetail
     *
     * @param toolName
     * @param pageable
     * @return
     */
    public Page<CheckerDetailEntity> findCheckerDetailByToolName(String toolName, @NotNull Pageable pageable) {
        Criteria criteria = Criteria.where("tool_name").is(toolName);
        //总条数
        long totalCount = defectCoreMongoTemplate.count(new Query(criteria), "t_checker_detail");
        // 以tool_name进行过滤
        MatchOperation match = Aggregation.match(criteria);
        // 分页排序
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        SortOperation sort = Aggregation.sort(pageable.getSort());
        SkipOperation skip = Aggregation.skip((long) (pageNumber * pageSize));
        LimitOperation limit = Aggregation.limit(pageSize);
        Aggregation agg = Aggregation.newAggregation(match, sort, skip, limit);
        AggregationResults<CheckerDetailEntity> queryResults =
                defectCoreMongoTemplate.aggregate(agg, "t_checker_detail", CheckerDetailEntity.class);
        // 计算总页数
        int totalPageNum = 0;
        if (totalCount > 0) {
            totalPageNum = ((int) totalCount + pageSize - 1) / pageSize;
        }
        return new Page<>(totalCount, pageNumber + 1, pageSize, totalPageNum, queryResults.getMappedResults());
    }

    /**
     * 根据工具和规则key查询规则
     *
     * @param toolCheckerMap
     * @return
     */
    public List<CheckerDetailEntity> findByToolNameAndCheckers(Map<String, List<CheckerPropVO>> toolCheckerMap) {
        Criteria criteria = new Criteria();
        List<Criteria> orCriteriaList = Lists.newArrayList();
        toolCheckerMap.forEach((toolName, checkerList) -> {
            Set<String> checkers = checkerList.stream().map(CheckerPropVO::getCheckerKey).collect(Collectors.toSet());
            orCriteriaList.add(Criteria.where("tool_name").is(toolName).and("checker_key").in(checkers));
        });

        if (CollectionUtils.isNotEmpty(orCriteriaList)) {
            criteria.orOperator(orCriteriaList.toArray(new Criteria[0]));
        }

        Document fieldsObj = new Document();
        fieldsObj.put("checker_version", true);
        fieldsObj.put("tool_name", true);
        fieldsObj.put("checker_key", true);
        Query query = new BasicQuery(new Document(), fieldsObj);

        query.addCriteria(criteria);
        List<CheckerDetailEntity> checkerList = defectCoreMongoTemplate.find(query, CheckerDetailEntity.class);
        return checkerList;
    }

    /**
     * 根据工具和key查询规则详情
     *
     * @param checkerPropVOList
     * @return
     */
    public List<CheckerDetailEntity> findByToolNameAndCheckerKey(List<CheckerPropVO> checkerPropVOList) {
        if (CollectionUtils.isEmpty(checkerPropVOList)) {
            return Lists.newArrayList();
        }

        List<Criteria> orCriteriaList = Lists.newArrayList();
        for (CheckerPropVO vo : checkerPropVOList) {
            orCriteriaList.add(
                    Criteria.where("tool_name").is(vo.getToolName())
                            .and("checker_key").is(vo.getCheckerKey())
            );
        }

        Document fieldsObj = new Document();
        fieldsObj.put("checker_version", true);
        fieldsObj.put("tool_name", true);
        fieldsObj.put("checker_key", true);
        fieldsObj.put("props", true);
        fieldsObj.put("editable", true);
        fieldsObj.put("checker_source", true);

        Query query = new BasicQuery(new Document(), fieldsObj);
        query.addCriteria(new Criteria().orOperator(orCriteriaList.toArray(new Criteria[0])));

        return defectCoreMongoTemplate.find(query, CheckerDetailEntity.class);
    }

    /**
     * 根据工具和规则key查询规则
     *
     * @param toolCheckerList
     * @return
     */
    public List<CheckerDetailEntity> findByToolNameAndCheckerNames(
            List<CheckerDetailListQueryReqVO.ToolCheckers> toolCheckerList) {

        List<Criteria> orCriteriaList = Lists.newArrayList();
        toolCheckerList.forEach(it ->
                orCriteriaList.add(Criteria.where("tool_name").is(it.getToolName())
                        .and("checker_key").in(it.getCheckerList()))
        );

        Criteria criteria = new Criteria();
        if (CollectionUtils.isNotEmpty(orCriteriaList)) {
            criteria.orOperator(orCriteriaList.toArray(new Criteria[0]));
        }

        Query query = new Query();
        query.addCriteria(criteria);
        List<CheckerDetailEntity> checkerList = defectCoreMongoTemplate.find(query, CheckerDetailEntity.class);
        return checkerList;
    }

    /**
     * 根据传入参数条件查询，并工具名去重
     *
     * @param toolNameList
     * @param checkerCategoryList 数据库对应是枚举的name()，请看@see
     * @return
     */
    public List<String> distinctToolNameByCheckerCategoryInAndToolNameIn(
            List<String> toolNameList,
            List<String> checkerCategoryList
    ) {
        if (CollectionUtils.isEmpty(toolNameList)
                && CollectionUtils.isEmpty(checkerCategoryList)) {
            return Lists.newArrayList();
        }

        Query query = new Query();

        if (CollectionUtils.isNotEmpty(toolNameList)) {
            query.addCriteria(Criteria.where("tool_name").in(toolNameList));
        }

        if (CollectionUtils.isNotEmpty(checkerCategoryList)) {
            query.addCriteria(Criteria.where("checker_category").in(checkerCategoryList));
        }

        List<String> retTools = defectCoreMongoTemplate.findDistinct(
                query,
                "tool_name",
                CheckerDetailEntity.class,
                String.class
        );

        return retTools;
    }

    /**
     * 根据工具以及规则维度获取规则名字集合
     *
     * @param toolNameList
     * @param checkerCategoryList 数据库对应是枚举的name()，请看@see
     * @param checkerKeyList
     * @return
     * @see com.tencent.bk.codecc.defect.vo.enums.CheckerCategory
     */
    public List<CheckerDetailEntity> findByToolNameInAndCheckerCategory(
            List<String> toolNameList,
            List<String> checkerCategoryList,
            List<String> checkerKeyList
    ) {
        if (CollectionUtils.isEmpty(toolNameList)
                && CollectionUtils.isEmpty(checkerCategoryList)) {
            return Lists.newArrayList();
        }

        Document fieldsObj = new Document();
        fieldsObj.put("checker_key", true);
        fieldsObj.put("checker_category", true);
        fieldsObj.put("publisher", true);
        fieldsObj.put("tool_name", true);
        Query query = new BasicQuery(new Document(), fieldsObj);

        if (CollectionUtils.isNotEmpty(toolNameList)) {
            query.addCriteria(Criteria.where("tool_name").in(toolNameList));
        }

        if (CollectionUtils.isNotEmpty(checkerCategoryList)) {
            query.addCriteria(Criteria.where("checker_category").in(checkerCategoryList));
        }

        if (CollectionUtils.isNotEmpty(checkerKeyList)) {
            query.addCriteria(Criteria.where("checker_key").in(checkerKeyList));
        }

        // 规则状态，0为打开
        query.addCriteria(Criteria.where("status").is(0));
        List<CheckerDetailEntity> checkerDetailEntityList =
                defectCoreMongoTemplate.find(query, CheckerDetailEntity.class);

        if (CollectionUtils.isEmpty(checkerCategoryList)) {
            return Lists.newArrayList();
        }

        return checkerDetailEntityList;
    }

    public List<String> distinctCheckerCategoryByToolNameInAndCheckerKeyIn(
            List<String> toolNameList,
            List<String> checkerKeyList
    ) {
        if (CollectionUtils.isEmpty(toolNameList)) {
            return Lists.newArrayList();
        }

        Query query = new Query(
                Criteria.where("tool_name").in(toolNameList)
                        .and("checker_key").in(checkerKeyList)
        );

        return defectCoreMongoTemplate.findDistinct(
                query,
                "checker_category",
                CheckerDetailEntity.class,
                String.class
        );
    }

    /**
     * 根据工具和 checker_key 获取工具类型
     * @param propVOS
     * @return
     */
    public List<CheckerDetailEntity> findByCheckerPropVO(List<CheckerPropVO> propVOS) {
        if (CollectionUtils.isEmpty(propVOS)) {
            return new ArrayList<>();
        }
        Map<String, List<CheckerPropVO>> toolToCheckerMap = propVOS.stream()
                .collect(Collectors.groupingBy(CheckerPropVO::getToolName));
        List<Criteria> criList = new ArrayList<>();

        for (Map.Entry<String, List<CheckerPropVO>> entry : toolToCheckerMap.entrySet()) {
            List<String> checkerKeys = entry.getValue().stream()
                    .map(CheckerPropVO::getCheckerKey).collect(Collectors.toList());
            criList.add(Criteria.where("tool_name").is(entry.getKey()).and("checker_key").in(checkerKeys));
        }
        Criteria cri = new Criteria();
        cri.orOperator(criList.toArray(new Criteria[0]));

        Query query = new Query();
        query.addCriteria(cri);
        return defectCoreMongoTemplate.find(query, CheckerDetailEntity.class);
    }
}
