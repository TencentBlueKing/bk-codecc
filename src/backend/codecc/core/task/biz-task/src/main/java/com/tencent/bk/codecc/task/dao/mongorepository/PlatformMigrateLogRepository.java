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

package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.model.PlatformMigrateLogEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * platform迁移日志持久层
 *
 * @version V1.0
 * @date 2021/7/15
 */

@Repository
public interface PlatformMigrateLogRepository extends MongoRepository<PlatformMigrateLogEntity, String> {

    /**
     * 根据实体id查询迁移日志
     *
     * @param entityId id
     * @return entity
     */
    PlatformMigrateLogEntity findFirstByEntityId(String entityId);
}
