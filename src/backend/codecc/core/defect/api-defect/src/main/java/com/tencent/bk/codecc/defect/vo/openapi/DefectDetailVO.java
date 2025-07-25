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
 
package com.tencent.bk.codecc.defect.vo.openapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 告警详情视图
 * 
 * @date 2019/11/14
 * @version V1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("告警详情视图")
public class DefectDetailVO 
{
    @ApiModelProperty("严重级别")
    private Integer severity;

    @ApiModelProperty("忽略原因类型")
    private Integer ignoreReasonType;

    @ApiModelProperty("文件所在代码仓库地址")
    private String filePathName;

    @ApiModelProperty("告警作者")
    private Set<String> authorList;

    @ApiModelProperty("首次创建时间")
    private Long firstCreatedTime;

    @ApiModelProperty("规则名")
    private String checkerName;

    @ApiModelProperty("更新时间")
    private Long time;

    @ApiModelProperty("行号")
    private Integer lineNumber;

    @ApiModelProperty("告警详情链接")
    private String defectDetailUrl;

    @ApiModelProperty("版本号")
    private String fileVersion;

    @ApiModelProperty("告警状态")
    private Integer status;

    @ApiModelProperty("Coverity告警id")
    private Long cid;

}
