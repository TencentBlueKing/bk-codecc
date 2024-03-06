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

import com.tencent.bk.codecc.defect.model.SnapShotEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
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
    private MongoTemplate defectMongoTemplate;

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
        Criteria baseCriteria = Criteria.where("project_id").is(projectId)
                .and("build_id").is(buildId)
                .and("task_id").is(taskId);

        Query query;
        if (buildFlag != null) {
            query = Query.query(baseCriteria.and("build_flag").is(buildFlag));
        } else {
            query = Query.query(baseCriteria)
                    .with(Sort.by(Direction.DESC, "build_flag"))
                    .limit(1);
        }

        Update update = Update.update("metadata_report", status);
        // updateFirst does not support sort
        defectMongoTemplate.findAndModify(query, update, SnapShotEntity.class);
    }
}
