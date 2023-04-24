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

package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.CountDefectFileRequest;
import com.tencent.bk.codecc.defect.vo.GetFileContentSegmentReqVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectIdVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectPageVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectRspVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;

/**
 * 告警查询的业务接口
 *
 * @version V1.0
 * @date 2019/5/8
 */
public interface IQueryWarningBizService {
    /**
     * 处理告警查询请求
     *
     * @param queryWarningReq
     * @return
     */
    CommonDefectQueryRspVO processQueryWarningRequest(
            long taskId, DefectQueryReqVO queryWarningReq,
            int pageNum, int pageSize, String sortField, Sort.Direction sortType
    );


    /**
     * 多工具告警详情查询
     *
     * @param queryWarningDetailReq
     * @return
     */
    CommonDefectDetailQueryRspVO processQueryWarningDetailRequest(Long taskId, String userId,
            CommonDefectDetailQueryReqVO queryWarningDetailReq,
            String sortField, Sort.Direction sortType);


    /**
     * 查询某项目的所有的缺陷类型，用于初始化多工具告警管理页面的缺陷类型下拉列表控件
     *
     * @param taskIdList
     * @param toolNameList
     * @param statusSet
     * @return
     */
    QueryWarningPageInitRspVO processQueryWarningPageInitRequest(
            String userId,
            List<Long> taskIdList,
            List<String> toolNameList,
            List<String> dimensionList,
            Set<String> statusSet,
            String checkerSet,
            String buildId,
            String projectId,
            boolean isMultiTaskQuery
    );

    /**
     * 告警管理页面初始化
     *
     * @param projectId
     * @param defectQueryReqVO
     * @return
     */
    Object pageInit(String projectId, DefectQueryReqVO defectQueryReqVO);

    /**
     * 获取文件内容片段接口
     *
     * @param taskId
     * @param reqModel
     * @return
     */
    CommonDefectDetailQueryRspVO processGetFileContentSegmentRequest(long taskId, String userId,
            GetFileContentSegmentReqVO reqModel);

    /**
     * 根据前端传入的条件过滤告警
     *
     * @param taskId
     * @param defectList
     * @param queryCondObj
     * @param defectQueryRspVO
     * @return
     */
    Set<String> filterDefectByCondition(long taskId, List<?> defectList, Set<String> allChecker,
            DefectQueryReqVO queryCondObj, CommonDefectQueryRspVO defectQueryRspVO,
            List<String> toolNameSet);

    /**
     * 按组织架构查询任务告警
     *
     * @param deptTaskDefectReqVO 请求体
     * @return rsp
     */
    DeptTaskDefectRspVO processDeptTaskDefectReq(DeptTaskDefectReqVO deptTaskDefectReqVO);

    /**
     * 按组织架构筛选告警信息列表
     *
     * @param deptTaskDefectReqVO 请求体
     * @return resp
     */
    ToolDefectRspVO processDeptDefectList(DeptTaskDefectReqVO deptTaskDefectReqVO, Integer pageNum, Integer pageSize,
            String sortField, Sort.Direction sortType);

    /**
     * 按前端条件查询对应工具的告警id
     *
     * @param taskId 任务id
     * @param reqVO 查询条件
     * @return 工具告警id
     */
    List<ToolDefectIdVO> queryDefectsByQueryCond(long taskId, DefectQueryReqVO reqVO);

    /**
     * 按前端条件查询对应工具的告警id分页列表
     *
     * @param taskId 任务id
     * @param reqVO 查询条件
     * @return 工具告警id
     */
    ToolDefectPageVO queryDefectsByQueryCondWithPage(long taskId, DefectQueryReqVO reqVO, Integer pageNum,
            Integer pageSize);

    /**
     * 统计待修复告警对应的文件总数
     *
     * @param request
     * @return
     */
    Long countNewDefectFile(CountDefectFileRequest request);
}
