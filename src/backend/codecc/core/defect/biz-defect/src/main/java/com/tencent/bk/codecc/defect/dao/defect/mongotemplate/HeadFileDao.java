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

import com.tencent.bk.codecc.defect.model.HeadFileEntity;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.HashSet;

/**
 * oc头文件识别持久
 * 
 * @date 2021/12/31
 * @version V1.0
 */
@Repository
public class HeadFileDao {
    @Autowired
    private MongoTemplate defectMongoTemplate;

    /**
     * 更新头文件路径信息
     * @param headFileEntity
     */
    public void addHeadFileInfo(HeadFileEntity headFileEntity) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(headFileEntity.getTaskId()));
        Update update = new Update();
        update.set("task_id", headFileEntity.getTaskId());
        if (CollectionUtils.isNotEmpty(headFileEntity.getHeadFileSet())) {
            update.addToSet("head_file_set").each(headFileEntity.getHeadFileSet().toArray());
        }
        defectMongoTemplate.upsert(query, update, HeadFileEntity.class);
    }

    /**
     * 删除头文件路径信息
     * @param headFileEntity
     */
    public void deleteHeadFileInfo(HeadFileEntity headFileEntity) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(headFileEntity.getTaskId()));
        Update update = new Update();
        update.set("task_id", headFileEntity.getTaskId());
        if (CollectionUtils.isNotEmpty(headFileEntity.getHeadFileSet())) {
            update.pullAll("head_file_set", headFileEntity.getHeadFileSet().toArray());
        }
        defectMongoTemplate.updateMulti(query, update, HeadFileEntity.class);
    }

    /**
     * 删除头文件路径信息
     * @param taskId
     */
    public void deleteHeadFileInfo(Long taskId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("task_id").is(taskId));
        Update update = new Update();
        update.set("task_id", taskId);
        update.set("head_file_set", new HashSet<>());
        defectMongoTemplate.upsert(query, update, HeadFileEntity.class);
    }
}
