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

package com.tencent.bk.codecc.scanschedule.pojo.tool;


import com.google.common.collect.Lists;
import com.tencent.bk.codecc.scanschedule.pojo.input.ToolEnv;
import lombok.Data;

import java.util.List;

/**
 * 二进制数据结构
 * @version V1.0
 * @date 2022/8/18
 */
@Data
public class Binary {
    private String winUrl = ""; //二进制windows版本下载路径
    private String linuxUrl = ""; //二进制Linux版本下载路径
    private String macUrl = ""; //二进制mac版本下载路径
    private String binaryVersion = ""; //二进制版本
    private String winCommand = ""; //二进制Windows运行命令
    private String linuxCommand = ""; //二进制linux运行命令
    private String macCommand = ""; //二进制mac运行命令
    private List<ToolEnv> toolEnvs = Lists.newArrayList(); //二进制工具依赖环境
}
