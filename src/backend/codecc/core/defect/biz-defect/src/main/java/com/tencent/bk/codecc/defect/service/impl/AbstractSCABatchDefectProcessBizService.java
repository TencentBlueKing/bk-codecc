package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.SCAQueryWarningParams;
import com.tencent.bk.codecc.defect.service.AbstractBatchDefectProcessBizService;
import com.tencent.bk.codecc.defect.service.ISCABatchDefectProcessBizService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.sca.SCADefectQueryReqVO;
import com.tencent.codecc.common.db.CommonEntity;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.expression.utils.CollectionUtils;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.util.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SCA类工具告警批量处理抽象类
 */
@Slf4j
public abstract class AbstractSCABatchDefectProcessBizService extends AbstractBatchDefectProcessBizService {

    @Autowired
    private BizServiceFactory<ISCABatchDefectProcessBizService> scaBatchDefectProcessBizServiceFactory;

    @Override
    protected List getDefectsByQueryCondWithPage(long taskId, DefectQueryReqVO defectQueryReqVO, String startFilePath,
                                                 Long skip, Integer pageSize) {
        return getDefectsByQueryCondWithPage(taskId, defectQueryReqVO, startFilePath, pageSize);
    }

    /**
     * 根据条件分页查询SCA告警
     *
     * @param taskId
     * @param defectQueryReqVO
     * @param lastEntityId
     * @param pageSize
     * @return
     */
    protected List getDefectsByQueryCondWithPage(
            long taskId,
            DefectQueryReqVO defectQueryReqVO,
            String lastEntityId,
            Integer pageSize
    ) {
        if (!(defectQueryReqVO instanceof SCADefectQueryReqVO)) {
            log.error("internal param class type incorrect!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                    new String[]{"defectQueryReqVO"}, null);
        }
        SCADefectQueryReqVO request = (SCADefectQueryReqVO) defectQueryReqVO;

        // 校验处理公共参数：taskToolMap
        Map<Long, List<String>> taskToolMap = ParamUtils.getTaskToolMap(
                request.getToolNameList(),
                Lists.newArrayList(ComConstants.ToolType.SCA.name()),
                Lists.newArrayList(taskId),
                request.getBuildId()
        );
        request.setTaskIdList(Lists.newArrayList(taskId));

        // 构造查询筛选条件参数
        SCAQueryWarningParams scaQueryWarningParams = new SCAQueryWarningParams();
        scaQueryWarningParams.setTaskToolMap(taskToolMap);
        scaQueryWarningParams.setScaDefectQueryReqVO(request);

        if (CollectionUtils.isEmpty(request.getScaDimensionList())) {
            log.info("scaDimensionList is empty,task id:{}", taskId);
            return Collections.emptyList();
        }
        String scaDimension = request.getScaDimensionList().get(0);
        // 根据SCA维度,查询对应维度告警详情
        ISCABatchDefectProcessBizService service = scaBatchDefectProcessBizServiceFactory.createBizService(
                Collections.emptyList(),
                Collections.singletonList(ComConstants.ToolType.SCA.name()),
                ComConstants.BizServiceFlag.CORE,
                StringUtils.capitalize(scaDimension.toLowerCase(Locale.ENGLISH))
                        + ComConstants.BATCH_DEFECT_PROCESSOR_INFFIX,
                ISCABatchDefectProcessBizService.class
        );

        return service.getDefectsByQueryCondWithPage(scaQueryWarningParams, lastEntityId, pageSize);

    }

    @Override
    protected List getEffectiveDefectByDefectKeySet(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        String scaDimension = batchDefectProcessReqVO.getScaDimension();

        if (StringUtils.isBlank(scaDimension)) {
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"scaDimension"}, null);
        }

        // 根据SCA维度,查询对应维度告警详情
        ISCABatchDefectProcessBizService service = scaBatchDefectProcessBizServiceFactory.createBizService(
                Collections.emptyList(),
                Collections.singletonList(ComConstants.ToolType.SCA.name()),
                ComConstants.BizServiceFlag.CORE,
                StringUtils.capitalize(scaDimension.toLowerCase(Locale.ENGLISH))
                        + ComConstants.BATCH_DEFECT_PROCESSOR_INFFIX,
                ISCABatchDefectProcessBizService.class
        );
        return service.getEffectiveDefectByDefectKeySet(batchDefectProcessReqVO);
    }

    @Override
    protected Pair<ComConstants.BusinessType, ComConstants.ToolType> getBusinessTypeToolTypePair() {
        return null;
    }

    @Override
    protected long processDefectByPage(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        log.info("processDefectByPage start {} {} {}", batchDefectProcessReqVO.getTaskId(),
                batchDefectProcessReqVO.getToolName(), batchDefectProcessReqVO.getDimension());

        String bizType = batchDefectProcessReqVO.getBizType();
        int ignoreReasonType = batchDefectProcessReqVO.getIgnoreReasonType();
        boolean needBatchInsert = (bizType.contains(ComConstants.BusinessType.IGNORE_DEFECT.value())
                || bizType.contains(ComConstants.BusinessType.CHANGE_IGNORE_TYPE.value()))
                && ignoreReasonType == ComConstants.IgnoreReasonType.ERROR_DETECT.value();

        List pageDefectList;
        SCADefectQueryReqVO queryCondObj = getSCADefectQueryReqVO(batchDefectProcessReqVO);
        // 起始页的 lastEntityId 和 pageSize
        String lastEntityId = null;
        int pageSize = 1000;
        int processCount = 0;
        do {
            SCADefectQueryReqVO reqVO = new SCADefectQueryReqVO();
            BeanUtils.copyProperties(queryCondObj, reqVO);
            reqVO.setScaDimensionList(Collections.singletonList(batchDefectProcessReqVO.getScaDimension()));

            // 获取SCA维度告警列表
            pageDefectList = getDefectsByQueryCondWithPage(batchDefectProcessReqVO.getTaskId(), reqVO,
                    lastEntityId, pageSize);

            if (CollectionUtils.isEmpty(pageDefectList)) {
                break;
            }

            // 获取分页起始的entityId
            Optional<String> lastEntityIdOpt = getStartEntityId(pageDefectList, lastEntityId);
            lastEntityId = lastEntityIdOpt.orElse(null);

            // 批量业务处理
            doBizByPage(pageDefectList, batchDefectProcessReqVO);
            processAfterEachPageDone(pageDefectList, batchDefectProcessReqVO);
            processCount += pageDefectList.size();
        } while (pageDefectList.size() == pageSize);
        processAfterAllPageDone(batchDefectProcessReqVO);
        log.info("processDefectByPage end {} {} {} {}", batchDefectProcessReqVO.getTaskId(),
                batchDefectProcessReqVO.getToolName(), batchDefectProcessReqVO.getDimension(), processCount);
        return processCount;
    }

    protected Optional<String> getStartEntityId(
            List pageDefectList,
            String startEntityId
    ) {
        if (CollectionUtils.isEmpty(pageDefectList)) {
            return Optional.ofNullable(startEntityId);
        }

        // 获取当前页最后一条记录的entityId
        String lastEntityId = getPageLastEntityId((CommonEntity) pageDefectList.get(pageDefectList.size() - 1));

        return Optional.ofNullable(lastEntityId);
    }

    protected String getPageLastEntityId(CommonEntity defect) {
        if (defect == null) {
            return null;
        }
        return defect.getEntityId();
    }


    /**
     * 解析批量处理请求中的告警查询条件
     *
     * @param batchDefectProcessReqVO
     * @return
     */
    private SCADefectQueryReqVO getSCADefectQueryReqVO(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        // 解析请求中的查询条件字段
        String queryDefectCondition = batchDefectProcessReqVO.getQueryDefectCondition();
        SCADefectQueryReqVO queryCondObj = JsonUtil.INSTANCE.to(queryDefectCondition, SCADefectQueryReqVO.class);
        if (queryCondObj == null) {
            log.error("defect batch op, query obj deserialize fail, json: {}", queryDefectCondition);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID);
        }
        log.info("defect batch op, query obj: {}", queryCondObj);

        // 设置业务处理对应的告警状态
        Set<String> statusAllows = new HashSet<>(getStatusConditionByBizType(batchDefectProcessReqVO.getBizType()));
        Set<String> retainStatus = CollectionUtils.isEmpty(queryCondObj.getStatus()) ? statusAllows
                : queryCondObj.getStatus().stream().filter(statusAllows::contains).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(retainStatus)) {
            retainStatus = Sets.newHashSet("-1");
        }
        queryCondObj.setStatus(retainStatus);

        return queryCondObj;
    }

    /**
     * 根据业务类型，获取对应需要处理的告警状态
     *
     * @param bizType
     * @return
     */
    protected Set<String> getStatusConditionByBizType(String bizType) {
        if (bizType.contains(ComConstants.BusinessType.IGNORE_DEFECT.value())) {
            // 对已经修复的BUG也可以进行忽略
            return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.NEW.value()),
                    String.valueOf(ComConstants.DefectStatus.FIXED.value()));

        } else if (bizType.contains(ComConstants.BusinessType.REVERT_IGNORE.value())) {
            return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.IGNORE.value()));

        } else if (bizType.contains(ComConstants.BusinessType.MARK_DEFECT.value())) {
            // 对于恢复忽略再标记的需要开放忽略
            return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.NEW.value()));
        } else if (bizType.contains(ComConstants.BusinessType.ASSIGN_DEFECT.value())) {
            // 对已忽略也可以进行处理人修改
            return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.NEW.value()),
                    String.valueOf(ComConstants.DefectStatus.IGNORE.value()));
        }
        return Sets.newHashSet(String.valueOf(ComConstants.DefectStatus.NEW.value()));
    }
}
