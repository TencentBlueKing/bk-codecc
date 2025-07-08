/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.util;


import org.apache.commons.lang3.StringUtils;

public class ToolUtils {

    private static final String COMMIT_TOOL_SEPARATE_FLAG = "@";

    /**
     * 将带有CommmitId的路径转换为正常的路径
     * 扫描Commit的工具，会再路径中自带commitId (例：/1.go@12312312 中 12312312 为commitId)，需要去除工具自带的CommitId
     *
     * @param path
     * @return
     */
    public static String convertCommitToolPathToCommon(String path) {
        if (StringUtils.isBlank(path) || !path.contains(COMMIT_TOOL_SEPARATE_FLAG)) {
            return path;
        }
        return path.substring(0, path.lastIndexOf(COMMIT_TOOL_SEPARATE_FLAG));
    }

}
