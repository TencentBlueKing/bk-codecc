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

package com.tencent.devops.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 用户登录统计视图
 *
 * @version V1.0
 * @date 2020/10/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户登录统计视图")
public class UserLogInfoStatVO extends CommonVO {

    @Schema(description = "用户名")
    private String userName;


    @Schema(description = "首次登录时间")
    private Long firstLogin;


    @Schema(description = "最近登录时间")
    private Long lastLogin;


    @Schema(description = "事业群")
    private String bgName;


    @Schema(description = "部门")
    private String deptName;


    @Schema(description = "中心")
    private String centerName;

}
