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

package com.tencent.bk.codecc.defect.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 冒烟检查详情视图
 *
 * @version V1.0
 * @date 2021/5/31
 */

@Data
@ApiModel("冒烟检查详情视图")
public class SmokeCheckDetailVO {

    @ApiModelProperty("任务ID")
    private Long taskId;

    @ApiModelProperty("工具名")
    private String toolName;

    @ApiModelProperty("项目id")
    private String projectId;

    @ApiModelProperty("冒烟前告警数")
    private int beforeDefectCount;

    @ApiModelProperty("冒烟后告警数")
    private int afterDefectCount;

    @ApiModelProperty("告警变化数")
    private int defectChange;

    @ApiModelProperty("是否已执行")
    private int hadExecute;

    @ApiModelProperty("最近分析时间")
    private String analyzeDate;

    @ApiModelProperty("冒烟前耗时")
    private long beforeElapseTime;

    @ApiModelProperty("冒烟后耗时")
    private long afterElapseTime;
}
