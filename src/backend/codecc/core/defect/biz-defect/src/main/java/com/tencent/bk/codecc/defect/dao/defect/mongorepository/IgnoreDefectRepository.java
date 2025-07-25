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

package com.tencent.bk.codecc.defect.dao.defect.mongorepository;

import com.tencent.bk.codecc.defect.model.ignore.IgnoreCommentDefectModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * 注释忽略告警信息查询
 * 
 * @date 2021/6/30
 * @version V1.0
 */
@Repository
public interface IgnoreDefectRepository extends MongoRepository<IgnoreCommentDefectModel, String>
{
    /**
     * 通过任务id寻找忽略告警信息
     * @param taskId
     * @return
     */
    IgnoreCommentDefectModel findFirstByTaskId(Long taskId);
}
