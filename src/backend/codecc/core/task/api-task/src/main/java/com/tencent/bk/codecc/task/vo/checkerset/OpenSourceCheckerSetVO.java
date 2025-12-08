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

package com.tencent.bk.codecc.task.vo.checkerset;

import com.tencent.devops.common.api.OrgInfoVO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;
import lombok.Data;

/**
 * 规则子选项
 *
 * @version V4.0
 * @date 2021/06/18
 */

@Data
@Schema
public class OpenSourceCheckerSetVO {

    @Schema
    private String checkerSetId;

    @Schema
    private Set<String> toolList;

    @Schema
    private String checkerSetType;

    @Schema
    private Integer version;

    @Schema(description = "之前版本号")
    private Integer lastVersion;

    @Schema(description = "规则集名称")
    private String checkerSetName;

    @Schema(description = "可见范围")
    private List<OrgInfoVO> scopes;

    @Schema(description = "可见范围-任务创建来源")
    private List<String> taskCreateFromScopes;

    @Schema(description = "代码语言/code")
    private String lang;

    @Schema(description = "版本类型: enum ToolIntegratedStatus")
    private String versionType;

    @Schema(description = "管理类型")
    private String manageType;
}