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

package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;


/**
 * AI修复建议记录表
 *
 * @author jimxzcai
 * @version V1.0
 * @date 2019/11/4
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_defect_suggestion")
public class DefectSuggestionRecordEntity extends CommonEntity {

    /**
     * 告警主Id
     */
    @Field("defect_id")
    @Indexed
    private String defectId;

    /**
     * 创建时间
     */
    @Field("created_after_date")
    @Indexed(name = "createdDateIndex", expireAfter = "P7D")
    private Long createdAfterDate;

    /**
     * AI模型类型
     */
    @Field("llm_name")
    private String llmName;

    /**
     * 项目Id
     */
    @Field("project_id")
    private String projectId;

    /**
     * 任务Id
     */
    @Field("task_id")
    private String taskId;

    /**
     * 用户名
     */
    @Field("user_id")
    private String userId;

    /**
     * 流响应
     */
    @Field("stream")
    private Boolean stream;

    /**
     * 修复建议内容
     */
    @Field("content")
    private String content;

    /**
     * 修复建议好评价
     */
    @Field("good_evaluates")
    private List<String> goodEvaluates;

    /**
     * 修复建议坏评价
     */
    @Field("bad_evaluates")
    private List<String> badEvaluates;
}
