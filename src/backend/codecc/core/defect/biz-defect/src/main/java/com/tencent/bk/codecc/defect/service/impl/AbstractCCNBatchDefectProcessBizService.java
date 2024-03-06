package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.service.AbstractBatchDefectProcessBizService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.Tool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 圈复杂度告警处理处理抽象类
 *
 * @version V1.0
 * @date 2020/3/3
 */
@Slf4j
public abstract class AbstractCCNBatchDefectProcessBizService extends AbstractBatchDefectProcessBizService {

    @Autowired
    private CCNDefectDao ccnDefectDao;

    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Autowired
    private CCNQueryWarningBizServiceImpl ccnQueryWarningBizService;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Override
    protected abstract void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO);

    @Override
    protected List getDefectsByQueryCond(long taskId, DefectQueryReqVO request) {
        return getDefectsByQueryCondWithPage(taskId, request, null, null, null);
    }

    @Override
    protected List getDefectsByQueryCondWithPage(long taskId, DefectQueryReqVO request,
            String startFilePath, Long skip, Integer pageSize) {
        List<Long> taskIdList = Collections.singletonList(taskId);
        String buildId = request.getBuildId();
        List<String> dimensionList = Lists.newArrayList(Tool.CCN.name());
        List<String> toolNameList = Lists.newArrayList(Tool.CCN.name());
        Map<Long, List<String>> taskToolMap = ParamUtils.getTaskToolMap(
                toolNameList,
                dimensionList,
                taskIdList,
                buildId
        );
        log.info("query ccn defect list, task tool map: {}, \n{}", taskToolMap.size(),
                JsonUtil.INSTANCE.toJson(taskToolMap));
        // 多任务维度，有些任务可能曾经开启过圈复杂度，但现在已经停用了
        taskIdList = Lists.newArrayList(taskToolMap.keySet());
        request.setTaskIdList(taskIdList);

        // 严重级别
        Set<Map.Entry<Integer, Integer>> riskFactors = Sets.newHashSet();
        if (CollectionUtils.isNotEmpty(request.getSeverity())) {
            for (String severity : request.getSeverity()) {
                ComConstants.RiskFactor riskFactor = ComConstants.RiskFactor.get(Integer.parseInt(severity));
                riskFactors.add(thirdPartySystemCaller.getCCNRiskFactorConfig(riskFactor));
            }
        }

        // 获取快照defectId列表
        Set<String> defectIds = StringUtils.isNotEmpty(buildId)
                ? ccnQueryWarningBizService.getDefectIdsByBuildId(taskIdList.get(0), toolNameList, buildId)
                : Sets.newHashSet();


        Map<String, Boolean> filedMap = new HashMap<>();
        filedMap.put("_id", true);
        filedMap.put("status", true);
        filedMap.put("tool_name", true);
        filedMap.put("author", true);
        filedMap.put("severity", true);
        filedMap.put("rel_path", true);

        if (pageSize == null) {
            return ccnDefectDao.findDefectByCondition(taskIdList, request.getAuthor(),
                    CollectionUtils.isEmpty(request.getStatus()) ? Collections.emptySet() :
                            request.getStatus().stream().map(Integer::valueOf).collect(Collectors.toSet()),
                    request.getFileList(), riskFactors, defectIds, request.getBuildId(),
                    request.getStartCreateTime(), request.getEndCreateTime(), request.getIgnoreReasonTypes(), filedMap);
        } else {
            return ccnDefectDao.findDefectByConditionWithFilePathPage(taskIdList, request.getAuthor(),
                    CollectionUtils.isEmpty(request.getStatus()) ? Collections.emptySet() :
                            request.getStatus().stream().map(Integer::valueOf).collect(Collectors.toSet()),
                    request.getFileList(), riskFactors, defectIds, request.getBuildId(), request.getStartCreateTime(),
                    request.getEndCreateTime(), request.getIgnoreReasonTypes(), startFilePath, skip, pageSize,
                    filedMap);
        }
    }

    @Override
    protected void processCustomizeOpsAfterEachPageDone(List defectList,
            BatchDefectProcessReqVO batchDefectProcessReqVO) {

    }

    /**
     * 根据前端传入的告警key，查询有效的告警
     * 过滤规则是：忽略告警、告警处理人分配、告警标志修改针对的都是待修复告警，而恢复忽略针对的是已忽略告警
     *
     * @param batchDefectProcessReqVO
     */
    @Override
    protected List<CCNDefectEntity> getEffectiveDefectByDefectKeySet(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        Long taskId = batchDefectProcessReqVO.getTaskId();
        List<CCNDefectEntity> defecEntityList = ccnDefectRepository.findByTaskIdAndEntityIdIn(taskId,
                batchDefectProcessReqVO.getDefectKeySet());
        Iterator<CCNDefectEntity> it;
        if (CollectionUtils.isEmpty(defecEntityList)) {
            return new ArrayList<>();
        } else {
            it = defecEntityList.iterator();
        }

        while (it.hasNext()) {
            CCNDefectEntity defectEntity = it.next();
            int status = defectEntity.getStatus();
            Set<String> statusCondSet = getStatusCondition(null);
            boolean match = false;
            for (String statusCondStr : statusCondSet) {
                int statusCond = Integer.parseInt(statusCondStr);
                boolean notMatchNewStatus = statusCond == ComConstants.DefectStatus.NEW.value()
                        && status != ComConstants.DefectStatus.NEW.value();
                boolean notMatchIgnoreStatus = statusCond > ComConstants.DefectStatus.NEW.value()
                        && (status & statusCond) == 0;
                if (notMatchNewStatus || notMatchIgnoreStatus) {
                    continue;
                }
                match = true;
                break;
            }
            if (!match) {
                it.remove();
            }
        }
        return defecEntityList;
    }

    @Override
    protected List<Long> getTaskIdsByDefectKeySet(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        return ccnDefectDao.findTaskIdsByEntityIds(batchDefectProcessReqVO.getDefectKeySet());
    }
}
