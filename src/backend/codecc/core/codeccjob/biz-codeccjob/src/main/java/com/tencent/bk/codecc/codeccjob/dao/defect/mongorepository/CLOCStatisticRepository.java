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

package com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.statistic.CLOCStatisticEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * cloc统计持久
 *
 * @version V1.0
 * @date 2020/4/9
 */
@Repository
public interface CLOCStatisticRepository extends MongoRepository<CLOCStatisticEntity, String> {

    /**
     * 根据 task_id 和 build_id 查询单个记录
     *
     * @param taskId 任务ID
     * @param buildId 构建ID
     */
    List<CLOCStatisticEntity> findByTaskIdAndToolNameAndBuildId(Long taskId, String toolName, String buildId);

    void deleteAllByTaskIdAndToolNameInAndBuildIdIn(long taskId, List<String> toolNames, List<String> buildIds);

    long deleteByTaskId(long taskId);

    CLOCStatisticEntity findFirstByTaskIdAndBuildId(long taskId, String buildId);

    long deleteByTaskIdAndBuildIdIsNot(long taskId, String buildId);
}
