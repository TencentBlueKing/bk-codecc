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

import com.tencent.bk.codecc.defect.api.ServiceDefectRestResource;
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService;
import com.tencent.bk.codecc.defect.service.GetTaskLogService;
import com.tencent.bk.codecc.defect.service.IQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.impl.StatQueryWarningServiceImpl;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.GrayBuildNumAndTaskVO;
import com.tencent.bk.codecc.defect.vo.GrayDefectStaticVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectIdVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectPageVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import com.tencent.devops.common.web.RestResource;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 告警模块树服务实现
 *
 * @version V1.0
 * @date 2019/10/20
 */
@RestResource
@Slf4j
public class ServiceDefectRestResourceImpl implements ServiceDefectRestResource {

    @Autowired
    private BizServiceFactory<IBizService> bizServiceFactory;
    @Autowired
    private StatQueryWarningServiceImpl statQueryWarningService;
    @Autowired
    private BizServiceFactory<IQueryWarningBizService> fileAndDefectQueryFactory;
    @Autowired
    private GetTaskLogService getTaskLogService;
    @Autowired
    private CommonDefectMigrationService commonDefectMigrationService;

    @Override
    public Result<Boolean> batchDefectProcess(long taskId,
            String userName,
            BatchDefectProcessReqVO batchDefectProcessReqVO) {
        batchDefectProcessReqVO.setTaskId(taskId);
        batchDefectProcessReqVO.setIgnoreAuthor(userName);
        IBizService<BatchDefectProcessReqVO> bizService =
                bizServiceFactory.createBizService(batchDefectProcessReqVO.getToolNameList(),
                        batchDefectProcessReqVO.getDimensionList(),
                        ComConstants.BATCH_PROCESSOR_INFIX + batchDefectProcessReqVO.getBizType(),
                        IBizService.class);
        return bizService.processBiz(batchDefectProcessReqVO);
    }

    @Override
    public Result<Long> lastestStatDefect(long taskId, String toolName) {
        return new Result<>(statQueryWarningService.getLastestMsgTime(taskId));
    }

    @Override
    public Result<Map<Long, String>> getLatestAnalyzeStatus(List<Long> taskIds) {
        return new Result<>(getTaskLogService.getLatestAnalyzeStatus(taskIds));
    }

    @Override
    public Result<List<ToolDefectIdVO>> queryDefectIdByCondition(
            long taskId,
            @NotNull DefectQueryReqVO reqVO
    ) {
        IQueryWarningBizService service = fileAndDefectQueryFactory.createBizService(
                reqVO.getToolNameList(),
                reqVO.getDimensionList(),
                ComConstants.BusinessType.QUERY_WARNING.value(),
                IQueryWarningBizService.class
        );

        return new Result<>(service.queryDefectsByQueryCond(taskId, reqVO));
    }

    @Override
    public Result<ToolDefectPageVO> queryDefectIdPageByCondition(long taskId, DefectQueryReqVO reqVO,
            int pageNum, int pageSize) {
        IQueryWarningBizService service = fileAndDefectQueryFactory.createBizService(
                reqVO.getToolNameList(),
                reqVO.getDimensionList(),
                ComConstants.BusinessType.QUERY_WARNING.value(),
                IQueryWarningBizService.class
        );

        return new Result<>(service.queryDefectsByQueryCondWithPage(taskId, reqVO, pageNum, pageSize));
    }


    @Override
    public Result<Boolean> commonToLintMigrationSuccessful(long taskId) {
        return new Result<>(commonDefectMigrationService.isMigrationSuccessful(taskId));
    }

    @Override
    public Result<List<GrayDefectStaticVO>> getGaryDefectStaticList(GrayBuildNumAndTaskVO grayBuildNumAndTaskVO) {
        IQueryWarningBizService bizService = fileAndDefectQueryFactory.createBizService(
                ComConstants.ToolPattern.LINT.name(), ComConstants.BusinessType.QUERY_WARNING.value(),
                IQueryWarningBizService.class);
        return new Result<>(bizService.getGaryDefectStaticList(grayBuildNumAndTaskVO));
    }
}
