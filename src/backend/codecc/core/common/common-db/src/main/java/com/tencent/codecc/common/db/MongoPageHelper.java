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

package com.tencent.codecc.common.db;

import com.google.common.collect.Lists;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.CommonMessageCode;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.CollectionUtils;

/**
 * MongoDB分页查询工具类
 *
 * @version V1.0
 * @date 2020/6/26
 */
@Slf4j
public class MongoPageHelper
{
    public static final int FIRST_PAGE_NUM = 1;
    public static final String ID = "_id";
    private final MongoTemplate mongoTemplate;

    public MongoPageHelper(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * 分页查询，直接返回集合类型的结果.
     */
    public <T> Page<T> pageQuery(
            Query query, Class<T> entityClass, Integer pageSize, Integer pageNum,
            List<Order> sortList, Duration timeout
    ) {
        return pageQuery(query, entityClass, Function.identity(), pageSize, pageNum, sortList, timeout);
    }

    /**
     * 分页查询，不考虑条件分页，直接使用skip-limit来分页.
     */
    public <T, R> Page<R> pageQuery(
            Query query, Class<T> entityClass, Function<T, R> mapper, Integer pageSize,
            Integer pageNum, List<Order> sortList, Duration timeout
    ) {
        return pageQuery(query, entityClass, mapper, pageSize, pageNum, sortList, null, timeout);
    }

    /**
     * 分页查询.
     *
     * @param query Mongo Query对象，构造你自己的查询条件.
     * @param entityClass Mongo collection定义的entity class，用来确定查询哪个集合.
     * @param mapper 映射器，你从db查出来的list的元素类型是entityClass, 如果你想要转换成另一个对象，比如去掉敏感字段等，可以使用mapper来决定如何转换.
     * @param pageSize 分页的大小.
     * @param pageNum 当前页.
     * @param lastId 条件分页参数, 区别于skip-limit，采用find(_id>lastId).limit分页.
     *         如果不跳页，像朋友圈，微博这样下拉刷新的分页需求，需要传递上一页的最后一条记录的ObjectId。 如果是null，则返回pageNum那一页.
     * @param <T> collection定义的class类型.
     * @param <R> 最终返回时，展现给页面时的一条记录的类型。
     * @return Page       一个封装page信息的对象.
     */
    public <T, R> Page<R> pageQuery(
            Query query,
            Class<T> entityClass,
            Function<T, R> mapper,
            Integer pageSize,
            Integer pageNum,
            List<Order> sortList,
            String lastId,
            Duration timeout
    ) {
        if (pageNum <= 0) {
            pageNum = FIRST_PAGE_NUM;
        }

        Query queryForFindList = Query.of(query);
        Criteria criteria = new Criteria();

        if (timeout != null) {
            queryForFindList.maxTime(timeout);
        }

        if (StringUtils.isNotBlank(lastId)) {
            if (pageNum != FIRST_PAGE_NUM) {
                criteria.and(ID).gt(new ObjectId(lastId));
            }
            queryForFindList.limit(pageSize);
        } else {
            int skip = pageSize * (pageNum - 1);
            queryForFindList.skip(skip).limit(pageSize);
        }

        long beginTime = System.currentTimeMillis();
        List<T> entityList =
                mongoTemplate.find(queryForFindList.addCriteria(criteria).with(Sort.by(sortList)), entityClass);
        log.info("page query get record cost: {}", System.currentTimeMillis() - beginTime);

        if (CollectionUtils.isEmpty(entityList)) {
            return new Page<>(0L, pageNum, pageSize, 0, Lists.newArrayList());
        }

        if (timeout != null) {
            // 计算剩余可用的超时时间
            long availableTimeout = (beginTime + timeout.toMillis()) - System.currentTimeMillis();
            if (availableTimeout <= 0) {
                throw new CodeCCException(CommonMessageCode.DB_QUERY_TIME_OUT, "mongodb operation timeout");
            }

            // 实际上该超时并不能作用于count，SDK导致
            query.maxTime(Duration.ofMillis(availableTimeout));
        }

        beginTime = System.currentTimeMillis();
        long total = mongoTemplate.count(query, entityClass);
        log.info("page query get total cost: {}", System.currentTimeMillis() - beginTime);
        int pages = (int) Math.ceil(total / (double) pageSize);

        final Page<R> pageResult = new Page<>(
                total, pageNum, pageSize, pages,
                entityList.stream().map(mapper).collect(Collectors.toList())
        );

        return pageResult;
    }
}
