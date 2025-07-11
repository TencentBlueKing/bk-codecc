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
package com.tencent.bk.codecc.defect.vo.openapi;

import com.tencent.devops.common.api.pojo.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 任务概览统计响应视图
 *
 * @version V1.0
 * @date 2020/3/16
 */

@Data
@ApiModel("任务概览统计响应视图")
public class TaskOverviewDetailRspVO
{
    @ApiModelProperty("任务概览清单")
    private Page<TaskOverviewDetailVO> statisticsTask;

    @ApiModelProperty("个性化扫描任务清单")
    private Page<CustomTaskOverviewVO> statCustomTask;

}
