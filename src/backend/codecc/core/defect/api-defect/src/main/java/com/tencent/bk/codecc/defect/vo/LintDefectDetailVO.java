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
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ApiModel("告警详细信息视图")
public class LintDefectDetailVO extends LintDefectVO {
    @ApiModelProperty(value = "有关告警实例的数据。可以有多个告警实例。")
    private List<DefectInstanceVO> defectInstances;

    @ApiModelProperty(value = "告警涉及的相关文件信息。可以有多个。")
    private Map<String, DefectFilesInfoVO> fileInfoMap = new HashMap<>();

    @ApiModelProperty(value = "文件对应仓库版本号--COMMON类工具专有")
    private String fileVersion;
}
