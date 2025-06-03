/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.tencent.bk.codecc.task.model.ToolMetaEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * 工具元数据持久层
 *
 * @version V1.0
 * @date 2019/6/13
 */
@Repository
public class ToolMetaDao
{
    @Autowired
    private MongoTemplate mongoTemplate;

    public Boolean updateToolMeta(ToolMetaEntity toolMetaEntity)
    {
        Query query = new Query(Criteria.where("name").is(toolMetaEntity.getName()));
        Update update = new Update();
        update.set("params", toolMetaEntity.getParams());
        update.set("updated_date", System.currentTimeMillis());
        return mongoTemplate.updateMulti(query, update, ToolMetaEntity.class).getModifiedCount() > 0;
    }

    public Boolean updateVisibleRange(String toolName, Set<String> visibleProjects, Set<String> visibleOrgIds) {
        Query query = new Query(Criteria.where("name").is(toolName));
        Update update = new Update();
        update.set("visible_projects", visibleProjects);
        update.set("visible_org_ids", visibleOrgIds);
        update.set("updated_date", System.currentTimeMillis());

        mongoTemplate.updateFirst(query, update, ToolMetaEntity.class);

        return true;
    }

    public ToolMetaEntity findBasicInfoByToolName(String toolName) {
        Query query = new Query(Criteria.where("name").is(toolName));
        query.fields().include("name", "display_name", "lang", "description", "brief_introduction", "type");

        return mongoTemplate.findOne(query, ToolMetaEntity.class);
    }

    public ToolMetaEntity findToolOptionsByToolName(String toolName) {
        Query query = new Query(Criteria.where("name").is(toolName));
        query.fields().include("name", "tool_options");

        return mongoTemplate.findOne(query, ToolMetaEntity.class);
    }

    public List<ToolMetaEntity> findAllToolMetasSelectingNameTypePattern() {
        Query query = new Query();
        query.fields().include("name", "type", "pattern");
        return mongoTemplate.find(query, ToolMetaEntity.class);
    }

}
