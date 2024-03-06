package com.tencent.bk.codecc.defect.dao.core.mongotemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.pojo.CheckerMaxVersionAggModel;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetSource;
import com.tencent.devops.common.constant.CheckerConstants;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 规则集数据DAO
 *
 * @version V4.0
 * @date 2019/11/2
 */
@Slf4j
@Repository
public class CheckerSetDao {

    @Autowired
    private MongoTemplate defectCoreMongoTemplate;

    /**
     * 更新指定规则集所有版本数据
     *
     * @param checkerSetEntity
     */
    public void updateAllVersionCheckerSet(CheckerSetEntity checkerSetEntity) {
        Query query = new Query();
        query.addCriteria(Criteria.where("checker_set_id").is(checkerSetEntity.getCheckerSetId()));

        Update update = new Update();
        update.set("checker_set_name", checkerSetEntity.getCheckerSetName())
                .set("scope", checkerSetEntity.getScope()).set("last_update_time",
                        checkerSetEntity.getLastUpdateTime());
        defectCoreMongoTemplate.updateMulti(query, update, CheckerSetEntity.class);
    }


    /**
     * 通过条件查询所有规则集
     *
     * @param keyWord
     * @param codeLang
     * @param checkerSetCategory
     * @param tool
     * @return
     */
    public List<CheckerSetEntity> findByComplexCheckerSetCondition(String keyWord, Set<String> checkerSetIds,
            Set<String> codeLang,
            Set<CheckerSetCategory> checkerSetCategory,
            Set<String> tool,
            Set<CheckerSetSource> checkerSetSource,
            String creator,
            Boolean officialFlag,
            boolean needCheckerSetFilter) {
        Query query = new Query();
        Criteria firstCriteria = new Criteria();
        List<Criteria> andCriteriaList = new ArrayList<>();
        List<Criteria> finalCriteriaList = new ArrayList<>();
        if (needCheckerSetFilter) {
            if (CollectionUtils.isNotEmpty(checkerSetIds)) {
                andCriteriaList.add(Criteria.where("checker_set_id").in(checkerSetIds));
            } else {
                andCriteriaList.add(Criteria.where("checker_set_id").in(Collections.emptyList()));
            }
        }
        if (StringUtils.isNotBlank(keyWord)) {
            andCriteriaList.add(Criteria.where("checker_set_name").regex(keyWord));
        }
        if (CollectionUtils.isNotEmpty(codeLang)) {
            List<Criteria> langCriteria = new ArrayList<>();
            List<Criteria> langOrCriteria = new ArrayList<>();
            for (String lang : codeLang) {
                langOrCriteria.add(Criteria.where("checker_set_lang").regex(lang));
            }
            langCriteria.add(new Criteria().orOperator(langOrCriteria.toArray(new Criteria[0])));

            andCriteriaList.addAll(langCriteria);
        }
        if (CollectionUtils.isNotEmpty(checkerSetCategory)) {
            andCriteriaList.add(
                    Criteria.where("catagories").elemMatch(Criteria.where("en_name").in(checkerSetCategory)));
        }
        if (CollectionUtils.isNotEmpty(tool)) {
            andCriteriaList.add(Criteria.where("checker_props").elemMatch(Criteria.where("tool_name").in(tool)));
        }
        if (StringUtils.isNotBlank(creator)) {
            andCriteriaList.add(Criteria.where("creator").is(creator));
        }
        if (CollectionUtils.isNotEmpty(checkerSetSource)) {
            List<Criteria> sourceCriteria = new ArrayList<>();
            sourceCriteria.add(Criteria.where("checker_set_source").in(checkerSetSource));
            if (checkerSetSource.contains(CheckerSetSource.SELF_DEFINED)) {
                sourceCriteria.add(Criteria.where("checker_set_source").exists(false));
                sourceCriteria.add(Criteria.where("checker_set_source").is(null));
            }
            andCriteriaList.add(new Criteria().orOperator(sourceCriteria.toArray(new Criteria[0])));
        }

        if (CollectionUtils.isNotEmpty(andCriteriaList)) {
            firstCriteria.andOperator(andCriteriaList.toArray(new Criteria[0]));
            finalCriteriaList.add(firstCriteria);
        }

        Criteria secondCriteria = new Criteria();
        if (null != officialFlag && officialFlag) {
            List<Criteria> secondAndCriteriaList = new ArrayList<>();
            if (StringUtils.isNotBlank(keyWord)) {
                secondAndCriteriaList.add(Criteria.where("checker_set_name").regex(keyWord));
            }
            List<Criteria> langOrCriteria = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(codeLang)) {
                for (String lang : codeLang) {
                    langOrCriteria.add(Criteria.where("checker_set_lang").regex(lang));
                }
                secondAndCriteriaList.add(new Criteria().orOperator(langOrCriteria.toArray(new Criteria[0])));
            }
            if (CollectionUtils.isNotEmpty(checkerSetCategory)) {
                secondAndCriteriaList.add(
                        Criteria.where("catagories").elemMatch(Criteria.where("en_name").in(checkerSetCategory)));
            }
            if (CollectionUtils.isNotEmpty(tool)) {
                secondAndCriteriaList.add(
                        Criteria.where("checker_props").elemMatch(Criteria.where("tool_name").in(tool)));
            }
            if (StringUtils.isNotBlank(creator)) {
                secondAndCriteriaList.add(Criteria.where("creator").is(creator));
            }

            List<Criteria> finalSourceCriteria = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(checkerSetSource)) {
                List<Criteria> sourceCriteria = new ArrayList<>();

                sourceCriteria.add(Criteria.where("checker_set_source").in(checkerSetSource));
                if (checkerSetSource.contains(CheckerSetSource.SELF_DEFINED)) {
                    sourceCriteria.add(Criteria.where("checker_set_source").exists(false));
                    sourceCriteria.add(Criteria.where("checker_set_source").is(null));
                }
                finalSourceCriteria.add(new Criteria().orOperator(sourceCriteria.toArray(new Criteria[0])));
            }

            Criteria secondSourceCriteria =
                    Criteria.where("checker_set_source").in(Arrays.asList(CheckerSetSource.DEFAULT,
                            CheckerSetSource.RECOMMEND));
            finalSourceCriteria.add(secondSourceCriteria);
            secondAndCriteriaList.add(new Criteria().andOperator(finalSourceCriteria.toArray(new Criteria[0])));

            if (CollectionUtils.isNotEmpty(secondAndCriteriaList)) {
                secondCriteria.andOperator(secondAndCriteriaList.toArray(new Criteria[0]));
                finalCriteriaList.add(secondCriteria);
            }
        }

        if (CollectionUtils.isNotEmpty(finalCriteriaList)) {
            query.addCriteria(new Criteria().orOperator(finalCriteriaList.toArray(new Criteria[0])));
        }

        return defectCoreMongoTemplate.find(query, CheckerSetEntity.class);
    }

    /**
     * updateCheckerSetUsage
     *
     * @param statisticsMap
     */
    public void updateCheckerSetUsage(Map<String, Long> statisticsMap) {
        if (statisticsMap == null || statisticsMap.size() == 0) {
            return;
        }

        BulkOperations ops = defectCoreMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CheckerSetEntity.class);
        int batchSize = 20000;
        int counter = 0;

        for (Entry<String, Long> entry : statisticsMap.entrySet()) {
            counter++;

            String checkerSetId = entry.getKey();
            Long usageCount = entry.getValue();
            Query query = Query.query(Criteria.where("checker_set_id").is(checkerSetId));
            Update update = Update.update("task_usage", usageCount);
            ops.updateMulti(query, update);

            if (counter % batchSize == 0 || counter == statisticsMap.size()) {
                ops.execute();
                ops = defectCoreMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CheckerSetEntity.class);
            }
        }
    }

    /**
     * 通过条件查询更多规则集
     *
     * @param quickSearch
     * @param codeLang
     * @param checkerSetCategory
     * @param pageable
     * @return
     */
    public List<CheckerSetEntity> findMoreByCondition(String quickSearch, Set<String> codeLang,
            Set<CheckerSetCategory> checkerSetCategory,
            Set<String> projectCheckerSetIds, Boolean projectInstalled,
            Map<String, Integer> toolGrayMap, Pageable pageable) {
        // 查询条件
        List<Criteria> criteriaList = new ArrayList<>();
        if (StringUtils.isNotBlank(quickSearch)) {
            criteriaList.add(
                    new Criteria().orOperator(
                            Criteria.where("checker_set_name").regex(quickSearch, "i"),
                            Criteria.where("creator").regex(quickSearch, "i"),
                            Criteria.where("description").regex(quickSearch, "i")
                    )
            );
        }
        if (CollectionUtils.isNotEmpty(codeLang)) {
            List<Criteria> langCriteria = new ArrayList<>();
            langCriteria.add(Criteria.where("checker_set_lang").in(codeLang));
            Criteria langArrayCriteria = Criteria.where("legacy").is(true);
            List<Criteria> langOrCriteria = new ArrayList<>();
            for (String lang : codeLang) {
                langOrCriteria.add(Criteria.where("checker_set_lang").regex(lang));
            }
            langCriteria.add(new Criteria().andOperator(langArrayCriteria,
                    new Criteria().orOperator(langOrCriteria.toArray(new Criteria[0]))));
            criteriaList.add(new Criteria().orOperator(langCriteria.toArray(new Criteria[0])));
        }
        if (CollectionUtils.isNotEmpty(checkerSetCategory)) {
            List<String> categorylist =
                    checkerSetCategory.stream().map(category -> category.name()).collect(Collectors.toList());
            criteriaList.add(Criteria.where("catagories").elemMatch(Criteria.where("en_name").in(categorylist)));
        }

        // 按照项目是否安装过滤
        if (projectInstalled != null) {
            if (projectInstalled) {
                criteriaList.add(
                        new Criteria().orOperator(
                                Criteria.where("checker_set_source").in(Lists.newArrayList(
                                        CheckerSetSource.RECOMMEND.name(), CheckerSetSource.DEFAULT.name())),
                                Criteria.where("checker_set_id").in(projectCheckerSetIds)
                        )
                );
            } else {
                criteriaList.add(Criteria.where("checker_set_source").nin(Lists.newArrayList(
                        CheckerSetSource.RECOMMEND.name(), CheckerSetSource.DEFAULT.name())));
                criteriaList.add(Criteria.where("checker_set_id").nin(projectCheckerSetIds));
            }
        } else {
            // 只保留未下架的公开的规则集
            criteriaList.add(Criteria.where("scope").is(CheckerConstants.CheckerSetScope.PUBLIC.code()));
            //            criteriaList.add(Criteria.where("enable").is(CheckerConstants.CheckerSetEnable.ENABLE.code
            //            ()));
        }

        // 只查询灰度配置指定集成状态的和 >=0 的规则集
        List<Criteria> secondCriteria =
                Lists.newArrayList(Criteria.where("version").gte(ToolIntegratedStatus.P.value()));
        if (MapUtils.isNotEmpty(toolGrayMap)) {
            toolGrayMap.forEach((tool, status) -> {
                // 只添加灰度状态为非生产的筛选条件
                if (status != null && status < ToolIntegratedStatus.P.value()) {
                    secondCriteria.add(Criteria.where("tool_name").is(tool).and("version").is(status));
                }
            });
        }
        criteriaList.add(new Criteria().orOperator(secondCriteria.toArray(new Criteria[0])));

        List<AggregationOperation> aggOptions = new ArrayList<>();

        MatchOperation match = Aggregation.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        aggOptions.add(match);

        //添加版本号排序
        SortOperation versionSort = Aggregation.sort(Sort.Direction.ASC, "version");
        aggOptions.add(versionSort);

        //分组去重，以checker_set_id进行分组
        GroupOperation group = Aggregation.group("checker_set_id")
                .last("checker_set_id").as("checker_set_id")
                .last("version").as("version")
                .last("checker_set_name").as("checker_set_name")
                .last("checker_set_lang").as("checker_set_lang")
                .last("code_lang").as("code_lang")
                .last("creator").as("creator")
                .last("create_time").as("create_time")
                .last("checker_props").as("checker_props")
                .last("task_usage").as("task_usage")
                .last("description").as("description")
                .last("catagories").as("catagories")
                .last("official").as("official")
                .last("legacy").as("legacy")
                .last("scope").as("scope")
                .last("enable").as("enable")
                .last("checker_set_source").as("checker_set_source")
                // 借用base_checker_set_id临时存放主键Id，以正确使用I18N
                .last("_id").as("base_checker_set_id");
        aggOptions.add(group);

        // 排序分页
        if (pageable != null) {
            SortOperation sort = Aggregation.sort(Sort.Direction.ASC, "legacy").and(pageable.getSort());
            aggOptions.add(sort);

            SkipOperation skip = Aggregation.skip((long) (pageable.getPageNumber() * pageable.getPageSize()));
            aggOptions.add(skip);

            LimitOperation limit = Aggregation.limit(pageable.getPageSize());
            aggOptions.add(limit);
        }
        Aggregation agg = Aggregation.newAggregation(aggOptions);
        AggregationResults<CheckerSetEntity> queryResult = defectCoreMongoTemplate.aggregate(agg, "t_checker_set",
                CheckerSetEntity.class);

        List<CheckerSetEntity> retList = queryResult.getMappedResults();

        if (CollectionUtils.isNotEmpty(retList)) {
            for (CheckerSetEntity entity : retList) {
                entity.setEntityId(entity.getBaseCheckerSetId());
                entity.setBaseCheckerSetId(null);
            }
        }

        return retList;
    }

    public List<CheckerSetEntity> findByCheckerSetIdInAndVersionIn(
            List<String> checkerSetIdList,
            List<ComConstants.ToolIntegratedStatus> versionList) {
        Query query = new Query();
        query.addCriteria(Criteria.where("checker_set_id").in(checkerSetIdList)
                .and("tool_integrated_status").in(versionList));
        return defectCoreMongoTemplate.find(query, CheckerSetEntity.class);
    }

    /**
     * 查询规则列表通过规则id和指定版本
     *
     * @param checkerIdAndVersionMap 规则id和指定版本列表
     * @return
     */
    public List<CheckerSetEntity> queryCheckerDetailForPreCI(Map<String, Integer> checkerIdAndVersionMap) {
        if (MapUtils.isEmpty(checkerIdAndVersionMap)) {
            return Lists.newArrayList();
        }
        Query query = new Query();
        Criteria criteria = new Criteria();
        List<Criteria> orCriteriaList = Lists.newArrayList();

        for (Entry<String, Integer> kv : checkerIdAndVersionMap.entrySet()) {
            orCriteriaList.add(Criteria.where("checker_set_id").is(kv.getKey())
                    .and("version").is(kv.getValue()));
        }

        if (CollectionUtils.isNotEmpty(orCriteriaList)) {
            criteria.orOperator(orCriteriaList.toArray(new Criteria[0]));
        }
        query.addCriteria(criteria);

        return defectCoreMongoTemplate.find(query, CheckerSetEntity.class, "t_checker_set");
    }


    /**
     * 根据规则集id查询规则集名称
     *
     * @param checkerSetId 规则集id
     * @return checkerSetEntity
     */
    public CheckerSetEntity queryCheckerSetNameByCheckerSetId(String checkerSetId) {
        CheckerSetEntity checkerSetEntity = defectCoreMongoTemplate
                .findOne(new Query(Criteria.where("checker_set_id").is(checkerSetId)), CheckerSetEntity.class,
                        "t_checker_set");
        return checkerSetEntity;
    }

    /**
     * 获取规则集最大版本关系
     *
     * @param checkIdSet
     * @return
     */
    public Map<String, Integer> queryCheckerMaxVersion(Set<String> checkIdSet) {
        if (CollectionUtils.isEmpty(checkIdSet)) {
            return Maps.newHashMap();
        }
        Criteria criteria = Criteria.where("checker_set_id").in(checkIdSet);
        MatchOperation match = Aggregation.match(criteria);
        GroupOperation group = Aggregation.group("checker_set_id").max("version").as("maxVersion");
        AggregationResults<CheckerMaxVersionAggModel> retAgg = defectCoreMongoTemplate.aggregate(
                Aggregation.newAggregation(match, group),
                "t_checker_set",
                CheckerMaxVersionAggModel.class
        );

        if (retAgg != null && CollectionUtils.isNotEmpty(retAgg.getMappedResults())) {
            return retAgg.getMappedResults().stream().collect(Collectors.toMap(
                    CheckerMaxVersionAggModel::getCheckerSetId,
                    CheckerMaxVersionAggModel::getMaxVersion, (k1, k2) -> k2)
            );
        }

        return Maps.newHashMap();
    }

    /**
     * 批量更新toolVersions字段
     * @param needUpdateMap 规则集ID, toolName
     */
    public Long updateToolVersions(Map<String, String> needUpdateMap) {
        if (MapUtils.isEmpty(needUpdateMap)) {
            log.warn("updateToolVersions needUpdateMap is empty!");
            return ComConstants.COMMON_NUM_0L;
        }

        BulkOperations ops = defectCoreMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CheckerSetEntity.class);
        needUpdateMap.forEach((key, toolName) -> {
            String[] strArr = key.split(ComConstants.SEMICOLON);
            String checkerSetId = strArr[0];
            int version = Integer.parseInt(strArr[1]);
            Query query = new Query();
            query.addCriteria(Criteria.where("checker_set_id").is(checkerSetId).and("version").is(version));

            Update update = new Update();
            update.set("tool_name", toolName);
            ops.updateOne(query, update);
        });

        int modifiedCount = ops.execute().getModifiedCount();
        return (long) modifiedCount;
    }
}
