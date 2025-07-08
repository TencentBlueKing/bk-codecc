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

import com.tencent.bk.codecc.defect.model.SnapShotEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    /**
     * 查询最新的快照信息，如果重试扫描任务可能存在多个快照
     * @param projectId
     * @param buildId
     * @param taskId
     * @return
     */
    public Optional<SnapShotEntity> getLatestSnapShot(String projectId, String buildId, long taskId) {
        Criteria criteria = Criteria.where("project_id").is(projectId)
                .and("build_id").is(buildId)
                .and("task_id").is(taskId);

        // build_flag在上报时使用构建时间戳作为其值，因此可使用该字段区分数据的新旧
        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Direction.DESC, "build_flag"));

        return Optional.ofNullable(defectMongoTemplate.findOne(query, SnapShotEntity.class));
    }
}
