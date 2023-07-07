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

package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 管理员授权信息
 *
 * @version V1.0
 * @date 2021/4/13
 */

@Data
public class AdminAuthorizeInfoVO {

    @ApiModelProperty("用户名ID")
    private String userId;

    @ApiModelProperty("用户名ID集合")
    private List<String> userIdList;

    @ApiModelProperty("已授权的BG id")
    private List<Integer> bgIdList;

    @ApiModelProperty("已授权的来源平台：开源/非开源")
    private List<String> createFroms;

    @ApiModelProperty("备注")
    private String remarks;

}
