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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to
 * use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.service.GrayToolProjectService;
import com.tencent.bk.codecc.task.vo.GrayToolProjectReqVO;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.bk.codecc.task.vo.GrayToolReportVO;
import com.tencent.bk.codecc.task.vo.TriggerGrayToolVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.util.UUIDUtil;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 灰度工具项目服务代码
 *
 * @version V1.0
 * @date 2020/12/29
 */
@Slf4j
@Service
public class GrayToolProjectServiceImpl implements GrayToolProjectService {

    @Override
    public GrayToolProjectVO findGrayInfoByProjectId(String projectId) {
        GrayToolProjectVO projectVO = new GrayToolProjectVO();
        projectVO.setStatus(ToolIntegratedStatus.P.value());
        return projectVO;
    }

    @Override
    public Boolean updateGrayInfo(String userId, GrayToolProjectVO grayToolProjectVO) {
        return true;
    }

    @Override
    public Page<GrayToolProjectVO> queryGrayToolProjectList(GrayToolProjectReqVO reqVO, Integer pageNum,
            Integer pageSize, String sortField, String sortType) {
        return new Page<>(0, pageNum, pageSize, 0, Collections.emptyList());
    }

    @Override
    public Boolean save(String userId, GrayToolProjectVO grayToolProjectVO) {
        return true;
    }

    @Override
    public void selectGrayTaskPool(String toolName, String stage, String userName) {

    }

    @Override
    public Set<Long> findTaskListByToolName(String toolName) {
        return new HashSet<>();
    }

    @Override
    public TriggerGrayToolVO triggerGrayToolTasks(String toolName, String taskNum) {
        String codeccBuildId = UUIDUtil.INSTANCE.generate();
        return new TriggerGrayToolVO(codeccBuildId, Collections.emptySet());
    }

    @Override
    public GrayToolReportVO findGrayToolReportByToolNameAndBuildId(String toolName, String codeccBuildId) {
        return null;
    }

    @Override
    public List<GrayToolProjectVO> findGrayToolProjectByProjectIds(Set<String> projectIdSet) {
        return Collections.emptyList();
    }

    @Override
    public Set<String> findAllGrayProjectIdSet() {
        return Collections.emptySet();
    }
}
