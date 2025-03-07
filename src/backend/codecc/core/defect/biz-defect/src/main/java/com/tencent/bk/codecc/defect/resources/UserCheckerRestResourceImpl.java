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

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.codecc.defect.api.UserCheckerRestResource;
import com.tencent.bk.codecc.defect.service.CheckerImportService;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.ICheckerSetBizService;
import com.tencent.bk.codecc.defect.service.IConfigCheckerPkgBizService;
import com.tencent.bk.codecc.defect.vo.CheckerCommonCountVO;
import com.tencent.bk.codecc.defect.vo.CheckerDetailListQueryReqVO;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.CheckerImportVO;
import com.tencent.bk.codecc.defect.vo.CheckerListQueryReq;
import com.tencent.bk.codecc.defect.vo.CheckerManagementPermissionReqVO;
import com.tencent.bk.codecc.defect.vo.ConfigCheckersPkgReqVO;
import com.tencent.bk.codecc.defect.vo.GetCheckerListRspVO;
import com.tencent.bk.codecc.defect.vo.checkerset.AddCheckerSet2TaskReqVO;
import com.tencent.bk.codecc.defect.vo.checkerset.CheckerSetDifferenceVO;
import com.tencent.bk.codecc.defect.vo.checkerset.UpdateCheckerSetReqVO;
import com.tencent.bk.codecc.defect.vo.checkerset.UserCreatedCheckerSetsVO;
import com.tencent.bk.codecc.defect.vo.enums.CheckerListSortType;
import com.tencent.bk.codecc.defect.vo.enums.CheckerPermissionType;
import com.tencent.devops.common.api.annotation.I18NResponse;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.auth.api.OpAuthApi;
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.audit.ActionIds;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.security.AuthMethod;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

/**
 * 配置规则包服务实现
 *
 * @version V1.0
 * @date 2019/6/5
 */
@Slf4j
@RestResource
public class UserCheckerRestResourceImpl implements UserCheckerRestResource {
    @Autowired
    private CheckerImportService checkerImportService;

    @Autowired
    private IConfigCheckerPkgBizService configCheckerPkgBizService;

    @Autowired
    private ICheckerSetBizService checkerSetBizService;

    @Autowired
    private CheckerService checkerService;

    @Autowired
    private OpAuthApi opAuthApi;

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public Result<GetCheckerListRspVO> checkerPkg(Long taskId, String toolName) {
        return new Result<>(configCheckerPkgBizService.getConfigCheckerPkg(taskId, toolName));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public Result<Boolean> configCheckerPkg(String user, Long taskId, String toolName,
            ConfigCheckersPkgReqVO packageVo) {
        return new Result<>(configCheckerPkgBizService.configCheckerPkg(taskId, toolName, packageVo, user));
    }

    @Override
    @AuthMethod(permission = {CodeCCAuthAction.TASK_MANAGE})
    public Result<Boolean> updateCheckerSet(Long taskId, String toolName, String checkerSetId,
            UpdateCheckerSetReqVO updateCheckerSetReqVO, String user,
            String projectId) {
        return new Result<>(
                checkerSetBizService.updateCheckerSet(taskId, toolName, checkerSetId, updateCheckerSetReqVO, user,
                        projectId));
    }

    @Override
    public Result<Boolean> addCheckerSet2Task(String user, Long taskId,
            AddCheckerSet2TaskReqVO addCheckerSet2TaskReqVO) {
        addCheckerSet2TaskReqVO.setNeedUpdatePipeline(true);
        return new Result<>(checkerSetBizService.addCheckerSet2Task(user, taskId, addCheckerSet2TaskReqVO));
    }

    @Override
    public Result<UserCreatedCheckerSetsVO> getUserCreatedCheckerSet(String toolName, String user, String projectId) {
        return new Result<>(checkerSetBizService.getUserCreatedCheckerSet(toolName, user, projectId));
    }

    @Override
    public Result<CheckerSetDifferenceVO> getCheckerSetVersionDifference(String user, String projectId, String toolName,
            String checkerSetId,
            CheckerSetDifferenceVO checkerSetDifferenceVO) {
        return new Result<>(checkerSetBizService.getCheckerSetVersionDifference(user, projectId, toolName, checkerSetId,
                checkerSetDifferenceVO));
    }

    @Override
    public Result<Boolean> updateCheckerConfigParam(Long taskId, String toolName, String checkerName, String paramValue,
            String user) {
        return new Result<>(checkerService.updateCheckerConfigParam(taskId, toolName, checkerName, paramValue, user));
    }

    @Override
    @I18NResponse
    public Result<CheckerDetailVO> queryCheckerDetail(String toolName, String checkerKey) {
        return new Result<>(checkerService.queryCheckerDetail(toolName, checkerKey));
    }

    @Override
    @I18NResponse
    public Result<List<CheckerDetailVO>> queryCheckerDetailList(
            CheckerListQueryReq checkerListQueryReq,
            String userId,
            String projectId,
            Integer pageNum,
            Integer pageSize,
            Sort.Direction sortType,
            CheckerListSortType sortField
    ) {
        // isOp为true可查看项目之外的自定义规则，需要判断是否为OP管理员
        if (checkerListQueryReq.getIsOp() && !opAuthApi.isOpAdminMember(userId)) {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER, new String[]{"op admin member"});
        }
        return new Result<>(checkerService.queryCheckerDetailList(checkerListQueryReq, projectId, pageNum,
                pageSize, sortType, sortField));
    }


    @Override
    public Result<List<CheckerCommonCountVO>> queryCheckerCountList(CheckerListQueryReq checkerListQueryReq,
            String userId, String projectId) {
        // isOp为true可查看项目之外的自定义规则，需要判断是否为OP管理员
        if (checkerListQueryReq.getIsOp() && !opAuthApi.isOpAdminMember(userId)) {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER, new String[]{"op admin member"});
        }
        return new Result<>(checkerService.queryCheckerCountListNew(checkerListQueryReq, projectId));
    }

    @Override
    public Result<List<CheckerDetailVO>> queryCheckerByTool(String toolName) {
        return new Result<>(checkerService.queryCheckerByTool(toolName));
    }

    @Override
    public Result<Boolean> updateCheckerByCheckerKey(CheckerDetailVO checkerDetailVO, String userId) {
        // 判断是否为OP管理员
        if (!opAuthApi.isOpAdminMember(userId)) {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER, new String[]{"op admin member"});
        }
        return new Result<>(checkerService.updateCheckerByCheckerKey(checkerDetailVO, userId));
    }

    @AuditEntry(actionId = ActionIds.CREATE_REGEX_RULE)
    @Override
    public Result<Boolean> customCheckerImport(String userName, String projectId, CheckerImportVO checkerImportVO) {
        // 校验输入参数
        if (CollectionUtils.isEmpty(checkerImportVO.getCheckerDetailVOList())) {
            String errMsg = "输入规则不能为空";
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{errMsg}, null);
        }

        if (StringUtils.isBlank(checkerImportVO.getToolName())) {
            String errMsg = "输入工具名不能为空";
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{errMsg}, null);
        }

        return new Result<>(checkerImportService.customCheckerImport(userName, projectId, checkerImportVO));
    }

    @AuditEntry(actionId = ActionIds.UPDATE_REGEX_RULE)
    @Override
    public Result<Boolean> updateCustomCheckerByCheckerKey(CheckerDetailVO checkerDetailVO,
                                                           String projectId, String userId) {
        return new Result<>(checkerService.updateCustomCheckerByCheckerKey(checkerDetailVO, projectId, userId));
    }

    /**
     * 根据checkerKey和ToolName删除用户自定义规则
     *
     * @deprecated 由于规则集的更新采用累加版本方式，删除规则对规则集的处理逻辑将导致原有设计遭到破坏，目前暂时停用删除功能
     */
    @Deprecated
    @Override
    public Result<Boolean> deleteCustomCheckerByCheckerKey(CheckerDetailVO checkerDetailVO,
                                                           String projectId, String userId) {
        return new Result<>(checkerService.deleteCustomCheckerByCheckerKey(checkerDetailVO, projectId, userId));
    }


    @Override
    public Result<List<CheckerDetailVO>> queryCheckerDetailListForPreCI(
            CheckerDetailListQueryReqVO checkerListQueryReq) {
        return new Result<>(checkerService.queryCheckerDetailList(checkerListQueryReq));
    }

    @Override
    public Result<List<CheckerPermissionType>> getCheckerManagementPermission(
        CheckerManagementPermissionReqVO authManagementPermissionReqVO) {
        return new Result<>(checkerService.getCheckerManagementPermission(authManagementPermissionReqVO));
    }

}
