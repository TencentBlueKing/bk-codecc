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

package com.tencent.bk.codecc.defect.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代码行数视图
 *
 * @version V1.0
 * @date 2020/3/31
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("代码行数视图")
public class CodeLineModel {

    @ApiModelProperty("语言名称")
    private String language;

    @ApiModelProperty("代码行数")
    private Long codeLine;

    @ApiModelProperty("注释行")
    private Long commentLine;

    @ApiModelProperty("有效注释行")
    private Long efficientCommentLine;

    @ApiModelProperty("空白行")
    private Long blankLine;

    @ApiModelProperty("语言值")
    private Long langValue;

    @ApiModelProperty("文件数")
    private Long fileNum;
}
