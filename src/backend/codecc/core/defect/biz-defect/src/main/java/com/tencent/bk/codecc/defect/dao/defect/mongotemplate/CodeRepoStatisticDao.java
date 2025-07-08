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

package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.model.CodeRepoFromAnalyzeLogEntity;
import com.tencent.bk.codecc.defect.model.CodeRepoStatisticEntity;
import com.tencent.bk.codecc.defect.pojo.CodeRepoStatisticModel;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 代码仓库总表持久类
 *
 * @version V2.0
 * @date 2021/3/24
 */
@Repository
public class CodeRepoStatisticDao {

    private static final String CODE_REPO_STATISTIC = "t_code_repo_statistic";

    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 根据时间获取总代码仓库数量
     *
     * @param endTime 结束时间
     * @param createFrom 数据来源
     * @return long
     */
    public long getUrlCountByEndTimeAndCreateFrom(long endTime, String createFrom) {
        Criteria criteria = getCodeRepoStatTrendCriteria(endTime, createFrom, "url_first_scan");
        MatchOperation match = Aggregation.match(criteria);

        GroupOperation group = Aggregation.group("url");
        CountOperation count = Aggregation.count().as("url_count");

        Aggregation agg = Aggregation.newAggregation(match, group, count)
                .withOptions(new AggregationOptions.Builder().allowDiskUse(true).build());

        List<CodeRepoStatisticModel> countResult =
                defectMongoTemplate.aggregate(agg, CODE_REPO_STATISTIC, CodeRepoStatisticModel.class)
                        .getMappedResults();
        if (CollectionUtils.isNotEmpty(countResult)) {
            return countResult.get(0).getUrlCount();
        }
        return 0;
    }

    /**
     * 根据时间获取总代码分支数量
     *
     * @param endTime 结束时间
     * @param createFrom 数据来源
     * @return long
     */
    public long getBranchCount(long endTime, String createFrom) {
        Criteria criteria = getCodeRepoStatTrendCriteria(endTime, createFrom, "branch_first_scan");
        return defectMongoTemplate.count(new Query(criteria), CODE_REPO_STATISTIC);
    }

    /**
     * 获取总代码库/分支数量公共条件
     *
     * @param endTime 时间
     * @param createFrom 来源
     * @param firstScan 时间字段
     * @return criteria
     */
    @NotNull
    private Criteria getCodeRepoStatTrendCriteria(long endTime, String createFrom, String firstScan) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();
        // 时间
        if (endTime != 0) {
            criteriaList.add(Criteria.where(firstScan).lte(endTime));
        }
        // 来源
        if (StringUtils.isNotEmpty(createFrom)) {
            criteriaList.add(Criteria.where("data_from").is(createFrom));
        }
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }
        return criteria;
    }

    /**
     * 批量查询时间范围内首次分析的代码库信息
     *
     * @param taskIds 任务id集合
     * @param startDate 开始时间
     * @param endDate 截止时间
     * @return list
     */
    public List<CodeRepoFromAnalyzeLogEntity> findByTaskId(Collection<Long> taskIds, long startDate, long endDate) {

        MatchOperation matchIdx = Aggregation.match(Criteria.where("task_id").in(taskIds));

        UnwindOperation unwind = Aggregation.unwind("code_repo_list");

        MatchOperation matchAfter = Aggregation
                .match(Criteria.where("code_repo_list.createDate").gte(startDate).lt(endDate)
                        .and("code_repo_list.url").exists(true)
                        .and("code_repo_list.branch").exists(true));

        GroupOperation group = Aggregation.group("task_id")
                .first("task_id").as("task_id")
                .addToSet("code_repo_list").as("code_repo_list");

        AggregationOptions options = new AggregationOptions.Builder().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(matchIdx, unwind, matchAfter, group).withOptions(options);

        AggregationResults<CodeRepoFromAnalyzeLogEntity> results =
                defectMongoTemplate.aggregate(agg, "t_code_repo_from_analyzelog", CodeRepoFromAnalyzeLogEntity.class);
        return results.getMappedResults();
    }

    /**
     * 按url分组分页获取url
     */
    public List<String> distinctUrlByDataFrom(String dataFrom, Integer pageNum, Integer pageSize)  {
        int page = (pageNum == null || pageNum <= 0) ? 0 : pageNum - 1;
        int size = (pageSize == null || pageSize <= 0 || pageSize > ComConstants.COMMON_NUM_10000)
                ? ComConstants.SMALL_PAGE_SIZE : pageSize;

        MatchOperation match = Aggregation.match(Criteria.where("data_from").is(dataFrom));
        GroupOperation group = Aggregation.group("url").first("url").as("url");
        SortOperation sort = Aggregation.sort(Sort.Direction.ASC, "url");

        SkipOperation skip = Aggregation.skip((long) page * size);
        LimitOperation limit = Aggregation.limit(size);

        Aggregation agg = Aggregation.newAggregation(match, group, sort, skip, limit)
                .withOptions(new AggregationOptions.Builder().allowDiskUse(true).build());
        return defectMongoTemplate.aggregate(agg, CODE_REPO_STATISTIC, CodeRepoStatisticEntity.class)
                .getMappedResults().stream().map(CodeRepoStatisticEntity::getUrl).collect(Collectors.toList());
    }

    /**
     * 指定查询
     * @param createFrom 来源
     * @see com.tencent.devops.common.constant.ComConstants.DefectStatType
     * @return list
     */
    public List<CodeRepoStatisticEntity> findByCreateFromAndUrls(String createFrom, List<String> urls) {
        List<Criteria> criteriaList = Lists.newArrayList();

        if (StringUtils.isNotEmpty(createFrom)) {
            criteriaList.add(Criteria.where("data_from").is(createFrom));
        }

        if (CollectionUtils.isNotEmpty(urls)) {
            criteriaList.add(Criteria.where("url").in(urls));
        }

        Criteria criteria = new Criteria();
        if (CollectionUtils.isEmpty(criteriaList)) {
            return Collections.emptyList();
        }
        criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        Query query = new Query(criteria);
        return defectMongoTemplate.find(query, CodeRepoStatisticEntity.class, CODE_REPO_STATISTIC);
    }
}