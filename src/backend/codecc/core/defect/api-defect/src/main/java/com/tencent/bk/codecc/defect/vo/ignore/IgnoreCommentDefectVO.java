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
 
package com.tencent.bk.codecc.defect.vo.ignore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 注释忽略信息视图
 * 
 * @date 2021/6/30
 * @version V1.0
 */
@Data
@ApiModel("注释忽略信息视图")
public class IgnoreCommentDefectVO {
    @ApiModelProperty("任务id")
    private Long taskId;

    @ApiModelProperty("注释忽略信息")
    private Map<String, List<IgnoreCommentDefectSubVO>> ignoreDefectMap;
}
