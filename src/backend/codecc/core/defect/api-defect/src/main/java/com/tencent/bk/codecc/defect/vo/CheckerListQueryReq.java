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

import com.tencent.bk.codecc.defect.vo.enums.CheckerCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerRecommendType;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 查询规则清单首页实体类
 *
 * @date 2019/12/25
 * @version V1.0
 */
@Data
@Schema(description = "查询规则清单首页实体类")
public class CheckerListQueryReq {
    @Schema(description = "关键字")
    private String keyWord;

    @Schema(description = "语言")
    private Set<String> checkerLanguage;

    @Schema(description = "规则类型")
    private Set<CheckerCategory> checkerCategory;

    @Schema(description = "工具")
    private Set<String> toolName;

    @Schema(description = "标签")
    private Set<String> tag;

    @Schema(description = "严重等级")
    private Set<String> severity;

    @Schema(description = "可修改参数")
    private Set<Boolean> editable;

    @Schema(description = "推荐")
    private Set<CheckerRecommendType> checkerRecommend;

    @Schema(description = "规则集id")
    private String checkerSetId;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "是否规则集选中")
    private Set<Boolean> checkerSetSelected;

    @Schema(description = "规则创建来源")
    private Set<CheckerSource> checkerSource;

    @Schema(description = "查询请求是否来源于op")
    private Boolean isOp = false;
}
