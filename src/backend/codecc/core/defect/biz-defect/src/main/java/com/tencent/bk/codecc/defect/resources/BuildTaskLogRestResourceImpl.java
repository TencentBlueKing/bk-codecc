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

package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.BuildTaskLogRestResource;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.bk.codecc.defect.vo.GrayBuildNumAndTaskVO;
import com.tencent.bk.codecc.defect.vo.GrayTaskLogRepoInfoVO;
import com.tencent.bk.codecc.defect.vo.TaskLogRepoInfoVO;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.bk.codecc.defect.vo.UploadToolErrorTaskLogVO;
import com.tencent.devops.common.api.constant.CommonMessageCode;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import com.tencent.devops.common.web.RestResource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Locale;

/**
 * 分析任务构建机接口实现
 *
 * @version V1.0
 * @date 2019/7/21
 */
@RestResource
public class BuildTaskLogRestResourceImpl implements BuildTaskLogRestResource {
    private static Logger logger = LoggerFactory.getLogger(BuildTaskLogRestResourceImpl.class);

    @Autowired
    private BizServiceFactory<IBizService> bizServiceFactory;

    @Autowired
    private TaskLogService taskLogService;

    @Override
    public Result uploadTaskLog(UploadTaskLogStepVO uploadTaskLogStepVO) {
        if (StringUtils.isNotEmpty(uploadTaskLogStepVO.getToolName())) {
            uploadTaskLogStepVO.setToolName(uploadTaskLogStepVO.getToolName().toUpperCase());
        }
        logger.info("recv task: {}, step: {}, flag: {}, start: {}, end: {}", uploadTaskLogStepVO.getStreamName(),
                uploadTaskLogStepVO.getStepNum(), uploadTaskLogStepVO.getFlag(),
                uploadTaskLogStepVO.getStartTime(), uploadTaskLogStepVO.getEndTime());
        IBizService taskLogService = bizServiceFactory.createBizService(uploadTaskLogStepVO.getToolName(),
                ComConstants.BusinessType.ANALYZE_TASK.value(), IBizService.class);
        return taskLogService.processBiz(uploadTaskLogStepVO);
    }

    @Override
    public Result<Boolean> uploadToolErrorTaskLog(UploadToolErrorTaskLogVO uploadToolErrorTaskLogVO) {
        if (uploadToolErrorTaskLogVO.getTaskId() == null
                || StringUtils.isEmpty(uploadToolErrorTaskLogVO.getLandunBuildId())) {
            throw new CodeCCException(CommonMessageCode.ERROR_INVALID_PARAM_, new String[]{"非法空参数"});
        }
        if (StringUtils.isNotEmpty(uploadToolErrorTaskLogVO.getToolName())) {
            uploadToolErrorTaskLogVO.setToolName(uploadToolErrorTaskLogVO.getToolName().toUpperCase(Locale.ENGLISH));
        }
        logger.info("start to report tool error task log taskId:{} buildId:{} toolName:{}",
            uploadToolErrorTaskLogVO.getTaskId(),
            uploadToolErrorTaskLogVO.getLandunBuildId(),
            uploadToolErrorTaskLogVO.getToolName());

        return new Result<>(taskLogService.uploadToolErrorTaskLog(uploadToolErrorTaskLogVO));
    }


    @Override
    public Result<Boolean> uploadDirStructSuggestParam(UploadTaskLogStepVO uploadTaskLogStepVO) {
        if (StringUtils.isNotEmpty(uploadTaskLogStepVO.getToolName())) {
            uploadTaskLogStepVO.setToolName(uploadTaskLogStepVO.getToolName().toUpperCase());
        }
        return new Result<>(taskLogService.uploadDirStructSuggestParam(uploadTaskLogStepVO));
    }

    @Override
    public Result<TaskLogVO> getBuildTaskLog(long taskId, String toolName, String buildId) {
        return new Result<>(taskLogService.getBuildTaskLog(taskId, toolName.toUpperCase(), buildId));
    }

    @Override
    public Result<TaskLogRepoInfoVO> getLastAnalyzeRepoInfo(Long taskId, String toolName) {
        return new Result<>(taskLogService.getLastAnalyzeRepoInfo(taskId, toolName));
    }

    @Override
    public Result<List<GrayTaskLogRepoInfoVO>> getTaskLogInfoByBuildBumAndTaskId(
            GrayBuildNumAndTaskVO grayBuildNumAndTaskVO
    ) {
        return new Result<>(taskLogService.getTaskLogInfoByBuildBumAndTaskId(grayBuildNumAndTaskVO));
    }

    @Override
    public Result<List<TaskLogVO>> listBuildTaskLog(long taskId, String toolName, int range) {
        return new Result<>(taskLogService.listBuildTaskLog(taskId, toolName, range));
    }
}
