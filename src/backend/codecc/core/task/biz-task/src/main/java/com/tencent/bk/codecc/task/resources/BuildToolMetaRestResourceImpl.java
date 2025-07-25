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

package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.codecc.task.api.BuildToolMetaRestResource;
import com.tencent.bk.codecc.task.service.ToolMetaService;
import com.tencent.devops.common.api.BKToolBasicInfoVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.constant.CommonMessageCode;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.audit.ActionIds;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.web.RestResource;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Locale;

/**
 * 工具元数据注册接口
 *
 * @version V2.0
 * @date 2020/8/8
 */
@RestResource
public class BuildToolMetaRestResourceImpl implements BuildToolMetaRestResource {

    @Autowired
    private ToolMetaService toolMetaService;

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    @Override
    @AuditEntry(actionId = ActionIds.REGISTER_TOOL)
    public Result<ToolMetaDetailVO> register(String userName, ToolMetaDetailVO toolMetaDetailVO) {
        return new Result<>(toolMetaService.register(userName, toolMetaDetailVO));
    }

    @Override
    public Result<List<ToolMetaDetailVO>> queryToolMetaDataList(String projectId, Long taskId) {
        return new Result<>(toolMetaService.queryToolMetaDataList(projectId, taskId));
    }

    @Override
    public Result<ToolMetaDetailVO> queryToolMetaData(String projectId, String toolName) {
        if (StringUtils.isBlank(toolName)) {
            throw new CodeCCException(CommonMessageCode.ERROR_INVALID_PARAM_);
        }
        return new Result<>(toolMetaCacheService.getToolDetailFromCache(toolName.toUpperCase(Locale.ENGLISH)));
    }

    @Override
    public Result<BKToolBasicInfoVO> getBasicInfo(String toolName) {
        return new Result<>(toolMetaService.getBKToolBasicInfo(toolName));
    }

}
