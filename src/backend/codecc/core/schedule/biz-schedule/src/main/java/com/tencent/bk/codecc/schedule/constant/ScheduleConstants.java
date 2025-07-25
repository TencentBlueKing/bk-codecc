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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 *  Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.schedule.constant;

/**
 * Coverity模块的常量
 *
 * @version V1.0
 * @date 2019/10/2
 */
public interface ScheduleConstants {
    /**
     * 分片文件的后缀
     */
    String CHUNK_FILE_SUFFIX = ".chunk";

    /**
     * 上传类型
     */
    enum UploadType {
        /**
         * 上传中间结果
         */
        SUCCESS_RESULT,

        /**
         * 上传失败的中间结果，所在目录/data/bkee/codecc/cfs/fail_result_upload
         */
        FAIL_RESULT,

        /**
         * 上传的json文件
         */
        SCM_JSON,

        /**
         * 聚类的临时文件
         */
        AGGREGATE,
        /**
         * 文件缓存的临时文件
         */
        FILE_CACHE;
    }

    /**
     * 下载类型
     */
    enum DownloadType {
        /**
         * 上传失败的中间结果，所在目录/data/bkee/codecc/cfs/result_upload
         */
        LAST_RESULT,

        /**
         * 工具客户端，所在目录/data/bkee/codecc/nfs/tool_client_download
         */
        TOOL_CLIENT,

        /**
         * 构建脚本,bin.zip，所在目录/data/bkee/codecc/nfs/script_download
         */
        BUILD_SCRIPT,

        /**
         * scm工具，所在目录/data/bkee/codecc/nfs/tool_client_download/scm_tool
         */
        SCM_TOOL,

        /**
         * 构建脚本，所在目录/data/bkee/codecc/nfs/tool_client_download/p4_tool
         */
        P4_TOOL,

        /**
         * 聚类的临时文件
         */
        GATHER,

        /**
         * OP运营数据生成的Excel文件 /data/bkee/codecc/nfs/download/op_excel
         */
        OP_EXCEL;
    }

    /**
     * 分析服务器RPC方法
     */
    enum RpcMethod {
        /**
         * 触发分析接口
         */
        TRIGGER("trigger_agent_analyze_job"),

        /**
         * 中断分析接口
         */
        ABORT("abort_agent_analyze_job"),

        /**
         * 检查分析进程是否
         */
        CHECK("check_agent_analyze_job"),

        /**
         * 检查分析进程是否
         */
        COMMIT("trigger_agent_commit_job");

        private String methodName;

        RpcMethod(String methodName) {
            this.methodName = methodName;
        }

        public String methodName() {
            return this.methodName;
        }
    }
}
