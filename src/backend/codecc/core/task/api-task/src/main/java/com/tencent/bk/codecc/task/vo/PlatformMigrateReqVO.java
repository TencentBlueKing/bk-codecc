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

package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Platform迁移请求视图
 *
 * @version V1.0
 * @date 2021/7/15
 */

@Data
@ApiModel("Platform迁移请求视图")
public class PlatformMigrateReqVO {

    @ApiModelProperty("工具名")
    private String toolName;

    @ApiModelProperty("原IP")
    private String sourceIp;

    @ApiModelProperty("目标IP")
    private String targetIp;

    @ApiModelProperty("备注")
    private String remarks;

    @ApiModelProperty("迁移记录id")
    private String entityId;
}
