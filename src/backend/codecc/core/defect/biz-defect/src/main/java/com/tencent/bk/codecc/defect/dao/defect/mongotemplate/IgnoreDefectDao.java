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

import com.tencent.bk.codecc.defect.model.ignore.IgnoreCommentDefectModel;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.HashMap;

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

    /**
     * 更新忽略告警信息
     * @param ignoreCommentDefectModel
     */
    public void upsertIgnoreDefectInfo(IgnoreCommentDefectModel ignoreCommentDefectModel) {
        if (MapUtils.isNotEmpty(ignoreCommentDefectModel.getIgnoreDefectMap())) {
            BulkOperations ops = defectMongoTemplate
                    .bulkOps(BulkOperations.BulkMode.UNORDERED, IgnoreCommentDefectModel.class);
            ignoreCommentDefectModel.getIgnoreDefectMap().forEach((k, v) -> {
                    Query query = new Query();
                    query.addCriteria(Criteria.where("task_id").is(ignoreCommentDefectModel.getTaskId()));
                    Update update = new Update();
                    update.set("task_id", ignoreCommentDefectModel.getTaskId());
                    if (null == v) {
                        update.unset(String.format("ignore_defect_map.%s", k));
                    } else {
                        update.set(String.format("ignore_defect_map.%s", k), v);
                    }
                    ops.upsert(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 删除忽略告警映射字段
     * @param taskId
     */
    public void deleteIgnoreDefectMap(Long taskId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId));
        Update update = new Update();
        update.set("task_id", taskId);
        update.set("ignore_defect_map", new HashMap<>());
        defectMongoTemplate.upsert(query, update, IgnoreCommentDefectModel.class);
    }
}
