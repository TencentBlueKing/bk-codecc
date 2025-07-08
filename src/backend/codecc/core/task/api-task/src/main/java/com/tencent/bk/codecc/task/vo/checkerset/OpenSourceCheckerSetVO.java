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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel
public class OpenSourceCheckerSetVO {

    @ApiModelProperty
    private String checkerSetId;

    @ApiModelProperty
    private Set<String> toolList;

    @ApiModelProperty
    private String checkerSetType;

    @ApiModelProperty
    private Integer version;

    @ApiModelProperty("之前版本号")
    private Integer lastVersion;

    @ApiModelProperty("规则集名称")
    private String checkerSetName;

    @ApiModelProperty("可见范围")
    private List<OrgInfoVO> scopes;

    @ApiModelProperty("可见范围-任务创建来源")
    private List<String> taskCreateFromScopes;

    @ApiModelProperty("代码语言/code")
    private String lang;

    @ApiModelProperty("版本类型: enum ToolIntegratedStatus")
    private String versionType;

    @ApiModelProperty("管理类型")
    private String manageType;
}