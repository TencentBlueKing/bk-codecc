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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警基础信息VO
 *
 * @version V1.0
 * @date 2019/10/18
 */
@Data
@Schema(description = "告警基础信息视图")
public class DefectDetailVO extends DefectBaseVO
{
    @Schema(description = "CWE(Common Weakness Enumeration),通用缺陷对照表")
    private Integer cwe;

    @Schema(description = "第三方平台的buildId")
    private String platformBuildId;

    @Schema(description = "第三方平台的项目ID")
    private String platformProjectId;

    @Schema(description = "有关告警实例的数据。可以有多个告警实例。")
    private List<DefectInstanceVO> defectInstances;

    @Schema(description = "告警涉及的相关文件信息。可以有多个。")
    private Map<String, DefectFilesInfoVO> fileInfoMap = new HashMap<>();

}
