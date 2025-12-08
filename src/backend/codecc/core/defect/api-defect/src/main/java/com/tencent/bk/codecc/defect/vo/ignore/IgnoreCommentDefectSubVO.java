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
 
package com.tencent.bk.codecc.defect.vo.ignore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 注释忽略子视图
 * 
 * @date 2021/6/30
 * @version V1.0
 */
@Data
@Schema(description = "注释忽略子视图")
public class IgnoreCommentDefectSubVO {
    @Schema(description = "行号")
    private Integer lineNum;

    @Schema(description = "忽略规则")
    private Map<String, String> ignoreRule;
}
