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

package com.tencent.bk.codecc.defect.dao.defect;

import com.tencent.codecc.common.db.MongoPageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * MongoDB分页查询工具的配置类
 *
 * @version V1.0
 * @date 2020/6/26
 */
@Configuration
public class MongoPageHelperConfiguration {

    @Autowired
    private MongoTemplate defectMongoTemplate;

    @Bean
    public MongoPageHelper mongoPageHelper() {
        return new MongoPageHelper(defectMongoTemplate);
    }
}
