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

import com.tencent.bk.codecc.defect.model.ignore.IgnoreCommentDefectModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 注释忽略持久类
 * 
 * @date 2021/6/30
 * @version V1.0
 */
@Repository
public class IgnoreDefectDao {
    @Autowired
    private MongoTemplate defectMongoTemplate;

    public IgnoreCommentDefectModel findFirstUnmigratedByTaskId(Long taskId) {
        Criteria criteria = Criteria.where("task_id").is(taskId).and("migrated").ne(true);
        Query query = new Query(criteria);

        return defectMongoTemplate.findOne(query, IgnoreCommentDefectModel.class);
    }

    public void updateMigratedByTaskId(Long taskId) {
        Criteria criteria = Criteria.where("task_id").is(taskId).and("migrated").ne(true);
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("migrated", true);

        defectMongoTemplate.updateFirst(query, update, IgnoreCommentDefectModel.class);
    }
}
