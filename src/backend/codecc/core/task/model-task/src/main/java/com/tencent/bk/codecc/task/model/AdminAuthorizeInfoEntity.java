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

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 管理员授权信息实体类
 *
 * @version V1.0
 * @date 2021/4/13
 */

@Data
@Document(collection = "t_admin_authorize_info")
public class AdminAuthorizeInfoEntity {

    /**
     * 实体id
     */
    @Id
    private String entityId;

    /**
     * 用户名ID
     */
    @Field("user_id")
    @Indexed
    private String userId;

    /**
     * 已授权的BG id
     */
    @Field("bg_id_list")
    private List<Integer> bgIdList;

    /**
     * 已授权的来源平台
     */
    @Field("create_froms")
    private List<String> createFroms;

    /**
     * 备注
     */
    @Field("remarks")
    private String remarks;

}
