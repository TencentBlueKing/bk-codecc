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

import com.tencent.bk.codecc.defect.api.BuildDefectRestResource;
import com.tencent.bk.codecc.defect.service.IQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.UploadRepositoriesService;
import com.tencent.bk.codecc.defect.vo.coderepository.UploadRepositoriesVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

/**
 * 工具侧上报告警的接口实现
 *
 * @version V1.0
 * @date 2019/5/13
 */
@RestResource
@Slf4j
public class BuildDefectRestResourceImpl implements BuildDefectRestResource {
    @Autowired
    private UploadRepositoriesService uploadRepositoriesService;
    @Autowired
    private BizServiceFactory<IQueryWarningBizService> fileAndDefectQueryFactory;

    @Override
    public Result uploadRepositories(UploadRepositoriesVO uploadRepositoriesVO) {
        log.info("upload code repositories, task id: {}, tool name: {} build id: {}",
                uploadRepositoriesVO.getTaskId(), uploadRepositoriesVO.getToolName(),
                uploadRepositoriesVO.getBuildId());
        return uploadRepositoriesService.uploadRepositories(uploadRepositoriesVO);
    }

    @Override
    public Result<CommonDefectQueryRspVO> queryDefectListWithIssue(
            String userId,
            String projectId,
            DefectQueryReqVO defectQueryReqVO,
            int pageNum,
            int pageSize,
            String sortField,
            Sort.Direction sortType
    ) {
        defectQueryReqVO.setProjectId(projectId);
        defectQueryReqVO.setUserId(userId);

        IQueryWarningBizService service = fileAndDefectQueryFactory.createBizService(
                defectQueryReqVO.getToolNameList(),
                defectQueryReqVO.getDimensionList(),
                ComConstants.BusinessType.QUERY_WARNING.value(),
                IQueryWarningBizService.class
        );

        long taskId = CollectionUtils.isEmpty(defectQueryReqVO.getTaskIdList()) ? 0L
                : defectQueryReqVO.getTaskIdList().get(0);

        return new Result<>(
                service.processQueryWarningRequest(taskId, defectQueryReqVO, pageNum, pageSize, sortField, sortType)
        );
    }
}
