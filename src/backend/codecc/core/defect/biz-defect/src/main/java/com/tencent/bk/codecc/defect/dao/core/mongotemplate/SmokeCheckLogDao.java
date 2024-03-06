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

package com.tencent.bk.codecc.defect.dao.core.mongotemplate;

import com.tencent.bk.codecc.defect.model.admin.SmokeCheckLogEntity;
import com.tencent.devops.common.api.pojo.Page;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * 冒烟检查记录Dao
 *
 * @version V1.0
 * @date 2021/6/1
 */

@Repository
public class SmokeCheckLogDao {

    @Autowired
    private MongoTemplate defectCoreMongoTemplate;


    /**
     * 分页查询冒烟检查记录
     *
     * @param toolName 工具
     * @param pageable 分页
     * @return page
     */
    public Page<SmokeCheckLogEntity> findPage(String toolName, Pageable pageable) {
        Query query = new Query();
        if (StringUtils.isNotBlank(toolName)) {
            query.addCriteria(Criteria.where("tool_name").is(toolName));
        }
        long totalCount = defectCoreMongoTemplate.count(query, "t_smoke_log");

        query.with(pageable);
        List<SmokeCheckLogEntity> entities = defectCoreMongoTemplate.find(query, SmokeCheckLogEntity.class);

        return new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize(), totalCount, entities);
    }
}
