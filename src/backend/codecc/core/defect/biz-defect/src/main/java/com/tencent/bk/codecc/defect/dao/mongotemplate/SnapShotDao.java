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

package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.model.SnapShotEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 快照Dao
 *
 * @version V1.0
 * @date 2021/11/14
 */
@Repository
public class SnapShotDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 更新红线数据上传状态
     *
     * @param projectId
     * @param buildId
     * @param status
     */
    public void updateMetadataReportStatus(
            String projectId,
            Long taskId,
            String buildId,
            Boolean status,
            Long buildFlag
    ) {
        Criteria criteria = Criteria.where("project_id").is(projectId)
                .and("task_id").is(taskId)
                .and("build_id").is(buildId);

        if (buildFlag != null) {
            criteria.and("build_flag").is(buildFlag);
        }

        Query query = Query.query(criteria);
        Update update = Update.update("metadata_report", status);
        mongoTemplate.updateFirst(query, update, SnapShotEntity.class);
    }
}
