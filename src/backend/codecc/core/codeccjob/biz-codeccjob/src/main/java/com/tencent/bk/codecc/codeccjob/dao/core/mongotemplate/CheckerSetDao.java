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

package com.tencent.bk.codecc.codeccjob.dao.core.mongotemplate;

import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import java.util.Collection;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * 规则集数据DAO
 *
 * @version V4.0
 * @date 2019/11/2
 */
@Repository
public class CheckerSetDao {

    @Autowired
    private MongoTemplate defectCoreMongoTemplate;


    /**
     * 查询带有指定规则的规则集
     *
     * @param checkers 规则集
     * @return list
     */
    public List<CheckerSetEntity> findByCheckerNameList(Collection<String> checkers) {
        Query query = new Query();

        if (CollectionUtils.isNotEmpty(checkers)) {
            query.addCriteria(Criteria.where("checker_props").elemMatch(Criteria.where("checker_key").in(checkers)));
        }
        query.addCriteria(Criteria.where("task_usage").gt(0).and("enable").is(1));

        return defectCoreMongoTemplate.find(query, CheckerSetEntity.class);
    }

}
