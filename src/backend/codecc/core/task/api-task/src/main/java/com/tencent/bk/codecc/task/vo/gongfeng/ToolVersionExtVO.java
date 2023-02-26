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

package com.tencent.bk.codecc.task.vo.gongfeng;

import com.tencent.devops.common.api.ToolVersionVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 工具镜像版本信息
 *
 * @version V1.0
 * @date 2021/6/21
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class ToolVersionExtVO extends ToolVersionVO {

    @ApiModelProperty("之前镜像版本号")
    private String lastDockerImageVersion;

    @ApiModelProperty("工具名")
    private String toolName;

    @ApiModelProperty("镜像版本号下拉选项")
    private List<String> imageVersionList;

    @ApiModelProperty("镜像Hash值下拉选项")
    private List<String> imageHashList;
}
