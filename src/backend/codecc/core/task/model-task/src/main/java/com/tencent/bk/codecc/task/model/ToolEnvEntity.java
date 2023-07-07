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

package com.tencent.bk.codecc.task.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 工具依赖环境版本
 *
 * @version V4.0
 * @date 2020/12/29
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToolEnvEntity {
    /**
     * 依赖的环境命令
     */
    @Field("depend_bin")
    private String dependBin;

    /**
     * 依赖的环境命令版本
     */
    @Field("depend_version")
    private String dependVersion;

}
