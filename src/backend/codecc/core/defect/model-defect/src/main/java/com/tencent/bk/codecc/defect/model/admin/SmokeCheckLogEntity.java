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

package com.tencent.bk.codecc.defect.model.admin;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 冒烟检查日志列表
 *
 * @version V1.0
 * @date 2021/5/31
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_smoke_log")
public class SmokeCheckLogEntity extends CommonEntity {

    /**
     * 工具名
     */
    @Field("tool_name")
    @Indexed
    private String toolName;

    /**
     * 符合条件的任务ID数
     */
    @Field("task_id_count")
    private int taskIdCount;

    /**
     * 筛选任务ID的分级条件
     */
    @Field("filter_text")
    private String filterText;

    /**
     * 备注
     */
    @Field("remarks")
    private String remarks;

}
