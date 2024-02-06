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

package com.tencent.bk.codecc.defect.dao.core.mongorepository;

import com.tencent.bk.codecc.defect.model.admin.SmokeCheckDetailEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface SmokeCheckDetailRepository extends MongoRepository<SmokeCheckDetailEntity, String> {

    /**
     * 按归属id查询对应冒烟检查数据
     *
     * @param belongToId smokeCheckLog::entityId
     * @return list
     */
    List<SmokeCheckDetailEntity> findByBelongToId(String belongToId);

}
