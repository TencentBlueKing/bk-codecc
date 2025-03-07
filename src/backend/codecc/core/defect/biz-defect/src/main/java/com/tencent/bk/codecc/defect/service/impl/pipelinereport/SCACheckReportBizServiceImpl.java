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

package com.tencent.bk.codecc.defect.service.impl.pipelinereport;

import com.tencent.bk.codecc.defect.dao.defect.mongorepository.statistic.SCAStatisticRepository;
import com.tencent.bk.codecc.defect.model.pipelinereport.SCASnapShotEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.ToolSnapShotEntity;
import com.tencent.bk.codecc.defect.model.statistic.SCAStatisticEntity;
import com.tencent.bk.codecc.defect.service.ICheckReportBizService;
import com.tencent.devops.common.service.ToolMetaCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 重复率分析结果报告生成服务代码
 *
 */
@Service("SCACheckerReportBizService")
@Slf4j
public class SCACheckReportBizServiceImpl implements ICheckReportBizService {

    @Autowired
    private SCAStatisticRepository scaStatisticRepository;

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    @Value("${bkci.public.url:#{null}}")
    private String devopsHost;

    @Value("${bkci.public.schemes:http}")
    private String devopsSchemes;

    @Override
    public ToolSnapShotEntity getReport(long taskId, String projectId, String toolName, String buildId) {
        SCASnapShotEntity scaSnapShotEntity = new SCASnapShotEntity();
        handleToolBaseInfo(scaSnapShotEntity, taskId, toolName, projectId);
        SCAStatisticEntity scaStatistic = scaStatisticRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId,
                toolName, buildId);
        if (scaStatistic == null) {
            return scaSnapShotEntity;
        }
        scaSnapShotEntity.setPackageCount(scaStatistic.getPackageCount());
        scaSnapShotEntity.setHighPackageCount(scaStatistic.getHighPackageCount());
        scaSnapShotEntity.setMediumPackageCount(scaStatistic.getMediumPackageCount());
        scaSnapShotEntity.setLowPackageCount(scaStatistic.getLowPackageCount());
        scaSnapShotEntity.setUnknownPackageCount(scaStatistic.getUnknownPackageCount());
        scaSnapShotEntity.setNewVulCount(scaStatistic.getNewVulCount());
        scaSnapShotEntity.setNewHighVulCount(scaStatistic.getNewHighVulCount());
        scaSnapShotEntity.setNewMediumVulCount(scaStatistic.getNewMediumVulCount());
        scaSnapShotEntity.setNewLowVulCount(scaStatistic.getNewLowVulCount());
        scaSnapShotEntity.setNewUnknownVulCount(scaStatistic.getNewUnknownVulCount());
        scaSnapShotEntity.setLicenseCount(scaStatistic.getLicenseCount());
        scaSnapShotEntity.setHighLicenseCount(scaStatistic.getHighLicenseCount());
        scaSnapShotEntity.setMediumLicenseCount(scaStatistic.getMediumLicenseCount());
        scaSnapShotEntity.setLowLicenseCount(scaStatistic.getLowLicenseCount());
        scaSnapShotEntity.setUnknownLicenseCount(scaStatistic.getUnknownLicenseCount());

        return scaSnapShotEntity;
    }


    private void handleToolBaseInfo(SCASnapShotEntity scaSnapShotEntity, long taskId, String toolName,
            String projectId) {
        //获取工具信息
        scaSnapShotEntity.setToolNameCn(toolMetaCacheService.getToolDisplayName(toolName));
        scaSnapShotEntity.setToolNameEn(toolName);
        if (StringUtils.isNotEmpty(projectId)) {
            String defectDetailUrl = String.format(
                    "%s://%s/console/codecc/%s/task/%d/defect/sca/pkg/list",
                    devopsSchemes, devopsHost, projectId, taskId
            );

            String defectReportUrl = String.format(
                    "%s://%s/console/codecc/%s/task/%d/defect/sca/pkg/list",
                    devopsSchemes, devopsHost, projectId, taskId
            );

            scaSnapShotEntity.setDefectDetailUrl(defectDetailUrl);
            scaSnapShotEntity.setDefectReportUrl(defectReportUrl);
        }
    }
}
