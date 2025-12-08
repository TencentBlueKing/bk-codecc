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

import com.tencent.bk.codecc.defect.vo.enums.CheckerSetCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Data;

/**
 * 查询规则集清单首页实体类
 *
 * @version V1.0
 * @date 2020/1/7
 */
@Data
@Schema(description = "查询规则集清单首页实体类")
public class CheckerSetListQueryReq {

    @Schema(description = "项目id")
    private String projectId;

    @Schema(description = "任务id")
    private Long taskId;

    @Schema(description = "关键字")
    private String keyWord;

    @Schema(description = "语言")
    private Set<String> checkerSetLanguage;

    @Schema(description = "规则集类别")
    private Set<CheckerSetCategory> checkerSetCategory;

    @Schema(description = "工具名")
    private Set<String> toolName;

    @Schema(description = "规则集来源")
    private Set<CheckerSetSource> checkerSetSource;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "快速搜索框")
    private String quickSearch;

    @Schema(description = "排序字段")
    private String sortField;

    @Schema(description = "排序字段")
    private String sortType;

    @Schema(description = "分页配置")
    private Integer pageNum;

    @Schema(description = "分页大小配置")
    private Integer pageSize;
}
