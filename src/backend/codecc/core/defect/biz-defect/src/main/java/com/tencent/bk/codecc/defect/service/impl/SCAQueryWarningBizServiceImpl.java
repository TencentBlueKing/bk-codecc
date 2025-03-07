package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.SCAQueryWarningParams;
import com.tencent.bk.codecc.defect.service.AbstractQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.ISCAQueryWarningService;
import com.tencent.bk.codecc.defect.service.impl.sca.SCAClusterDefectServiceImpl;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.DefectFileContentSegmentQueryRspVO;
import com.tencent.bk.codecc.defect.vo.QueryCheckersAndAuthorsRequest;
import com.tencent.bk.codecc.defect.vo.QueryDefectFileContentSegmentReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import com.tencent.bk.codecc.defect.vo.sca.SCADefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.sca.SCADefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.sca.SCAQueryWarningPageInitRspVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;


import com.tencent.devops.common.util.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


@Service("SCAQueryWarningBizService")
@Slf4j
public class SCAQueryWarningBizServiceImpl extends AbstractQueryWarningBizService {

    @Autowired
    private BizServiceFactory<ISCAQueryWarningService> scaQueryWarningBizServiceFactory;

    @Autowired
    private SCALicenseQueryWarningBizServiceImpl scaLicenseQueryWarningBizServiceImpl;

    @Autowired
    private SCAVulnerabilityQueryWarningBizServiceImpl scaVulnerabilityQueryWarningBizServiceImpl;

    @Autowired
    private SCAPackageQueryWarningBizServiceImpl scaPackageQueryWarningBizServiceImpl;

    @Autowired
    private SCAClusterDefectServiceImpl scaClusterDefectServiceImpl;

    @Override
    public int getSubmitStepNum() {
        return ComConstants.Step4MutliTool.COMMIT.value();
    }

    @Override
    public CommonDefectQueryRspVO processQueryWarningRequest(
            long taskId,
            DefectQueryReqVO queryWarningReq,
            int pageNum,
            int pageSize,
            String sortField,
            Sort.Direction sortType
    ) {
        return processQueryWarningRequestCore(
                queryWarningReq,
                pageNum,
                pageSize,
                sortField,
                sortType
        );
    }

    /**
     * 查询代码成分告警列表
     *
     * @param queryWarningReq
     * @param pageNum
     * @param pageSize
     * @param sortField
     * @param sortType
     * @return
     */
    protected CommonDefectQueryRspVO processQueryWarningRequestCore(
            DefectQueryReqVO queryWarningReq,
            int pageNum,
            int pageSize,
            String sortField,
            Sort.Direction sortType
    ) {
        // 请求类转换
        if (queryWarningReq == null) {
            return new CommonDefectQueryRspVO();
        }
        if (!(queryWarningReq instanceof SCADefectQueryReqVO)) {
            log.error("input param class type incorrect!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                    new String[]{"queryWarningReq"}, null);
        }
        SCADefectQueryReqVO request = (SCADefectQueryReqVO) queryWarningReq;

        // 获取查询的SCA维度
        List<String> scaDimensionList = request.getScaDimensionList();
        if (CollectionUtils.isEmpty(scaDimensionList)) {
            log.info("sca dimension empty, task id list: {}", request.getTaskIdList());
            return new CommonDefectQueryRspVO();
        }
        String scaDimension = scaDimensionList.get(0);
        log.info("SCA {} list query, task id list: {}",
                scaDimension, request.getTaskIdList());

        // 处理公共请求参数：taskIdList、toolNameList
        SCAQueryWarningParams scaQueryWarningParams = processQueryWarningRequestParams(request);
        if (scaQueryWarningParams == null) {
            return new CommonDefectQueryRspVO();
        }

        // 根据SCA维度,查询对应维度告警列表
        ISCAQueryWarningService service = scaQueryWarningBizServiceFactory.createBizService(
                queryWarningReq.getToolNameList(),
                queryWarningReq.getDimensionList(),
                ComConstants.BizServiceFlag.CORE,
                StringUtils.capitalize(scaDimension.toLowerCase(Locale.ENGLISH))
                        + ComConstants.BusinessType.QUERY_WARNING.value(),
                ISCAQueryWarningService.class
        );

        return service.processQueryWarningRequest(
                scaQueryWarningParams,
                pageNum,
                pageSize,
                sortField,
                sortType
        );
    }

    /**
     * 查询代码成分详情
     *
     * @param projectId
     * @param taskId
     * @param userId
     * @param queryWarningDetailReq
     * @param sortField
     * @param sortType
     * @return
     */
    @Override
    public CommonDefectDetailQueryRspVO processQueryWarningDetailRequest(
            String projectId,
            Long taskId,
            String userId,
            CommonDefectDetailQueryReqVO queryWarningDetailReq,
            String sortField,
            Sort.Direction sortType
    ) {
        if (!(queryWarningDetailReq instanceof SCADefectDetailQueryReqVO)) {
            log.error("input param class type incorrect!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                    new String[]{"queryWarningDetailReq"}, null);
        }
        SCADefectDetailQueryReqVO requestVO = (SCADefectDetailQueryReqVO) queryWarningDetailReq;

        List<String> scaDimensionList = requestVO.getScaDimensionList();
        if (CollectionUtils.isEmpty(scaDimensionList)) {
            return new CommonDefectDetailQueryRspVO();
        }
        String scaDimension = scaDimensionList.get(0);

        // 根据SCA维度,查询对应维度告警详情
        ISCAQueryWarningService service = scaQueryWarningBizServiceFactory.createBizService(
                Collections.emptyList(),
                Collections.singletonList(ComConstants.ToolType.SCA.name()),
                ComConstants.BizServiceFlag.CORE,
                StringUtils.capitalize(scaDimension.toLowerCase(Locale.ENGLISH))
                        + ComConstants.BusinessType.QUERY_WARNING.value(),
                ISCAQueryWarningService.class
        );
        return service.processQueryWarningDetailRequest(requestVO);
    }

    @Override
    public CommonDefectDetailQueryRspVO processQueryDefectDetailWithoutFileContent(Long taskId, String userId,
            CommonDefectDetailQueryReqVO queryWarningDetailReq, String sortField, Sort.Direction sortType) {
        return null;
    }

    @Override
    public DefectFileContentSegmentQueryRspVO processQueryDefectFileContentSegment(String projectId, String userId,
            QueryDefectFileContentSegmentReqVO request) {
        return null;
    }

    /**
     * 查询告警页面初始化的处理人数据，筛选条件
     *      taskIdList
     *      toolNameList
     *      dimensionList
     *      buildId
     *      statusList
     * @param userId
     * @param projectId
     * @param request
     * @return
     */
    @Override
    public QueryWarningPageInitRspVO processQueryWarningPageInitRequest(
            String userId,
            String projectId,
            QueryCheckersAndAuthorsRequest request
    ) {
        List<String> statusList = request.getStatusList();
        Set<String> statusSet = CollectionUtils.isEmpty(statusList) ? Sets.newHashSet() : Sets.newHashSet(statusList);
        List<Long> taskIdList = request.getTaskIdList();
        String buildId = request.getBuildId();
        String scaDimension = request.getScaDimension();

        // 1.校验处理SCA公共参数：taskId和buildId
        if (CollectionUtils.isNotEmpty(taskIdList)
                && taskIdList.size() == 1
                && isInvalidBuildId(taskIdList.get(0), buildId)
        ) {
            log.error("Invalid buildId: {}, taskIdList: {}", buildId, taskIdList);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                    new String[]{"author init"}, null);
        }
        taskIdList = ParamUtils.allTaskByProjectIdIfEmpty(
                request.getTaskIdList(),
                projectId,
                userId
        );

        // 2.处理SCA查询公共筛选条件：toolNameList
        Map<Long, List<String>> taskToolMap = ParamUtils.getTaskToolMap(
                request.getToolNameList(),
                Lists.newArrayList(ComConstants.ToolType.SCA.name()),
                taskIdList,
                buildId
        );
        taskIdList = Lists.newArrayList(taskToolMap.keySet());
        request.setTaskIdList(taskIdList);

        // 3.构建SCA查询的过滤参数类：SCAQueryWarningParams
        SCADefectQueryReqVO scaDefectQueryReqVO = new SCADefectQueryReqVO();
        BeanUtils.copyProperties(request, scaDefectQueryReqVO);
        scaDefectQueryReqVO.setScaDimensionList(Collections.singletonList(request.getScaDimension()));
        scaDefectQueryReqVO.setStatus(statusSet);

        SCAQueryWarningParams scaQueryWarningParams = new SCAQueryWarningParams();
        scaQueryWarningParams.setTaskToolMap(taskToolMap);
        scaQueryWarningParams.setScaDefectQueryReqVO(scaDefectQueryReqVO);

        // 4.根据SCA维度,查询对应维度告警处理人列表
        ISCAQueryWarningService service = scaQueryWarningBizServiceFactory.createBizService(
                Collections.emptyList(),
                Collections.singletonList(ComConstants.ToolType.SCA.name()),
                ComConstants.BizServiceFlag.CORE,
                StringUtils.capitalize(scaDimension.toLowerCase(Locale.ENGLISH))
                        + ComConstants.BusinessType.QUERY_WARNING.value(),
                ISCAQueryWarningService.class
        );

        return service.processQueryAuthorsRequest(scaQueryWarningParams);

    }

    @Override
    public Object pageInit(String projectId, DefectQueryReqVO requestVO) {
        // 请求类型转换
        if (!(requestVO instanceof SCADefectQueryReqVO)) {
            log.error("input param class type incorrect!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                    new String[]{"queryWarningReq"}, null);
        }
        SCADefectQueryReqVO scaRequestVO = (SCADefectQueryReqVO) requestVO;
        if (CollectionUtils.isEmpty(scaRequestVO.getTaskIdList())) {
            log.error("taskIdList is empty!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                    new String[]{"pageInit"}, null);
        }
        Long taskId = scaRequestVO.getTaskIdList().get(0);

        // 统计类型为空，则默认统计该扫描任务最新一次的 组件、漏洞、许可证 数量
        if (StringUtils.isBlank(scaRequestVO.getStatisticType())) {
            return scaClusterDefectServiceImpl.getLatestClusterStatistic(taskId);
        }

        // 处理公共请求参数：taskIdList、toolNameList
        SCAQueryWarningParams scaQueryWarningParams = processQueryWarningRequestParams(scaRequestVO);
        if (scaQueryWarningParams == null) {
            return new SCAQueryWarningPageInitRspVO();
        }

        List<String> scaDimensionList = scaRequestVO.getScaDimensionList();
        if (CollectionUtils.isEmpty(scaDimensionList)) {
            log.info("scaDimensionList empty, task id: {}", taskId);
            return new SCAQueryWarningPageInitRspVO();
        }
        String scaDimension = scaDimensionList.get(0);
        log.info("SCA {} page init, task id list: {}",
                scaDimension,
                requestVO.getTaskIdList()
        );

        // 根据SCA维度,查询对应维度告警详情
        ISCAQueryWarningService service = scaQueryWarningBizServiceFactory.createBizService(
                Collections.emptyList(),
                Collections.singletonList(ComConstants.ToolType.SCA.name()),
                ComConstants.BizServiceFlag.CORE,
                StringUtils.capitalize(scaDimension.toLowerCase(Locale.ENGLISH))
                        + ComConstants.BusinessType.QUERY_WARNING.value(),
                ISCAQueryWarningService.class
        );

        return service.pageInit(scaQueryWarningParams);
    }

    /**
     * 处理告警列表查询的公共请求参数
     *
     * @param request
     * @return
     */
    protected SCAQueryWarningParams processQueryWarningRequestParams(
            SCADefectQueryReqVO request
    ) {
        // 校验查询筛选条件：taskIdList
        List<Long> taskIdList = request.getTaskIdList();
        String buildId = request.getBuildId();

        if (CollectionUtils.isNotEmpty(taskIdList)
                && taskIdList.size() == 1
                && isInvalidBuildId(taskIdList.get(0), buildId)) {
            log.info("Invalid buildId: {}, taskIdList: {}", buildId, taskIdList);
            return null;
        }
        taskIdList = ParamUtils.allTaskByProjectIdIfEmpty(
                request.getTaskIdList(),
                request.getProjectId(),
                request.getUserId()
        );

        // 处理查询筛选条件：toolNameList
        Map<Long, List<String>> taskToolMap = ParamUtils.getTaskToolMap(
                request.getToolNameList(),
                Lists.newArrayList(ComConstants.ToolType.SCA.name()),
                taskIdList,
                buildId
        );
        taskIdList = Lists.newArrayList(taskToolMap.keySet());
        request.setTaskIdList(taskIdList);

        if (MapUtils.isEmpty(taskToolMap)) {
            log.info("Get task tool map empty, task id list: {}, build id: {}, toolNameList: {}",
                    taskIdList, buildId, request.getToolNameList());
            return null;
        }
        log.info("SCA {} list query, task tool map: {}",
                request.getScaDimensionList(),
                JsonUtil.INSTANCE.toJson(taskToolMap)
        );

        // todo：处理filedMap

        SCAQueryWarningParams scaQueryWarningParams = new SCAQueryWarningParams();
        scaQueryWarningParams.setTaskToolMap(taskToolMap);
        scaQueryWarningParams.setScaDefectQueryReqVO(request);
        return scaQueryWarningParams;
    }

    @NotNull
    protected Map<String, Boolean> getDefectBaseFieldMap() {
        Map<String, Boolean> filedMap = new HashMap<>();
        filedMap.put("_id", true);
        filedMap.put("id", true);
        filedMap.put("file_name", true);
        filedMap.put("line_num", true);
        filedMap.put("file_path", true);
        filedMap.put("rel_path", true);
        filedMap.put("checker", true);
        filedMap.put("message", true);
        filedMap.put("author", true);
        filedMap.put("severity", true);
        filedMap.put("line_update_time", true);
        filedMap.put("create_time", true);
        filedMap.put("create_build_number", true);
        filedMap.put("status", true);
        filedMap.put("mark", true);
        filedMap.put("mark_time", true);
        filedMap.put("mark_but_no_fixed", true);
        filedMap.put("fixed_time", true);
        filedMap.put("ignore_time", true);
        filedMap.put("tool_name", true);
        filedMap.put("ignore_comment_defect", true);
        filedMap.put("task_id", true);
        filedMap.put("ignore_reason_type", true);
        filedMap.put("ignore_reason", true);
        filedMap.put("ignore_author", true);
        // 需求(120433908)需要展示分支信息
        filedMap.put("url", true);
        filedMap.put("branch", true);
        filedMap.put("ignore_approval_id", true);
        filedMap.put("ignore_approval_status", true);
        return filedMap;
    }


}
