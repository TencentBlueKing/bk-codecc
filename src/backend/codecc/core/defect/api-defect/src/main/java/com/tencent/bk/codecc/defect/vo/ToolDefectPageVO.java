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

package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具告警id视图
 *
 * @version V1.0
 * @date 2022/3/7
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("工具告警id视图")
public class ToolDefectPageVO {

    private long taskId;

    private String toolName;

    /**
     * 告警id， 任务、工具下唯一
     */
    @ApiModelProperty("告警id， 任务、工具下唯一")
    private List<String> id;
    /**
     * 总数
     */
    private Long count;
}
