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

package com.tencent.bk.codecc.defect.model.admin;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 冒烟检查详情
 *
 * @version V1.0
 * @date 2021/5/31
 */

@Data
@Document(collection = "t_smoke_detail")
public class SmokeCheckDetailEntity {

    @Id
    private String entityId;

    /**
     * 归属id
     */
    @Field("belong_to_id")
    @Indexed
    private String belongToId;

    /**
     * 任务ID
     */
    @Field("task_id")
    private Long taskId;

    /**
     * 工具名
     */
    @Field("tool_name")
    private String toolName;

    /**
     * 冒烟前告警数
     */
    @Field("before_defect_count")
    private int beforeDefectCount;

    /**
     * 冒烟后告警数
     */
    @Field("after_defect_count")
    private int afterDefectCount;

    /**
     * 告警变化数
     */
    @Field("defect_change")
    private int defectChange;

    /**
     * 是否已执行
     */
    @Field("had_execute")
    private int hadExecute;

    /**
     * 冒烟执行扫描状态
     */
    @Field("analyze_date")
    private String analyzeDate;

    /**
     * 冒烟前耗时
     */
    @Field("before_elapse_time")
    private long beforeElapseTime;

    /**
     * 冒烟后耗时
     */
    @Field("after_elapse_time")
    private long afterElapseTime;
}
