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

package com.tencent.bk.codecc.defect.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 重复率告警展示信息
 *
 * @version V1.0
 * @date 2019/6/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("圈复杂度缺陷视图")
public class DUPCDefectVO extends CommonVO
{
    /**
     * 任务id
     */
    @ApiModelProperty("告警的唯一标识，是相对路径的MD5")
    private Long taskId;

    /**
     * 工具名称
     */
    @ApiModelProperty("告警的唯一标识，是相对路径的MD5")
    private String toolName;

    /**
     * 相对路径，是文件的唯一标志，是除去文件在服务器上存在的根目录后的路径
     * rel_path，file_path，url三者的区别：
     * rel_path: src/crypto/block.go,
     * file_path: /data/iegci/multi_tool_code_resource_5/maoyan0417001_dupc/src/crypto/block.go,
     * url: http://svn.xxx.com/codecc/test_project_proj/branches/test/Go/go-master/src/crypto/block.go,
     */
    @ApiModelProperty("相对路径，是文件的唯一标志")
    private String relPath;

    @ApiModelProperty("代码仓库地址")
    private String url;

    @ApiModelProperty("文件路径")
    private String filePath;

    @ApiModelProperty("文件名")
    private String fileName;

    @ApiModelProperty("总行数")
    private Long totalLines;

    @ApiModelProperty("重复行数")
    private Long dupLines;

    @ApiModelProperty("重复率")
    private String dupRate;

    @ApiModelProperty("重复块数")
    private Integer blockNum;

    @ApiModelProperty("作者列表")
    private String authorList;

    @ApiModelProperty("文件的最新更新时间")
    private Long fileChangeTime;

    @ApiModelProperty("告警状态：NEW(1)，FIXED(2)，IGNORE(4)，PATH_MASK(8)，CHECKER_MASK(16)")
    private Integer status;

    @ApiModelProperty("风险系数，极高-1, 高-2，中-4，低-8")
    private Integer riskFactor;

    @ApiModelProperty("告警创建时间")
    private Long createTime;

    @ApiModelProperty("告警修复时间")
    private Long fixedTime;

    @ApiModelProperty("告警忽略时间")
    private Long ignoreTime;

    @ApiModelProperty("告警屏蔽时间")
    private Long excludeTime;

    @ApiModelProperty("缺陷数据的最后更新时间")
    private Long lastUpdateTime;

    @ApiModelProperty("文件版本号")
    private String revision;

    @ApiModelProperty("分支名")
    private String branch;

    @ApiModelProperty("Git子模块")
    private String subModule;

    /**
     * 发现该告警的最近分析版本号，项目工具每次分析都有一个版本，用于区分一个方法是哪个版本扫描出来的，根据版本号来判断是否修复，格式：
     * ANALYSIS_VERSION:projId:toolName
     */
    @ApiModelProperty("发现该告警的最近分析版本号")
    private String analysisVersion;
}
