package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.CLOCStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CodeRepoFromAnalyzeLogRepository;
import com.tencent.bk.codecc.defect.model.statistic.CLOCStatisticEntity;
import com.tencent.bk.codecc.defect.service.AbstractQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.EfficientCommentService;
import com.tencent.bk.codecc.defect.service.TaskLogOverviewService;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.bk.codecc.defect.service.TreeService;
import com.tencent.bk.codecc.defect.vo.CLOCDefectQueryRspInfoVO;
import com.tencent.bk.codecc.defect.vo.CLOCDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.CLOCDefectTreeRespVO;
import com.tencent.bk.codecc.defect.vo.CLOCTreeNodeVO;
import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import com.tencent.bk.codecc.defect.vo.TaskLogRepoInfoVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.service.utils.I18NUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service("CLOCQueryWarningBizService")
public class CLOCQueryWarningBizServiceImpl extends AbstractQueryWarningBizService {

    @Autowired
    CodeRepoFromAnalyzeLogRepository codeRepoFromAnalyzeLogRepository;
    @Autowired
    TaskLogService taskLogService;
    @Autowired
    private CLOCStatisticRepository clocStatisticRepository;
    @Autowired
    private TaskLogOverviewService taskLogOverviewService;
    @Autowired
    @Qualifier("CLOCTreeBizService")
    private TreeService treeService;
    @Autowired
    private EfficientCommentService efficientCommentService;

    @Override
    public int getSubmitStepNum() {
        return ComConstants.Step4MutliTool.COMMIT.value();
    }

    /**
     * 获取CLOC告警视图信息
     *
     * @param taskId          任务ID
     * @param pageNum         分页
     * @param pageSize        分页
     * @param queryWarningReq 查询擦树实体类
     * @param sortField       排序字段
     * @param sortType        排序类型
     */
    @Override
    public CommonDefectQueryRspVO processQueryWarningRequest(
            long taskId, DefectQueryReqVO queryWarningReq,
            int pageNum, int pageSize, String sortField, Sort.Direction sortType
    ) {
        CLOCDefectQueryRspVO clocDefectQueryRspVO = new CLOCDefectQueryRspVO();
        CLOCDefectTreeRespVO clocDefectTreeRespVO = new CLOCDefectTreeRespVO();

        switch (queryWarningReq.getOrder()) {
            case FILE: {
                generateFileTree(clocDefectTreeRespVO, taskId);
                return clocDefectTreeRespVO;
            }
            case LANGUAGE: {
                generateLanguage(clocDefectQueryRspVO, taskId);
                return clocDefectQueryRspVO;
            }
            default: {
                return clocDefectQueryRspVO;
            }
        }
    }

    @Override
    public CommonDefectDetailQueryRspVO processQueryWarningDetailRequest(Long taskId, String userId,
            CommonDefectDetailQueryReqVO queryWarningDetailReq,
            String sortField, Sort.Direction sortType
    ) {
        return new CommonDefectDetailQueryRspVO();
    }


    @Override
    public QueryWarningPageInitRspVO processQueryWarningPageInitRequest(
            String userId,
            List<Long> taskIdList,
            List<String> toolNameList,
            List<String> dimensionList,
            Set<String> statusSet,
            String checkerSet,
            String buildId,
            String projectId,
            boolean isMultiTaskQuery
    ) {
        return new QueryWarningPageInitRspVO();
    }



    /**
     * 按语言聚类CLOC扫描结果
     *
     * @param taskId               任务ID
     * @param clocDefectQueryRspVO CLOC告警统计响应体实体类
     */
    private void generateLanguage(CLOCDefectQueryRspVO clocDefectQueryRspVO, long taskId) {
        String toolName = getClocToolNameFromOverview(taskId);
        if (StringUtils.isEmpty(toolName)) {
            return;
        }
        CLOCStatisticEntity lastStatisticEntity =
                clocStatisticRepository.findFirstByTaskIdAndToolNameOrderByUpdatedDateDesc(taskId, toolName);
        if (lastStatisticEntity == null) {
            return;
        }
        String lastBuildId = lastStatisticEntity.getBuildId();
        log.info("query cloc info, taskId: {} | lastBuildId: {}", taskId, lastBuildId);
        List<CLOCStatisticEntity> clocStatisticEntities;
        if (StringUtils.isNotBlank(lastBuildId)) {
            clocStatisticEntities = clocStatisticRepository.findByTaskIdAndToolNameAndBuildId(
                    taskId, toolName, lastBuildId);
        } else {
            clocStatisticEntities = clocStatisticRepository.findByTaskIdAndToolName(taskId, toolName);
        }

        if (CollectionUtils.isEmpty(clocStatisticEntities)) {
            return;
        }

        List<CLOCDefectQueryRspInfoVO> clocDefectQueryRspInfoVOS = new ArrayList<>(clocStatisticEntities.size() + 2);
        long totalBlank = 0;
        long totalCode = 0;
        long totalComment = 0;
        long totalEfficientComment = 0;
        //用于计算有效注释率
        long totalLinesForEfficient = 0;
        List<String> effectiveFilters = efficientCommentService.getDisableLangList();

        for (CLOCStatisticEntity clocStatisticEntity : clocStatisticEntities) {
            totalBlank += clocStatisticEntity.getSumBlank();
            totalCode += clocStatisticEntity.getSumCode();
            totalComment += clocStatisticEntity.getSumComment();
            //计算总数时，忽略过滤掉的语言
            if (efficientCommentService.checkIfShowEffectiveComment(effectiveFilters,
                    clocStatisticEntity.getLanguage()) && clocStatisticEntity.getSumEfficientComment() != null) {
                totalEfficientComment += clocStatisticEntity.getSumEfficientComment();
                totalLinesForEfficient += (clocStatisticEntity.getSumBlank() + clocStatisticEntity.getSumCode()
                        + clocStatisticEntity.getSumComment());
            } else if (clocStatisticEntity.getSumEfficientComment() != null) {
                clocStatisticEntity.setSumEfficientComment(null);
            }
        }

        long totalLines = totalBlank + totalCode + totalComment;

        // 注入总计信息
        CLOCDefectQueryRspInfoVO totalInfo = new CLOCDefectQueryRspInfoVO();
        totalInfo.setLanguage(I18NUtils.getMessage("CODE_STATISTICS_ORDER_BY_LANGUAGE_TOTAL"));
        totalInfo.setSumBlank(totalBlank);
        totalInfo.setSumCode(totalCode);
        totalInfo.setSumComment(totalComment);
        totalInfo.setSumEfficientComment(totalEfficientComment);
        totalInfo.setSumLines(totalLines);
        totalInfo.setCommentRate(getCommentRate(totalLines, totalComment));
        totalInfo.setEfficientCommentRate(getEffectCommentRate(totalLinesForEfficient, totalEfficientComment));
        totalInfo.setProportion(100);
        clocDefectQueryRspVO.setTotalInfo(totalInfo);

        // 计算其他小比例语言统计信息
        AtomicLong otherSumBlank = new AtomicLong();
        AtomicLong otherSumCode = new AtomicLong();
        AtomicLong otherSumComment = new AtomicLong();
        AtomicLong otherSumEfficientComment = new AtomicLong();
        AtomicLong otherSumLines = new AtomicLong();
        AtomicLong otherSumLinesForEfficient = new AtomicLong();
        int otherProPortion = 0;

        List<CLOCDefectQueryRspInfoVO> losts = new LinkedList<>();
        // 计算各语言统计信息
        List<CLOCDefectQueryRspInfoVO> finalClocDefectQueryRspInfoVOS = clocDefectQueryRspInfoVOS;
        clocStatisticEntities.forEach(clocStatisticEntity -> {
            CLOCDefectQueryRspInfoVO clocDefectQueryRspInfoVO = new CLOCDefectQueryRspInfoVO();
            clocDefectQueryRspInfoVO.setLanguage(clocStatisticEntity.getLanguage());
            long sumBlank = clocStatisticEntity.getSumBlank();
            long sumCode = clocStatisticEntity.getSumCode();
            long sumComment = clocStatisticEntity.getSumComment();
            Long sumEfficientComment = clocStatisticEntity.getSumEfficientComment();
            long sumLines = sumBlank + sumCode + sumComment;
            double proportion = (((double) sumLines / (double) totalLines)) * 100;

            // 四舍五入精度丢失记录
            if ((proportion + 0.5) >= ((int) proportion + 1)) {
                losts.add(clocDefectQueryRspInfoVO);
            }

            if (proportion < 1) {
                otherSumBlank.addAndGet(sumBlank);
                otherSumCode.addAndGet(sumCode);
                otherSumComment.addAndGet(sumComment);
                if (sumEfficientComment != null) {
                    otherSumEfficientComment.addAndGet(sumEfficientComment);
                    otherSumLinesForEfficient.addAndGet(sumLines);
                }
                otherSumLines.addAndGet(sumLines);
            } else {
                clocDefectQueryRspInfoVO.setSumBlank(sumBlank);
                clocDefectQueryRspInfoVO.setSumCode(sumCode);
                clocDefectQueryRspInfoVO.setSumComment(sumComment);
                clocDefectQueryRspInfoVO.setSumEfficientComment(sumEfficientComment);
                clocDefectQueryRspInfoVO.setSumLines(sumLines);
                clocDefectQueryRspInfoVO.setCommentRate(getCommentRate(sumLines, sumComment));
                clocDefectQueryRspInfoVO.setEfficientCommentRate(getEffectCommentRate(sumLines, sumEfficientComment));
                clocDefectQueryRspInfoVO.setProportion((int) proportion);
                finalClocDefectQueryRspInfoVOS.add(clocDefectQueryRspInfoVO);
            }
        });

        // 计算其他小比例语言行数占比
        AtomicInteger allProportion = new AtomicInteger(clocDefectQueryRspInfoVOS.stream()
                .mapToInt(CLOCDefectQueryRspInfoVO::getProportion)
                .sum());
        if (otherSumLines.get() > 0) {
            otherProPortion = 100 - allProportion.get();
        } else if ((100 - allProportion.get()) > 0) {
            // 上面在计算集合中的语言行数所占百分比时精度丢失导致总百分比不到 100 的情况
            losts.forEach(lost -> {
                if (100 - allProportion.get() > 0) {
                    lost.setProportion(lost.getProportion() + 1);
                    allProportion.getAndIncrement();
                }
            });
        }
        // 注入其他小比例语言统计信息
        CLOCDefectQueryRspInfoVO otherInfo = new CLOCDefectQueryRspInfoVO();
        otherInfo.setLanguage("Others");
        otherInfo.setSumBlank(otherSumBlank.get());
        otherInfo.setSumCode(otherSumCode.get());
        otherInfo.setSumComment(otherSumComment.get());
        otherInfo.setSumEfficientComment(otherSumEfficientComment.get());
        otherInfo.setSumLines(otherSumLines.get());
        otherInfo.setCommentRate(getCommentRate(otherSumLines.get(), otherSumComment.get()));
        otherInfo.setEfficientCommentRate(getEffectCommentRate(otherSumLinesForEfficient.get(),
                otherSumEfficientComment.get()));
        otherInfo.setProportion(otherProPortion);
        clocDefectQueryRspVO.setOtherInfo(otherInfo);

        clocDefectQueryRspVO.setTaskId(taskId);
        clocDefectQueryRspVO.setNameEn(Tool.CLOC.name());
        clocDefectQueryRspVO.setToolName(toolName);
        // 按照代码行百分比排序，注入各语言统计信息
        clocDefectQueryRspInfoVOS = clocDefectQueryRspInfoVOS.stream()
                .sorted((x, y) -> -(x.getProportion() - y.getProportion()))
                .collect(Collectors.toList());
        clocDefectQueryRspVO.setLanguageInfo(clocDefectQueryRspInfoVOS);
    }

    /**
     * 按路径聚类CLOC扫描结果
     *
     * @param taskId               任务ID
     * @param clocDefectTreeRespVO CLOC告警树响应体实体类
     */
    private void generateFileTree(CLOCDefectTreeRespVO clocDefectTreeRespVO, long taskId) {
        String toolName = getClocToolNameFromOverview(taskId);
        if (StringUtils.isEmpty(toolName)) {
            return;
        }
        List<String> toolNames = toolName.equals(Tool.CLOC.name()) ? Arrays.asList(toolName, null) :
                Collections.singletonList(toolName);
        // 生成文件树
        CLOCTreeNodeVO root = (CLOCTreeNodeVO) treeService.getTreeNode(taskId, toolNames);
        clocDefectTreeRespVO.setClocTreeNodeVO(root);
        clocDefectTreeRespVO.setNameEn(Tool.CLOC.name());
        clocDefectTreeRespVO.setToolName(toolName);
        clocDefectTreeRespVO.setTaskId(taskId);

        // 注入代码库信息
        List<CLOCDefectTreeRespVO.CodeRepo> repoInfoList = new LinkedList<>();
        Map<String, TaskLogRepoInfoVO> repoInfoMap = taskLogService.getLastAnalyzeRepoInfo(taskId);
        if (repoInfoMap != null && !repoInfoMap.isEmpty()) {
            repoInfoMap.forEach(
                    (repoUrl, taskLogRepoInfoVO) -> {
                        if (StringUtils.isNotBlank(repoUrl)) {
                            repoInfoList.add(
                                    new CLOCDefectTreeRespVO.CodeRepo(repoUrl, taskLogRepoInfoVO.getBranch()));
                        }
                    });
        }

        if (repoInfoList.size() == 0) {
            log.info("get analyzed repo info fail, taskId: {}", taskId);
            repoInfoList.add(new CLOCDefectTreeRespVO.CodeRepo("", "master"));
        }
        clocDefectTreeRespVO.setCodeRepo(repoInfoList);
    }

    private String getClocToolNameFromOverview(Long taskId) {
        //查询taskId最近一次构建使用的工具是CLOC还是SCC（兼容以前的扫描数据）
        TaskLogOverviewVO entity = taskLogOverviewService.getTaskLogOverview(taskId, null, null);
        if (entity == null) {
            return null;
        }
        if (CollectionUtils.isEmpty(entity.getTools())
                || (!entity.getTools().contains(Tool.CLOC.name())
                && !entity.getTools().contains(Tool.SCC.name()))) {
            return Tool.CLOC.name();
        }
        return entity.getTools().contains(Tool.SCC.name()) ? Tool.SCC.name() : Tool.CLOC.name();
    }

    private Double getEffectCommentRate(Long total, Long comment) {
        if (comment == null) {
            return null;
        }
        return getCommentRate(total, comment);
    }


    private Double getCommentRate(Long total, Long comment) {
        if (total == null || total == 0 || comment == null || comment == 0) {
            return 0d;
        }
        return new BigDecimal(comment).divide(new BigDecimal(total), 4, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
}
