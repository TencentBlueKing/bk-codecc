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

package com.tencent.bk.codecc.task.dao.mongorepository;

import com.tencent.bk.codecc.task.model.NewTaskRetryRecordEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 新增开源扫描项目需要重试持久接口
 * 
 * @date 2020/7/15
 * @version V1.0
 */
@Repository
public interface NewTaskRetryRecordRepository extends MongoRepository<NewTaskRetryRecordEntity, String>
{
    /**
     * 是否需要重试
     * @param uploadTime
     * @param retryFlag
     * @return
     */
    List<NewTaskRetryRecordEntity> findByUploadTimeGreaterThanAndRetryFlagIsOrderByUploadTimeAsc(Long uploadTime, Boolean retryFlag);

}
