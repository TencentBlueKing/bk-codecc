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

package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 日志查询模型
 *
 * @version V1.0
 * @date 2019/7/12
 */
@Data
@ApiModel("日志查询模型")
public class QueryLogRepVO
{
    @ApiModelProperty(value = "构建ID")
    private String buildId;

    @ApiModelProperty(value = "是否结束")
    private Boolean finished;

    @ApiModelProperty(value = "日志列表")
    private List<LogLine> logs;

    @ApiModelProperty(value = "所用时间")
    private Long timeUsed;

    @ApiModelProperty(value = "日志查询状态")
    private Integer status;


    @ApiModel("日志模型")
    @Data
    private class LogLine
    {
        @ApiModelProperty(value = "日志行号")
        private Long lineNo;
        @ApiModelProperty(value = "日志时间戳")
        private Long timestamp;
        @ApiModelProperty(value = "日志消息体")
        private String message;
        @ApiModelProperty(value = "日志权重级")
        private Byte priority;
        @ApiModelProperty(value = "日志tag")
        private String tag;
        @ApiModelProperty(value = "日志执行次数")
        private int executeCount = 1;

    }

    @ApiModel("日志状态")
    public enum LogStatus
    {
        /**
         * 正常结束
         */
        SUCCEED(0),
        /**
         * 日志为空
         */
        EMPTY(1),
        /**
         * 日志被清除
         */
        CLEAN(2),
        /**
         * 日志被关闭
         */
        CLOSED(3),
        /**
         * 其他异常
         */
        FAIL(999);

        private int value;

        LogStatus(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }

    }

}

