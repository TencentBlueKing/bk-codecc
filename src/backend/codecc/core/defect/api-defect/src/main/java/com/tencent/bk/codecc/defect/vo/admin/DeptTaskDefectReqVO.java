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

package com.tencent.bk.codecc.defect.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Collection;
import java.util.List;

/**
 * 按组织架构查询任务告警请求体
 *
 * @version V1.0
 * @date 2020/2/12
 */

@Data
@ApiModel("按组织架构查询任务告警请求体")
public class DeptTaskDefectReqVO {
    @ApiModelProperty("工具名称")
    private String toolName;

    @ApiModelProperty("任务ID集合")
    private Collection<Long> taskIds;

    @ApiModelProperty("事业群ID")
    private Integer bgId;

    @ApiModelProperty("部门ID")
    private List<Integer> deptIds;

    @ApiModelProperty("开始时间")
    private String startDate;

    @ApiModelProperty("截止时间")
    private String endDate;

    @ApiModelProperty("创建来源")
    private List<String> createFrom;

    @ApiModelProperty("遗留告警超时天数阈值")
    private Integer timeoutDays;

    @ApiModelProperty("告警严重级别筛选")
    private Integer severity;

}
