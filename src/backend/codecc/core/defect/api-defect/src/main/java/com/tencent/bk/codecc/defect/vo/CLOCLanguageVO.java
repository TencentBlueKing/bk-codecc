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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * CLOC语言视图
 * 
 * @date 2020/4/9
 * @version V1.0
 */
@Data
public class CLOCLanguageVO 
{
    @Schema(description = "cloc语言信息")
    private String language;

    @Schema(description = "代码总和")
    private Long codeSum;

    @Schema(description = "空行总和")
    private Long blankSum;

    @Schema(description = "注释总和")
    private Long commentSum;

    @Schema(description = "有效注释总和")
    private Long efficientCommentSum;
}
