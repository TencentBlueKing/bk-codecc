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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 编辑任务信息请求体
 *
 * @version V1.0
 * @date 2021/5/25
 */
@Data
@ApiModel("编辑任务信息请求体")
public class TaskUpdateDeptInfoVO {

    @ApiModelProperty("任务ID")
    private Long taskId;

    @ApiModelProperty("任务拥有者")
    private List<String> taskOwner;

    @ApiModelProperty("事业群ID")
    private Integer bgId;

    @ApiModelProperty("业务线")
    private Integer businessLineId;

    @ApiModelProperty("部门ID")
    private Integer deptId;

    @ApiModelProperty("中心ID")
    private Integer centerId;

    @ApiModelProperty("项目组ID")
    private Integer groupId;
}
