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

package com.tencent.bk.codecc.defect.service.impl.pipelinereport;

import com.tencent.bk.codecc.defect.dao.defect.mongorepository.LintStatisticRepository;
import com.tencent.bk.codecc.defect.model.statistic.LintStatisticEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.LintSnapShotEntity;
import com.tencent.bk.codecc.defect.model.pipelinereport.ToolSnapShotEntity;
import com.tencent.bk.codecc.defect.service.ICheckReportBizService;
import com.tencent.devops.common.service.ToolMetaCacheService;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * lint类工具组装分析产出物报告逻辑
 *
 * @version V1.0
 * @date 2019/6/28
 */
@Service("LINTCheckerReportBizService")
@Slf4j
public class LINTCheckReportBizServiceImpl implements ICheckReportBizService {

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    @Autowired
    private LintStatisticRepository lintStatisticRepository;

    @Value("${bkci.public.url:#{null}}")
    private String devopsHost;

    @Value("${bkci.public.schemes:http}")
    private String devopsSchemes;

    @Override
    public ToolSnapShotEntity getReport(long taskId, String projectId, String toolName, String buildId) {
        LintSnapShotEntity lintSnapShotEntity = new LintSnapShotEntity();

        handleToolBaseInfo(lintSnapShotEntity, taskId, toolName, projectId, buildId);

        // 最近一次分析概要信息（重试时需获取最后一次为准）
        LintStatisticEntity lintStatisticEntity =
                lintStatisticRepository.findFirstByTaskIdAndToolNameAndBuildIdOrderByTimeDesc(taskId, toolName,
                        buildId);

        if (lintStatisticEntity == null) {
            log.info("no analysis result found! task id: {}, tool name: {}", taskId, toolName);
            return lintSnapShotEntity;
        }
        lintSnapShotEntity.setNewFileTotalDefectCount(lintStatisticEntity.getDefectCount());
        lintSnapShotEntity.setNewFileChangedDefectCount(lintStatisticEntity.getDefectChange());
        lintSnapShotEntity.setNewFileTotalCount(lintStatisticEntity.getFileCount());
        lintSnapShotEntity.setNewFileChangedCount(lintStatisticEntity.getFileChange());

        //统计不同等级告警数量
        lintSnapShotEntity.setTotalNewSerious(lintStatisticEntity.getTotalNewSerious());
        lintSnapShotEntity.setTotalNewNormal(lintStatisticEntity.getTotalNewNormal());
        lintSnapShotEntity.setTotalNewPrompt(lintStatisticEntity.getTotalNewPrompt());

        //统计不同等级告警总量
        lintSnapShotEntity.setTotalSerious(lintStatisticEntity.getTotalSerious());
        lintSnapShotEntity.setTotalNormal(lintStatisticEntity.getTotalNormal());
        lintSnapShotEntity.setTotalPrompt(lintStatisticEntity.getTotalPrompt());

        //待修复告警作者
        lintSnapShotEntity.setAuthorList(lintStatisticEntity.getAuthorStatistic());
        return lintSnapShotEntity;
    }

    private void handleToolBaseInfo(LintSnapShotEntity lintSnapShotEntity, long taskId, String toolName,
            String projectId, String buildId) {
        //获取工具信息
        lintSnapShotEntity.setToolNameCn(toolMetaCacheService.getToolDisplayName(toolName));
        lintSnapShotEntity.setToolNameEn(toolName);
        if (StringUtils.isNotEmpty(projectId)) {
            String defectDetailUrl = String.format(
                    "%s://%s/console/codecc/%s/task/%d/defect/lint/%s/list?buildId=%s",
                    devopsSchemes, devopsHost, projectId, taskId, toolName.toUpperCase(Locale.ENGLISH), buildId
            );

            String defectReportUrl = String.format(
                    "%s://%s/console/codecc/%s/task/%d/defect/lint/%s/charts",
                    devopsSchemes, devopsHost, projectId, taskId, toolName.toUpperCase(Locale.ENGLISH)
            );

            lintSnapShotEntity.setDefectDetailUrl(defectDetailUrl);
            lintSnapShotEntity.setDefectReportUrl(defectReportUrl);
        }
    }
}
