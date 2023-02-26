package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.service.AbstractBatchDefectProcessBizService;
import com.tencent.bk.codecc.defect.service.LintQueryWarningSpecialService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;

/**
 * Lint类工具告警批量处理抽象类
 *
 * @version V1.0
 * @date 2020/3/3
 */
@Slf4j
public abstract class AbstractLintBatchDefectProcessBizService extends AbstractBatchDefectProcessBizService {

    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;

    /**
     * 根据前端传入的条件查询告警键值
     *
     * @param taskId
     * @param request
     * @return
     */
    @Override
    protected List getDefectsByQueryCond(long taskId, DefectQueryReqVO request) {
        return getDefectsByQueryCondWithPage(taskId, request, null, null, null);
    }

    @Override
    protected List getDefectsByQueryCondWithPage(long taskId, DefectQueryReqVO request,
            String startFilePath, Long skip, Integer pageSize) {

        List<String> dimensionList = ParamUtils.allDimensionIfEmptyForLint(request.getDimensionList());
        String buildId = request.getBuildId();
        Map<Long, List<String>> taskToolMap = ParamUtils.getTaskToolMap(
                request.getToolNameList(),
                dimensionList,
                Lists.newArrayList(taskId),
                buildId
        );

        if (MapUtils.isEmpty(taskToolMap)) {
            return Lists.newArrayList();
        }

        Map<String, Boolean> filedMap = new HashMap<>();
        filedMap.put("_id", true);
        filedMap.put("status", true);
        filedMap.put("tool_name", true);
        filedMap.put("author", true);
        filedMap.put("severity", true);
        filedMap.put("file_path", true);

        LintQueryWarningSpecialService lintSpecialService =
                SpringContextUtil.Companion.getBean(LintQueryWarningSpecialService.class);
        // 快照对应告警主键集
        Pair<Set<String>, Set<String>> defectIdsPair = lintSpecialService.getDefectIdsPairByBuildId(
                taskToolMap, buildId
        );
        // 获取相同包id下的规则集合
        List<String> toolNameList = taskToolMap.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
        Set<String> pkgChecker = lintSpecialService.getCheckers(
                request.getCheckerSet(), request.getChecker(),
                toolNameList, dimensionList
        );
        List<LintDefectV2Entity> defectEntityList;
        if (pageSize == null) {
            defectEntityList = lintDefectV2Dao.findDefectByCondition(
                    taskToolMap,
                    request,
                    defectIdsPair.getFirst(),
                    pkgChecker,
                    filedMap,
                    defectIdsPair.getSecond()
            );
        } else {
            defectEntityList = lintDefectV2Dao.findDefectByConditionWithFilePathPage(
                    taskToolMap,
                    request,
                    defectIdsPair.getFirst(),
                    pkgChecker,
                    filedMap,
                    defectIdsPair.getSecond(),
                    startFilePath,
                    skip,
                    pageSize
            );
        }

        return defectEntityList;
    }

    @Override
    protected void processCustomizeOpsAfterEachPageDone(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO) {

    }

    @Override
    protected void processAfterAllPageDone(BatchDefectProcessReqVO batchDefectProcessReqVO) {

    }

    /**
     * 根据前端传入的告警key，查询有效的告警
     * 过滤规则是：忽略告警、告警处理人分配、告警标志修改针对的都是待修复告警，而恢复忽略针对的是已忽略告警
     *
     * @param batchDefectProcessReqVO
     */
    @Override
    protected List<LintDefectV2Entity> getEffectiveDefectByDefectKeySet(
            BatchDefectProcessReqVO batchDefectProcessReqVO
    ) {
        Long taskId = batchDefectProcessReqVO.getTaskId();
        Set<String> entityIdSet = batchDefectProcessReqVO.getDefectKeySet();
        List<LintDefectV2Entity> defectEntityList =
                lintDefectV2Repository.findNoneInstancesFieldByTaskIdAndEntityIdIn(taskId, entityIdSet);

        if (CollectionUtils.isEmpty(defectEntityList)) {
            return Lists.newArrayList();
        }

        Iterator<LintDefectV2Entity> it = defectEntityList.iterator();
        while (it.hasNext()) {
            LintDefectV2Entity defectEntity = it.next();
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

        return defectEntityList;
    }

    @Override
    protected List<Long> getTaskIdsByDefectKeySet(BatchDefectProcessReqVO batchDefectProcessReqVO) {
        Set<String> entityIdSet = batchDefectProcessReqVO.getDefectKeySet();
        return lintDefectV2Dao.findTaskIdsByEntityIds(entityIdSet);
    }

}
