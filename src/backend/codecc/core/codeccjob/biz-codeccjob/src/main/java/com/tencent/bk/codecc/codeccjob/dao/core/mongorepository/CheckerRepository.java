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

package com.tencent.bk.codecc.codeccjob.dao.core.mongorepository;

import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * 规则持久层代码
 *
 * @version V1.0
 * @date 2019/4/26
 */
@Repository
public interface CheckerRepository extends MongoRepository<CheckerDetailEntity, String> {

    /**
     * 通过工具名查询规则详细信息
     *
     * @param toolName 工具名
     * @return list
     */
    @Query(fields = "{'checker_key': 1, 'create_date': 1}")
    List<CheckerDetailEntity> findByToolName(String toolName);

}
