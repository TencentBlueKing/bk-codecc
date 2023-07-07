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

package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 工具构建信息视图
 * @version V1.0
 * @date 2021/5/7
 */
@Data
@ApiModel("工具构建信息")
public class ToolBuildInfoReqVO {

    @ApiModelProperty(value = "任务ID")
    private Long taskId;

    @ApiModelProperty("任务ID集合")
    private Collection<Long> taskIds;

    @ApiModelProperty(value = "工具名称")
    private String toolName;

    @ApiModelProperty("多个工具名称")
    private List<String> toolNames;

    @ApiModelProperty(value = "强制全量扫描标志 Y：强制全量扫描 N：按任务配置扫描")
    private String forceFullScan;

    @ApiModelProperty(value = "告警快照基准构建ID")
    private String defectBaseBuildId;

    @ApiModelProperty("创建来源")
    private Set<String> createFrom;
}
