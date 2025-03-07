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
 
package com.tencent.bk.codecc.task.dao.mongotemplate;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.task.model.GrayToolProjectEntity;
import com.tencent.bk.codecc.task.vo.GrayToolProjectReqVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * 灰度工具项目持久类
 * 
 * @date 2021/1/3
 * @version V1.0
 */
@Repository
public class GrayToolProjectDao {
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 根据project_id更新灰度项目记录
     * @param grayToolProjectEntity
     * @param user
     */
    public void upsertGrayToolProjectEntity(GrayToolProjectEntity grayToolProjectEntity,
                                            String user) {
        Query query = new Query();
        query.addCriteria(Criteria.where("project_id").is(grayToolProjectEntity.getProjectId()));
        Update update = new Update();
        update.set("project_id", grayToolProjectEntity.getProjectId());
        update.set("status", grayToolProjectEntity.getStatus());
        update.set("updated_date", System.currentTimeMillis());
        update.set("updated_by", user);
        update.set("created_date", System.currentTimeMillis());
        update.set("created_by", user);
        mongoTemplate.upsert(query, update, GrayToolProjectEntity.class);
    }

    public void findAndReplaceByToolNameAndProjectId(GrayToolProjectEntity entity) {
        Criteria criteria = Criteria.where("tool_name").is(entity.getToolName())
                .and("project_id").is(entity.getProjectId());
        Query query = new Query(criteria);
        mongoTemplate.findAndReplace(query, entity, FindAndReplaceOptions.options().upsert());
    }

    public List<GrayToolProjectEntity> findByToolNameAndProjectIdIn(String toolName, List<String> projectIdList) {
        Criteria criteria = Criteria.where("tool_name").is(toolName).and("project_id").in(projectIdList);
        Query query = new Query(criteria);

        return mongoTemplate.find(query, GrayToolProjectEntity.class);
    }

    /**
     * 分页查询灰度项目列表
     *
     * @param reqVO
     * @param pageable
     */
    public Page<GrayToolProjectEntity> findGrayToolPage(GrayToolProjectReqVO reqVO, Pageable pageable) {

        Criteria criteria = new Criteria();
        List<Criteria> criteriaList = Lists.newArrayList();

        // 项目id
        String projectId = reqVO.getProjectId();
        if (StringUtils.isNotEmpty(projectId)) {
            criteriaList.add(Criteria.where("project_id").is(projectId));
        }
        // 工具id
        String toolName = reqVO.getToolName();
        if (StringUtils.isNotEmpty(toolName)) {
            criteriaList.add(Criteria.where("tool_name").is(toolName));
        }
        // 创建人
        String createBy = reqVO.getCreatedBy();
        if (StringUtils.isNotEmpty(createBy)) {
            criteriaList.add(Criteria.where("created_by").is(createBy));
        }
        // 更新人
        String updateBy = reqVO.getUpdatedBy();
        if (StringUtils.isNotEmpty(updateBy)) {
            criteriaList.add(Criteria.where("updated_by").is(updateBy));
        }

        // 筛除机器创建项目-GRAY_TASK_POOL_*开头的项目(0:筛除 1:不筛除)
        Integer hasRobotTaskBool = reqVO.getHasRobotTaskBool();
        if (hasRobotTaskBool == 0) {
            criteriaList.add(Criteria.where("project_id").regex("^(?!GRAY_TASK_POOL_)"));
        }

        // 灰度状态
        if (reqVO.getStatus() != null) {
            criteriaList.add(Criteria.where("status").is(reqVO.getStatus()));
        }

        // 是否 开源治理项目
        if (reqVO.getOpenSourceProject() != null) {
            criteriaList.add(Criteria.where("is_open_source").is(reqVO.getOpenSourceProject()));
        }

        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        Query query = new Query();
        query.addCriteria(criteria);

        if (pageable != null) {
            query.with(pageable);
        }

        List<GrayToolProjectEntity> grayToolProjectEntityList =
                mongoTemplate.find(query, GrayToolProjectEntity.class, "t_gray_tool_project");

        Assert.assertNotNull("pageable is null!", pageable);
        return PageableExecutionUtils.getPage(grayToolProjectEntityList, pageable,
                () -> mongoTemplate.count(query.limit(-1).skip(-1), GrayToolProjectEntity.class));
    }

    /**
     * 获取灰度项目ID列表
     *
     * @return list
     */
    public List<GrayToolProjectEntity> findAllGrayProjectIdSet() {
        Document fieldsObj = new Document();
        fieldsObj.put("project_id", true);
        Query query = new BasicQuery(new Document(), fieldsObj);
        // 仅筛选所有灰度任务池的项目id
        query.addCriteria(Criteria.where("project_id").regex("^GRAY_TASK_POOL_"));
        return mongoTemplate.find(query, GrayToolProjectEntity.class, "t_gray_tool_project");
    }

    public long batchDeleteInProjectIds(String toolName, List<String> projectIds) {
        Criteria criteria = Criteria.where("tool_name").is(toolName).and("project_id").in(projectIds);
        Query query = new Query(criteria);

        return mongoTemplate.remove(query, GrayToolProjectEntity.class).getDeletedCount();
    }
}
