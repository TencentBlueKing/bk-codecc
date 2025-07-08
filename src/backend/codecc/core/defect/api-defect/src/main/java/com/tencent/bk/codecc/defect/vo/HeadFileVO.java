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

import java.util.Set;

/**
 * 头文件路径信息视图
 * 
 * @date 2021/12/31
 * @version V1.0
 */
@Data
@ApiModel("头文件路径信息视图")
public class HeadFileVO {
    @ApiModelProperty("头文件路径信息视图")
    private Long taskId;

    @ApiModelProperty("头文件路径清单")
    private Set<String> headFileSet;
}
