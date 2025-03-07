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


package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.task.model.BaseDataEntity;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 基础数据表持久层
 *
 * @version V1.0
 * @date 2021/9/27
 */
@Repository
public class BaseDataDao {
    @Autowired
    private MongoTemplate mongoTemplate;

    public Page<BaseDataEntity> getDefaultFilterPathListByParamValue(String paramType, String paramValue,
                                                                     Pageable pageable) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();

        // 默认屏蔽路径类型
        if (StringUtils.isNotEmpty(paramType)) {
            criteriaList.add(Criteria.where("param_type").is(paramType));
        }

        // 搜索框模糊查询
        if (StringUtils.isNotEmpty(paramValue)) {
            List<Criteria> quickSearchCriteria = new ArrayList<>();
            quickSearchCriteria.add(Criteria.where("param_value").regex(paramValue));
            quickSearchCriteria.add(Criteria.where("created_by").regex(paramValue));
            criteriaList.add(new Criteria().orOperator(quickSearchCriteria.toArray(new Criteria[0])));

        }

        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        // 获取满足条件的总数
        long totalCount = mongoTemplate.count(new Query(criteria), "t_base_data");

        // 分页排序
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        SortOperation sort = Aggregation.sort(pageable.getSort());
        SkipOperation skip = Aggregation.skip(Long.valueOf(pageNumber * pageSize));
        LimitOperation limit = Aggregation.limit(pageSize);

        Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria), sort, skip, limit);
        AggregationResults<BaseDataEntity> queryResults =
                mongoTemplate.aggregate(aggregation, "t_base_data", BaseDataEntity.class);

        // 计算总页数
        int totalPageNum = 0;
        if (totalCount > 0) {
            totalPageNum = (Integer.parseInt(String.valueOf(totalCount)) + pageSize - 1) / pageSize;
        }

        // 页码加1返回
        return new Page<>(totalCount, pageNumber + 1, pageSize, totalPageNum, queryResults.getMappedResults());
    }

    /**
     * 按paramCode分页查询
     *
     * @param paramType type
     * @param paramCode 流水线id
     * @param pageable  page
     * @return page
     */
    public Page<BaseDataEntity> findPipelineTaskLimitPage(String paramType, String paramCode, Pageable pageable) {
        Criteria criteria = new Criteria();

        if (StringUtils.isNotEmpty(paramType)) {
            criteria.and("param_type").is(paramType);
        }

        // 流水线id支持匹配查询
        if (StringUtils.isNotBlank(paramCode)) {
            criteria.and("param_code").regex(paramCode);
        }

        long totalCount = mongoTemplate.count(new Query(criteria), BaseDataEntity.class);

        // 分页排序
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();

        SkipOperation skip = Aggregation.skip(Long.valueOf(pageNumber * pageSize));
        SortOperation sort = Aggregation.sort(pageable.getSort());
        LimitOperation limit = Aggregation.limit(pageSize);

        Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria), sort, skip, limit);
        AggregationResults<BaseDataEntity> results =
                mongoTemplate.aggregate(aggregation, "t_base_data", BaseDataEntity.class);

        return new Page<>(pageNumber + 1, pageSize, totalCount, results.getMappedResults());
    }

    /**
     * 按paramType分页查询
     *
     * @param paramType 类型
     * @param pageable  page
     * @return page
     */
    public Page<BaseDataEntity> findGitHubSyncPage(String paramType, Pageable pageable) {
        Criteria criteria = new Criteria();

        // 如果paramType为空，则将组和单仓库都加入到查询条件中
        if (StringUtils.isNotEmpty(paramType)) {
            criteria.and("param_type").is(paramType);
        } else {
            criteria.and("param_type").in(ComConstants.SYNC_GITHUB_CONFIG_GROUP_REPO,
                    ComConstants.SYNC_GITHUB_CONFIG_SINGLE_REPO);
        }

        long totalCount = mongoTemplate.count(new Query(criteria), BaseDataEntity.class);

        // 分页排序
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();

        SkipOperation skip = Aggregation.skip(Long.valueOf(pageNumber * pageSize));
        SortOperation sort = Aggregation.sort(pageable.getSort());
        LimitOperation limit = Aggregation.limit(pageSize);

        Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria), sort, skip, limit);
        AggregationResults<BaseDataEntity> results =
                mongoTemplate.aggregate(aggregation, "t_base_data", BaseDataEntity.class);

        return new Page<>(pageNumber + 1, pageSize, totalCount, results.getMappedResults());
    }

    /**
     * 配置工具/语言顺序
     *
     * @param paramType  区分工具/语言
     * @param paramValue 更新的值
     */
    public void editToolLangSort(String paramType, String paramValue) {
        Query query = new Query();
        query.addCriteria(Criteria.where("param_type").is(paramType));
        Update update = new Update();
        update.set("param_value", paramValue);
        mongoTemplate.upsert(query, update, BaseDataEntity.class);
    }

    /**
     * 根据参数类型，参数名和参数值更新数据
     */
    public void updateByParamTypeAndParamNameAndParamValue(String paramType, String paramName, String paramValue,
            long updatedDate, String updateBy) {
        Query query = new Query();
        query.addCriteria(Criteria.where("param_type").is(paramType)
                .and("param_name").is(paramName));

        Update update = new Update();
        update.set("param_type", paramType)
                .set("param_name", paramName)
                .set("param_value", paramValue)
                .set("updated_date", updatedDate)
                .set("updated_by", updateBy);
        mongoTemplate.upsert(query, update, BaseDataEntity.class);
    }
}
