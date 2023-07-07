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

package com.tencent.devops.common.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Set;

/**
 * 企业微信通知视图
 *
 * @version V1.0
 * @date 2022/3/13
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("企业微信通知视图")
public class RtxNotifyVO {
    @ApiModelProperty("接收人列表")
    private Set<String> receivers;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("通知内容")
    private String substance;

    @ApiModelProperty("接受人类型 enum WeworkReceiverType: single、group")
    private String receiverType;

    @ApiModelProperty("文本类型 enum WeworkTextType: text、markdown")
    private String textType;
}