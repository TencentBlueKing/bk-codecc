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

import com.tencent.bk.codecc.task.api.UserGrayToolProjectRestResource;
import com.tencent.bk.codecc.task.service.GrayToolProjectService;
import com.tencent.bk.codecc.task.vo.GrayToolProjectReqVO;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.bk.codecc.task.vo.TriggerGrayToolVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.auth.api.OpAuthApi;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 灰度工具项目调用接口实现
 *
 * @version V2.0
 * @date 2020/12/29
 */
@Slf4j
@RestResource
public class UserGrayToolProjectRestResourceImpl implements UserGrayToolProjectRestResource {

    @Autowired
    private GrayToolProjectService grayToolProjectService;

    @Autowired
    private AuthExPermissionApi authExPermissionApi;

    @Autowired
    private OpAuthApi opAuthApi;

    @Override
    public Result<Boolean> register(String userId, GrayToolProjectVO grayToolProjectVO) {
        // 判断是否为OP管理员
        if (!opAuthApi.isOpAdminMember(userId)) {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER, new String[]{"op admin member"});
        }
        return new Result<>(grayToolProjectService.save(userId,grayToolProjectVO));
    }

    @Override
    public Result<Page<GrayToolProjectVO>> queryGrayToolProjectList(GrayToolProjectReqVO grayToolProjectReqVO,
                                           Integer pageNum, Integer pageSize, String sortField, String sortType) {
        Page<GrayToolProjectVO> grayToolProjectVOList = grayToolProjectService.queryGrayToolProjectList(
                                            grayToolProjectReqVO, pageNum, pageSize, sortField, sortType);
        return new Result<>(grayToolProjectVOList);
    }

    @Override
    public Result<Boolean> updateGrayToolProject(String userId, GrayToolProjectVO grayToolProjectVO) {
        // 判断是否为OP管理员
        if (!opAuthApi.isOpAdminMember(userId)) {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER, new String[]{"op admin member"});
        }
        return new Result<>(grayToolProjectService.updateGrayInfo(userId,grayToolProjectVO));
    }

//    @Override
//    public Result<GrayToolProjectVO> findByProjectId(String projectId) {
//        return new Result<>(grayToolProjectService.findGrayInfoByProjectId(projectId));
//    }

    @Override
    public Result<Boolean> createGrayTaskPool(String toolName, String langCode, String stage, String user)
    {
        grayToolProjectService.selectGrayTaskPool(toolName, langCode, stage, user);
        return new Result<>(true);
    }

    @Override
    public Result<TriggerGrayToolVO> triggerGrayTaskPool(String toolName, String taskNum, String langCode)
    {
        return new Result<>(grayToolProjectService.triggerGrayToolTasks(toolName, taskNum, langCode));
    }

//    @Override
//    public Result<GrayToolProjectVO> findGrayToolProjInfoByToolName(String toolName)
//    {
//        return new Result<>(grayToolProjectService.findGrayInfoByProjectId(toolName));
//    }
}
