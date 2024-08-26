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

package com.tencent.bk.codecc.scanschedule.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 工具扫描告警记录的实体类
 *
 * @author jimxzcai
 * @version V1.0
 * @date 2019/11/4
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_simple_defect")
public class SimpleDefectEntity extends CommonEntity {

    /**
     * scanId
     */
    @Field("scan_id")
    @Indexed
    private String scanId;

    /**
     * 告警作者
     */
    private String author;

    /**
     * 工具名称
     */
    @Field("tool_name")
    private String toolName;

    /**
     * 规则名称
     */
    private String checker;

    /**
     * 规则等级
     */
    private int severity;

    /**
     * 规则描述
     */
    private String message;

    /**
     * 扫描文本行
     */
    @Field("line_num")
    private int lineNum;

    /**
     * 告警创建时间
     */
    @Field("create_time")
    private Long createTime;
}