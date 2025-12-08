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
 
package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

/**
 * 运行失败记录视图
 * 
 * @date 2020/7/15
 * @version V1.0
 */
@Data
@Schema(description = "运行失败记录视图")
public class TaskFailRecordVO
{
    @Schema(description = "任务id")
    private Long taskId;

    @Schema(description = "流水线id")
    private String pipelineId;

    @Schema(description = "项目id")
    private String projectId;

    @Schema(description = "构建id")
    private String buildId;

    @Schema(description = "母机ip")
    private String machineIp;

    @Schema(description = "构建id")
    private Boolean retryFlag;

    @Schema(description = "上报时间")
    private Long uploadTime;

    @Schema(description = "任务耗时")
    private Long timeCost;

    @Schema(description = "失败原子环节")
    private String failAtomName;
}
