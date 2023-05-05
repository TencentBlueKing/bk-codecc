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

package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceReportDefectRestResource;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.mongorepository.TaskLogRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.model.TaskLogEntity;
import com.tencent.bk.codecc.defect.model.TaskLogEntity.TaskUnit;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.service.CommonDefectMigrationService;
import com.tencent.bk.codecc.defect.service.IUpdateDefectBizService;
import com.tencent.bk.codecc.defect.service.impl.CommonQueryWarningBizServiceImpl;
import com.tencent.bk.codecc.defect.vo.UpdateDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadDefectVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import com.tencent.devops.common.web.RestResource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.StringUtils;

/**
 * 工具侧上报告警的接口实现
 *
 * @version V1.0
 * @date 2019/5/13
 */
@RestResource
@Slf4j
public class ServiceReportDefectRestResourceImpl implements ServiceReportDefectRestResource {

    @Autowired
    private BizServiceFactory<IBizService> bizServiceFactory;
    @Autowired
    private IUpdateDefectBizService updateDefectBizService;
    @Qualifier("CommonQueryWarningBizService")
    @Autowired
    private CommonQueryWarningBizServiceImpl commonQueryWarningBizService;
    @Autowired
    private CommonDefectMigrationService commonDefectMigrationService;
    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;
    @Autowired
    private TaskLogRepository taskLogRepository;
    @Autowired
    private ToolBuildInfoRepository toolBuildInfoRepository;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private BuildRepository buildRepository;

    @Override
    public Result<Set<Long>> queryIds(long taskId, String toolName, Boolean migrationSuccessful) {
        if (Boolean.TRUE.equals(migrationSuccessful)) {
            Set<Long> idSet = lintDefectV2Repository.findIdsByTaskIdAndToolName(taskId, toolName)
                    .stream()
                    .filter(x -> !StringUtils.isEmpty(x.getId()))
                    .map(x -> Long.valueOf(x.getId()))
                    .collect(Collectors.toSet());

            return new Result<>(idSet);
        }

        return new Result<>(commonQueryWarningBizService.queryIds(taskId, toolName));
    }

    @Override
    public Result updateDefectStatus(UpdateDefectVO updateDefectVO) {
        updateDefectBizService.updateDefectStatus(updateDefectVO);
        return new Result(CommonMessageCode.SUCCESS, "update defectStatus success.");
    }

    @Override
    public Result reportDefects(UploadDefectVO uploadDefectVO) {
        log.info("report defects, taskId:{}, toolName:{}, buildId:{}", uploadDefectVO.getTaskId(),
                uploadDefectVO.getToolName(), uploadDefectVO.getBuildId());

        IBizService uploadDefectService = bizServiceFactory.createBizService(uploadDefectVO.getToolName(),
                ComConstants.BusinessType.UPLOAD_DEFECT.value(), IBizService.class);
        return uploadDefectService.processBiz(uploadDefectVO);
    }

    @Override
    public Result updateDefects(UpdateDefectVO updateDefectVO) {
        updateDefectBizService.updateDefects(updateDefectVO);
        return new Result(CommonMessageCode.SUCCESS, "update defectDetail success.");
    }
}
