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

package com.tencent.bk.codecc.task.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 任务id对应的工具信息
 *
 * @version V1.0
 * @date 2021/12/23
 */
@Data
public class TaskIdToolInfoEntity {

    @Field("task_id")
    private Long taskId;

    @Field("tool_name")
    private String toolName;

    @Field("gongfeng_project_id")
    private Long gongfengProjectId;
}
