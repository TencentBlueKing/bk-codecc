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
 
package com.tencent.bk.codecc.task.model;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * devcloud切换记录表
 * 
 * @date 2021/1/22
 * @version V1.0
 */
@Data
@Document(collection = "t_devcloud_switch")
public class DevCloudSwitchEntity {
    @Field("project_id")
    private String projectId;
    @Field("pipeline_id")
    @Indexed(background = true)
    private String pipelineId;
    @Field("gongfeng_project_id")
    @Indexed(background = true)
    private Integer gongfengProjectId;
}
