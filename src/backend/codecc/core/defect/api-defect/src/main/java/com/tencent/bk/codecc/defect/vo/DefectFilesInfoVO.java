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

@Data
@ApiModel("告警相关文件信息")
public class DefectFilesInfoVO {
    @ApiModelProperty(value = "文件路径名", required = true)
    private String filePath;

    @ApiModelProperty(value = "文件MD5与文件路径名共同唯一标志一个文件", required = true)
    private String fileMd5;

    @ApiModelProperty(value = "文件内容", required = true)
    private String contents;

    @ApiModelProperty(value = "文件的开始行", required = true)
    private int startLine = 1;

    @ApiModelProperty(value = "告警跟踪信息在文件中的最小行")
    private int minLineNum = 1;

    @ApiModelProperty(value = "告警跟踪信息在文件中的最大行")
    private int maxLineNum = 1;
}
