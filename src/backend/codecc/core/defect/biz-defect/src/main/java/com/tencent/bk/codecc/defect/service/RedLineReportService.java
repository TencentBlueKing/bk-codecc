/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.model.defect.DefectEntity;
import com.tencent.bk.codecc.defect.model.redline.RedLineExtraParams;
import com.tencent.bk.codecc.defect.vo.redline.PipelineRedLineCallbackVO;
import com.tencent.bk.codecc.defect.vo.redline.RedLineVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;

import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import java.util.List;
import java.util.Map;

/**
 * 上报质量红线服务
 *
 * @version V1.0
 * @date 2019/7/4
 */
public interface RedLineReportService<T extends DefectEntity> {

    /**
     * 保存质量红线数据
     *
     * @param taskDetailVO
     * @param toolName
     * @param buildId
     */
    void saveRedLineData(TaskDetailVO taskDetailVO, String toolName, String buildId, List<T> newDefectList);

    /**
     * 保存质量红线数据
     *
     * @param taskDetailVO
     * @param toolName
     * @param buildId
     */
    void saveRedLineData(TaskDetailVO taskDetailVO, String toolName, String buildId, List<T> newDefectList,
                         RedLineExtraParams<T> extraParams);

    /**
     * 获取相应工具的红线指标数据
     *
     * @param taskDetailVO
     * @param toolInfo
     * @param toolConfig
     * @param metadataModel
     * @param metadataCallback
     * @param effectiveTools
     */
    void getAnalysisResult(TaskDetailVO taskDetailVO, ToolMetaBaseVO toolInfo,
            ToolConfigInfoVO toolConfig, Map<String, RedLineVO> metadataModel,
            PipelineRedLineCallbackVO metadataCallback, List<String> effectiveTools, String buildId,
            List<T> newDefectList, RedLineExtraParams<T> extraParams);
}
