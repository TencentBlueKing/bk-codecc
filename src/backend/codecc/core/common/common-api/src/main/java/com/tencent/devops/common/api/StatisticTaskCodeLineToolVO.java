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

package com.tencent.devops.common.api;

import com.tencent.devops.common.constant.ComConstants;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 定时任务统计任务数、代码行、工具数 请求体
 *
 * @version V1.0
 * @date 2022/5/7
 */
@Data
public class StatisticTaskCodeLineToolVO {

    @ApiModelProperty(value = "数据来源范围")
    private List<ComConstants.DefectStatType> dataFromList;

    @ApiModelProperty(value = "截止时间戳")
    private Long endTime;

    @ApiModelProperty(value = "统计日期")
    private String date;

    @ApiModelProperty(value = "工具列表")
    private String toolOrder;
}
