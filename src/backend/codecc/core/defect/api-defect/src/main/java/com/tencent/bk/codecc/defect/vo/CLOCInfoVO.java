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

import com.tencent.devops.common.api.CommonVO;
import lombok.Data;

/**
 * cloc信息视图
 *
 * @version V1.0
 * @date 2019/9/29
 */
@Data
public class CLOCInfoVO extends CommonVO
{
    private Long blank;

    private Long code;

    private Long comment;

    private String language;

    private String filePath;

    private String pinpointHash;
}
