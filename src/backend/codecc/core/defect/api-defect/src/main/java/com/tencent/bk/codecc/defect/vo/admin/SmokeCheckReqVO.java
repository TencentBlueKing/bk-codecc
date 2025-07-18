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

package com.tencent.bk.codecc.defect.vo.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Set;

/**
 * 冒烟检查请求体
 *
 * @version V1.0
 * @date 2021/5/31
 */

@Data
@ApiModel("冒烟检查请求体")
public class SmokeCheckReqVO {

    @ApiModelProperty("工具名集合")
    private Set<String> toolNameSet;

    @ApiModelProperty("参数集合")
    private Set<SmokeParam> smokeParamSet;

    @ApiModelProperty("备注")
    private String remarks;
}
