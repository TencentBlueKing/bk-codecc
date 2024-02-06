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

package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.model.CodeRepoStatDailyEntity;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 代码库/分支每日统计表持久类
 *
 * @version V2.0
 * @date 2021/4/15
 */
@Repository
public class CodeRepoStatDailyDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 根据时间和来源获取代码库数/分支数
     *
     * @param date 时间
     * @param createFrom 来源
     * @return entity
     */
    public CodeRepoStatDailyEntity getUrlCountByYesterdayDate(String date, String createFrom) {
        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();
        // 时间
        if (StringUtils.isNotEmpty(date)) {
            criteriaList.add(Criteria.where("date").is(date));
        }
        // 来源
        if (StringUtils.isNotEmpty(createFrom)) {
            criteriaList.add(Criteria.where("data_from").is(createFrom));
        }
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }
        return defectMongoTemplate.findOne(new Query(criteria), CodeRepoStatDailyEntity.class,
                "t_code_repo_stat_daily");
    }
}