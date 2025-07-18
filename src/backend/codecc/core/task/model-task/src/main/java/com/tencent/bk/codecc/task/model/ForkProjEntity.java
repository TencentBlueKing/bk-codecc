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
 
package com.tencent.bk.codecc.task.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * fork信息实体类
 * 
 * @date 2019/12/31
 * @version V1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ForkProjEntity 
{
    @Field("path")
    private String path;

    @Field("path_with_namespace")
    private String pathWithNameSpace;

    @Field("name")
    private String name;

    @Field("id")
    private Integer id;

    @Field("name_with_namespace")
    private String nameWithNameSpace;
}
