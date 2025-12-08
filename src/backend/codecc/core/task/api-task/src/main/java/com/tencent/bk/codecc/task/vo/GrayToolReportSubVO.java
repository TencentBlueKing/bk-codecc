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

/**
 * 灰度报告子视图
 * 
 * @date 2021/1/8
 * @version V1.0
 */
@Schema(description = "灰度报告子视图")
@Data
public class GrayToolReportSubVO
{
    @Schema(description = "灰度总数")
    private Integer grayNum;

    @Schema(description = "总执行次数")
    private Integer totalNum;

    @Schema(description = "成功执行次数")
    private Integer successNum;

    @Schema(description = "成功执行次数")
    private String successRatio;

    @Schema(description = "告警数")
    private Integer defectCount;

    @Schema(description = "耗时")
    private Long elapsedTime;
}
