/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.component.GongfengFilterPathComponent;
import com.tencent.bk.codecc.defect.component.QueryWarningLogicComponent;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.IgnoreCheckerRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.BuildDefectRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CodeFileUrlRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CommonStatisticRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.DefectRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.FirstAnalysisSuccessTimeRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.DefectDao;
import com.tencent.bk.codecc.defect.model.CodeFileUrlEntity;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.pojo.CommonDefectIssueQueryMultiCond;
import com.tencent.bk.codecc.defect.service.AbstractQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.CommonQueryWarningContentService;
import com.tencent.bk.codecc.defect.service.CommonQueryWarningSpecialService;
import com.tencent.bk.codecc.defect.service.TreeService;
import com.tencent.bk.codecc.defect.utils.ConvertUtil;
import com.tencent.bk.codecc.defect.vo.CommonDefectGroupStatisticVO;
import com.tencent.bk.codecc.defect.vo.DefectBaseVO;
import com.tencent.bk.codecc.defect.vo.DefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.DefectDetailVO;
import com.tencent.bk.codecc.defect.vo.DefectFileContentSegmentQueryRspVO;
import com.tencent.bk.codecc.defect.vo.DefectFilesInfoVO;
import com.tencent.bk.codecc.defect.vo.DefectInstanceVO;
import com.tencent.bk.codecc.defect.vo.DefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.QueryDefectFileContentSegmentReqVO;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
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
import com.tencent.bk.codecc.defect.vo.openapi.DefectDetailExtVO;
import com.tencent.bk.codecc.defect.vo.openapi.TaskDefectVO;
import com.tencent.bk.codecc.defect.vo.report.CommonChartAuthorVO;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.auth.api.service.AuthTaskService;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatus;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.ListSortUtil;
import com.tencent.devops.common.util.MD5Utils;
import com.tencent.devops.common.util.PathUtils;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Coverity告警管理服务实现
 *
 * @version V1.0
 * @date 2019/10/24
 */
@Slf4j
@Service("CommonQueryWarningBizService")
public class CommonQueryWarningBizServiceImpl extends AbstractQueryWarningBizService
        implements CommonQueryWarningSpecialService {

    @Autowired
    private DefectRepository defectRepository;

    @Autowired
    private CodeFileUrlRepository codeFileUrlRepository;

    @Autowired
    private FirstAnalysisSuccessTimeRepository firstSuccessTimeRepository;

    @Autowired
    private BizServiceFactory<TreeService> treeServiceBizServiceFactory;

    @Autowired
    private CheckerRepository checkerRepository;

    @Autowired
    private IgnoreCheckerRepository ignoreCheckerRepository;

    @Autowired
    private AuthTaskService authTaskService;

    @Autowired
    private CommonQueryWarningContentService commonQueryWarningContentService;

    @Autowired
    private GongfengFilterPathComponent gongfengFilterPathComponent;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${codecc.public.url}")
    private String codeccGateWay;

    @Autowired
    private BuildDefectRepository buildDefectRepository;

    @Autowired
    private CommonStatisticRepository commonStatisticRepository;

    @Autowired
    private DefectDao defectDao;

    @Autowired
    private CheckerSetRepository checkerSetRepository;

    @Autowired
    private QueryWarningLogicComponent queryWarningLogicComponent;

    @Override
    public CommonDefectQueryRspVO processQueryWarningRequest(
            long taskId, DefectQueryReqVO defectQueryReqVO,
            int pageNum, int pageSize, String sortField, Sort.Direction sortType
    ) {
        return new CommonDefectQueryRspVO();
    }


    protected Set<String> convertDefectPathsToRelatePath(Set<String> defectPaths, Map<String, String> relatePathMap) {
        Set<String> defectPathsSet = new HashSet<>();
        if (CollectionUtils.isEmpty(defectPaths)) {
            return defectPaths;
        }
        // 这里过滤路径空的告警，页面按路径过滤 & 路径树 里看不到被过滤的告警
        defectPaths.stream().filter(Objects::nonNull)
                .forEach(defectPath -> {
                    defectPath = trimWinPathPrefix(defectPath);
                    String defectRelatePath = relatePathMap.get(defectPath.toLowerCase());
                    defectPathsSet.add(StringUtils.isEmpty(defectRelatePath) ? defectPath : defectRelatePath);
                });
        return defectPathsSet;
    }

    @Override
    public CommonDefectDetailQueryRspVO processQueryWarningDetailRequest(
            String projectId,
            Long taskId,
            String userId,
            CommonDefectDetailQueryReqVO queryWarningDetailReq,
            String sortField,
            Sort.Direction sortType) {
        DefectDetailQueryRspVO defectDetailQueryRspVO = new DefectDetailQueryRspVO();

        //查询告警信息
        CommonDefectEntity commonDefectEntity =
                defectRepository.findFirstByEntityId(queryWarningDetailReq.getEntityId());
        if (commonDefectEntity == null) {
            log.error("can't find defect entity by entityId: {}", queryWarningDetailReq.getEntityId());
            throw new CodeCCException(
                    CommonMessageCode.RECORD_NOT_EXITS,
                    new String[]{queryWarningDetailReq.getEntityId()}
            );
        }

        // 修正快照查
        String buildId = queryWarningDetailReq.getBuildId();
        if (StringUtils.isNotBlank(buildId)) {
            List<CommonDefectEntity> postHandleDefectList =
                    queryWarningLogicComponent.postHandleCommonDefect(Lists.newArrayList(commonDefectEntity), buildId);

            if (CollectionUtils.isNotEmpty(postHandleDefectList)) {
                commonDefectEntity = postHandleDefectList.get(0);
            }

            setDefectStatusOnLastBuild(taskId, buildId, commonDefectEntity.getId(), defectDetailQueryRspVO);
        }

        DefectDetailVO defectDetailVO = getDefectDetailVO(commonDefectEntity);
        if (defectDetailVO == null) {
            return defectDetailQueryRspVO;
        }

        CodeFileUrlEntity codeFileUrlEntity = codeFileUrlRepository.findFirstByTaskIdAndFile(
                taskId, commonDefectEntity.getFilePath());
        if (codeFileUrlEntity != null) {
            Map<String, CodeFileUrlEntity> codeRepoUrlMap = Maps.newHashMap();
            codeRepoUrlMap.put(codeFileUrlEntity.getFile(), codeFileUrlEntity);
            replaceFileNameWithURL(defectDetailVO, codeRepoUrlMap);
        }

        defectDetailQueryRspVO.setDefectDetailVO(defectDetailVO);
        defectDetailQueryRspVO.setFilePath(defectDetailVO.getFilePath());
        defectDetailQueryRspVO.setFileName(defectDetailVO.getFileName());

        return defectDetailQueryRspVO;
    }

    @Override
    public CommonDefectDetailQueryRspVO processQueryDefectDetailWithoutFileContent(Long taskId, String userId,
            CommonDefectDetailQueryReqVO queryWarningDetailReq, String sortField, Sort.Direction sortType) {
        return new CommonDefectDetailQueryRspVO();
    }

    @Override
    public DefectDetailVO getFilesContent(DefectDetailVO defectDetailVO) {
        return commonQueryWarningContentService.getFilesContent(defectDetailVO);
    }

    @Override
    public DefectFileContentSegmentQueryRspVO processQueryDefectFileContentSegment(String projectId, String userId,
            QueryDefectFileContentSegmentReqVO request) {
        return new DefectFileContentSegmentQueryRspVO();
    }

    @Override
    public DefectDetailVO getFilesInfo(DefectDetailVO defectDetailVO) {
        return commonQueryWarningContentService.getFilesInfo(defectDetailVO);
    }

    @Override
    @Deprecated
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
        /*long beginTime = System.currentTimeMillis();  // NOCC:VariableDeclarationUsageDistance(设计如此:)

        QueryWarningPageInitRspVO rspVO = new QueryWarningPageInitRspVO();
        List<String> toolNameSet = ParamUtils.getToolsByDimension(toolName, dimension, taskId, buildId);

        if (CollectionUtils.isEmpty(toolNameSet)) {
            return rspVO;
        }
        Set<Integer> statusFilter = getStatusFilterSet(statusSet, buildId);
        Set<String> defectIdSetByBuildId = getDefectIdsByBuildId(taskId, Lists.newArrayList(toolName), buildId);

        //统计文件树
        List<CommonDefectGroupStatisticVO> filePathGroups =
                defectDao.statisticByFilePath(taskId, toolNameSet, statusFilter, buildId, defectIdSetByBuildId);

        if (CollectionUtils.isNotEmpty(filePathGroups)) {
            String firstToolName = new ArrayList<>(toolNameSet).get(0);
            TreeService treeService = treeServiceBizServiceFactory.createBizService(
                    firstToolName, ComConstants.BusinessType.TREE_SERVICE.value(), TreeService.class);
            Map<String, String> relatePathMap = treeService.getRelatePathMap(taskId);
            Set<String> defectPaths = filePathGroups.stream().map(CommonDefectGroupStatisticVO::getFilePathName)
                    .collect(Collectors.toSet());
            defectPaths = convertDefectPathsToRelatePath(defectPaths, relatePathMap);
            TreeNodeVO treeNode = treeService.getTreeNode(taskId, defectPaths);
            rspVO.setFilePathTree(treeNode);
        } else {
            rspVO.setFilePathTree(new TreeNodeVO());
        }
        log.info("get file info size is: {}, task id: {}, tool name: {}", filePathGroups.size(), taskId, toolNameSet);

        //处理规则
        List<CommonDefectGroupStatisticVO> checkerGroups =
                defectDao.statisticByChecker(taskId, toolNameSet, statusFilter, buildId, defectIdSetByBuildId);
        if (CollectionUtils.isNotEmpty(checkerGroups)) {
            Map<String, Integer> checkerMap = new HashMap<>();
            checkerGroups.forEach(checkerGroup -> {
                checkerMap.put(checkerGroup.getChecker(),
                        checkerMap.getOrDefault(checkerGroup.getChecker(), 0)
                                + checkerGroup.getDefectCount());
            });
            rspVO.setCheckerMap(checkerMap);
        } else {
            rspVO.setCheckerMap(Maps.newHashMap());
        }

        //处理作者
        List<CommonDefectGroupStatisticVO> authorGroups =
                defectDao.statisticByAuthor(taskId, toolNameSet, statusFilter, buildId, defectIdSetByBuildId);
        if (CollectionUtils.isNotEmpty(authorGroups)) {
            Map<String, Integer> authorMap = new HashMap<>();
            authorGroups.forEach(checkerGroup -> {
                authorMap.put(checkerGroup.getAuthor(),
                        authorMap.getOrDefault(checkerGroup.getAuthor(), 0)
                                + checkerGroup.getDefectCount());
            });
            rspVO.setAuthorMap(authorMap);
        } else {
            rspVO.setAuthorMap(Maps.newHashMap());
        }

        log.info("======================getCheckerAuthorPathForPageInit cost: {}",
                System.currentTimeMillis() - beginTime);

        return rspVO;*/
    }

    @Override
    @Deprecated
    public Object pageInit(String projectId, DefectQueryReqVO request) {
        return new QueryWarningPageInitRspVO();
        /*log.info("begin pageInit taskId: {}, {}", taskId, GsonUtils.toJson(defectQueryReqVO));
        QueryWarningPageInitRspVO rspVO = new QueryWarningPageInitRspVO();
        String buildId = defectQueryReqVO.getBuildId();
        String toolName = defectQueryReqVO.getToolName();
        String dimension = defectQueryReqVO.getDimension();
        List<String> toolNameSet = ParamUtils.getToolsByDimension(toolName, dimension, taskId, buildId);

        log.info("begin pageInit with tool name set for task {}, {}", taskId, toolNameSet);

        if (CollectionUtils.isEmpty(toolNameSet)) {
            return rspVO;
        }

        //忽略新旧问题的过滤条件
        defectQueryReqVO.setDefectType(Sets.newHashSet(String.valueOf(ComConstants.DefectType.NEW.value()),
                String.valueOf(ComConstants.DefectType.HISTORY.value())));
        if (ComConstants.StatisticType.STATUS.name().equalsIgnoreCase(defectQueryReqVO.getStatisticType())) {
            // 1.根据规则、处理人、快照、路径、日期过滤后计算各状态告警数
            //统计状态时 忽略状态与严重程度的过滤条件
            defectQueryReqVO.setStatus(null);
            defectQueryReqVO.setSeverity(null);
            statisticByStatus(taskId, toolNameSet, defectQueryReqVO, rspVO);
        } else if (ComConstants.StatisticType.SEVERITY.name().equalsIgnoreCase(defectQueryReqVO.getStatisticType())) {
            // 2.根据规则、处理人、快照、路径、日期、状态过滤后计算: 各严重级别告警数
            //统计状态时 忽略严重程度的过滤条件
            defectQueryReqVO.setSeverity(null);
            statisticBySeverity(taskId, toolNameSet, defectQueryReqVO, rspVO);
        } else if (ComConstants.StatisticType.DEFECT_TYPE.name()
                .equalsIgnoreCase(defectQueryReqVO.getStatisticType())) {
            // 3.根据规则、处理人、快照、路径、日期、状态过滤后计算: 新老告警数
            statisticByDefectType(taskId, toolNameSet, defectQueryReqVO, rspVO);
        } else {
            log.error("StatisticType is invalid. {}", GsonUtils.toJson(defectQueryReqVO));
        }
        log.info("pageInit finish for task {}", taskId);
        return rspVO;*/
    }

    protected void statisticByStatus(long taskId, List<String> toolNameSet, DefectQueryReqVO defectQueryReqVO,
            QueryWarningPageInitRspVO rspVO) {
        List<CommonDefectGroupStatisticVO> groups = defectDao.statisticByStatus(taskId, toolNameSet,
                getQueryMultiCond(taskId, toolNameSet, defectQueryReqVO));
        groups.forEach(it -> {
            if (it.getStatus() == DefectStatus.NEW.value()) {
                rspVO.setExistCount(rspVO.getExistCount() + it.getDefectCount());
            } else if ((it.getStatus() & DefectStatus.FIXED.value()) > 0) {
                rspVO.setFixCount(rspVO.getFixCount() + it.getDefectCount());
            } else if ((it.getStatus() & DefectStatus.IGNORE.value()) > 0) {
                rspVO.setIgnoreCount(rspVO.getIgnoreCount() + it.getDefectCount());
            } else {
                rspVO.setMaskCount(rspVO.getMaskCount() + it.getDefectCount());
            }
        });

        // 若是快照查，则修正统计；快照查已移除"已修复"状态
        String buildId = defectQueryReqVO.getBuildId();
        if (StringUtils.isNotEmpty(buildId)) {
            // 已忽略、已屏蔽在多分支下是共享的；而待修复与已修复是互斥的
            rspVO.setExistCount(rspVO.getExistCount() + rspVO.getFixCount());
            rspVO.setFixCount(0);
        }
    }

    protected void statisticBySeverity(long taskId, List<String> toolNameSet, DefectQueryReqVO defectQueryReqVO,
            QueryWarningPageInitRspVO rspVO) {
        List<CommonDefectGroupStatisticVO> groups = defectDao.statisticBySeverity(taskId, toolNameSet,
                getQueryMultiCond(taskId, toolNameSet, defectQueryReqVO));
        groups.forEach(it -> {
            if (ComConstants.SERIOUS == it.getSeverity()) {
                rspVO.setSeriousCount(rspVO.getSeriousCount() + it.getDefectCount());
            } else if (ComConstants.NORMAL == it.getSeverity()) {
                rspVO.setNormalCount(rspVO.getNormalCount() + it.getDefectCount());
            } else if (ComConstants.PROMPT_IN_DB == it.getSeverity() || ComConstants.PROMPT == it.getSeverity()) {
                rspVO.setPromptCount(rspVO.getPromptCount() + it.getDefectCount());
            }
        });
    }

    protected void statisticByDefectType(long taskId, List<String> toolNameSet, DefectQueryReqVO defectQueryReqVO,
            QueryWarningPageInitRspVO rspVO) {
        defectQueryReqVO.setDefectType(Sets.newHashSet(String.valueOf(ComConstants.DefectType.NEW.value())));
        Long newDefectCount = defectDao.statisticByDefectType(taskId, toolNameSet,
                getQueryMultiCond(taskId, toolNameSet, defectQueryReqVO));
        rspVO.setNewCount(newDefectCount.intValue());

        defectQueryReqVO.setDefectType(Sets.newHashSet(String.valueOf(ComConstants.DefectType.HISTORY.value())));
        Long historyDefectCount = defectDao.statisticByDefectType(taskId, toolNameSet,
                getQueryMultiCond(taskId, toolNameSet, defectQueryReqVO));
        rspVO.setHistoryCount(historyDefectCount.intValue());
    }

    /**
     * 获取告警详情（将告警entity转成VO，整理告警相关文件）
     *
     * @param commonDefectEntity
     * @return
     */
    protected DefectDetailVO getDefectDetailVO(CommonDefectEntity commonDefectEntity) {
        DefectDetailVO defectDetailVO = JsonUtil.INSTANCE
                .to(JsonUtil.INSTANCE.toJson(commonDefectEntity), DefectDetailVO.class);
        List<DefectInstanceVO> defectInstanceList = defectDetailVO.getDefectInstances();
        if (CollectionUtils.isNotEmpty(defectInstanceList)) {
            for (DefectInstanceVO defectInstance : defectInstanceList) {
                List<DefectInstanceVO.Trace> traces = defectInstance.getTraces();
                for (int i = 0; i < traces.size(); i++) {
                    DefectInstanceVO.Trace trace = traces.get(i);
                    if (trace.getTraceNum() == null) {
                        trace.setTraceNum(i + 1);
                    }
                    parseTrace(defectDetailVO.getFileInfoMap(), trace);
                }
            }
            if (defectDetailVO.getFileInfoMap().size() < 1) {
                String md5 = defectDetailVO.getFileMd5();
                if (StringUtils.isEmpty(md5)) {
                    md5 = MD5Utils.getMD5(defectDetailVO.getFilePath());
                }
                DefectFilesInfoVO fileInfo = new DefectFilesInfoVO();
                fileInfo.setFilePath(defectDetailVO.getFilePath());
                fileInfo.setFileMd5(md5);
                fileInfo.setMinLineNum(defectDetailVO.getLineNum());
                fileInfo.setMaxLineNum(defectDetailVO.getLineNum());
                defectDetailVO.getFileInfoMap().put(md5, fileInfo);
            }
        }

        // 判断工具是否已经下架，已下架的工具不能从platform获取告警详情
        ToolMetaCacheService toolMetaCache = SpringContextUtil.Companion.getBean(ToolMetaCacheService.class);
        ToolMetaBaseVO toolMetaBase = toolMetaCache.getToolBaseMetaCache(defectDetailVO.getToolName());
        if (!ComConstants.ToolIntegratedStatus.D.name().equals(toolMetaBase.getStatus())) {
            defectDetailVO = getFilesContent(defectDetailVO);
        }

        return defectDetailVO;
    }

    /**
     * 转换 DefectQueryReqVO -> 请求体 CommonDefectIssueQueryMultiCond
     *
     * @param taskId
     * @param toolNameSet
     * @param defectQueryReqVO
     * @return
     */
    @Deprecated
    public CommonDefectIssueQueryMultiCond getQueryMultiCond(long taskId, List<String> toolNameSet,
            DefectQueryReqVO defectQueryReqVO) {
        throw new UnsupportedOperationException();
    }


    /**
     * 根据根据前端传入的条件过滤告警，并分类统计
     *
     * @param taskId
     * @param defectList
     * @param defectQueryReqVO
     * @param queryRspVO
     * @return
     */
    @Override
    public Set<String> filterDefectByCondition(long taskId,
            List<?> defectList,
            Set<String> allChecker,
            DefectQueryReqVO defectQueryReqVO,
            CommonDefectQueryRspVO queryRspVO,
            List<String> toolNameSet) {
        if (CollectionUtils.isEmpty(defectList)) {
            log.info("task[{}] defect entity list is empty", taskId);
            return new HashSet<>();
        }

        // 按构建号筛选
        boolean needBuildIdFilter = false;
        Set<String> buildDefectIds = Sets.newHashSet();
        if (StringUtils.isNotEmpty(defectQueryReqVO.getBuildId())) {
            buildDefectIds = getDefectIdsByBuildId(taskId, toolNameSet, defectQueryReqVO.getBuildId());
            needBuildIdFilter = true;
        }

        //根据查询条件进行过滤，并统计数量
        String condAuthor = defectQueryReqVO.getAuthor();
        Set<String> condFileList = getConditionFilterFiles(defectQueryReqVO);
        Set<String> condSeverityList = defectQueryReqVO.getSeverity();
        Set<String> condStatusList = defectQueryReqVO.getStatus();
        if (CollectionUtils.isEmpty(condStatusList)) {
            condStatusList = new HashSet<>(3);
            condStatusList.add(String.valueOf(DefectStatus.NEW.value()));
            condStatusList.add(String.valueOf(DefectStatus.FIXED.value()));
            condStatusList.add(String.valueOf(DefectStatus.IGNORE.value()));
        }
        if (condStatusList.contains(String.valueOf(DefectStatus.PATH_MASK.value()))) {
            condStatusList.add(String.valueOf(DefectStatus.CHECKER_MASK.value()));
        }
        String condStartCreateTime = defectQueryReqVO.getStartCreateTime();
        String condEndCreateTime = defectQueryReqVO.getEndCreateTime();
        String condStartFixTime = defectQueryReqVO.getStartFixTime();
        String condEndFixTime = defectQueryReqVO.getEndFixTime();
        Set<String> condDefectTypeList = defectQueryReqVO.getDefectType();
        Set<Integer> ignoreReasonTypes = defectQueryReqVO.getIgnoreReasonTypes();
        boolean condIgnoreTypesNotEmpty = CollectionUtils.isNotEmpty(ignoreReasonTypes);

        int seriousCount = 0;
        int normalCount = 0;
        int promptCount = 0;
        int existCount = 0;
        int fixCount = 0;
        int ignoreCount = 0;
        int maskCount = 0;
        int totalCount = 0;
        int newCount = 0;
        // NOCC:VariableDeclarationUsageDistance(设计如此:)
        int historyCount = 0;
        Map<String, Integer> checkerMap = new TreeMap<>();
        Map<String, Integer> authorMap = new TreeMap<>();
        Set<String> defectPaths = new HashSet<>();
        Iterator<CommonDefectEntity> it = (Iterator<CommonDefectEntity>) defectList.iterator();
        while (it.hasNext()) {
            CommonDefectEntity commonDefectEntity = it.next();

            // 判断是否匹配告警状态，先收集统计匹配告警状态的规则、处理人、文件路径
            boolean notMatchStatus = isNotMatchStatus(condStatusList, commonDefectEntity.getStatus());
            String checkerName = commonDefectEntity.getChecker();
            Set<String> authorList = commonDefectEntity.getAuthorList();
            if (!notMatchStatus) {
                checkerMap.put(checkerName, checkerMap.get(checkerName) == null ? 1 : checkerMap.get(checkerName) + 1);

                if (CollectionUtils.isNotEmpty(authorList)) {
                    authorList.forEach(author -> {
                        authorMap.put(author, authorMap.get(author) == null ? 1 : authorMap.get(author) + 1);
                    });
                }
                defectPaths.add(commonDefectEntity.getFilePath());
            }

            // 按构建号筛选
            if (needBuildIdFilter && !buildDefectIds.contains(commonDefectEntity.getId())) {
                it.remove();
                continue;
            }

            // 规则类型条件不为空且与当前数据的规则类型不匹配时，判断为true移除，否则false不移除
            boolean notMatchChecker = CollectionUtils.isNotEmpty(allChecker) && !allChecker.contains(checkerName);
            if (notMatchChecker) {
                it.remove();
                continue;
            }

            //告警作者条件不为空且与当前数据的作者不匹配时，判断为true移除，否则false不移除
            boolean notMatchAuthor = StringUtils.isNotEmpty(condAuthor) && (CollectionUtils.isEmpty(authorList)
                    || !authorList.contains(condAuthor));
            if (notMatchAuthor) {
                it.remove();
                continue;
            }

            // 根据文件过滤
            boolean notMatchFilePath = false;
            if (CollectionUtils.isNotEmpty(condFileList)) {
                // 判断文件名是否匹配文件路径列表，不匹配就移除
                notMatchFilePath = !checkIfMaskByPath(commonDefectEntity.getFilePath(), condFileList);
            }
            if (notMatchFilePath) {
                it.remove();
                continue;
            }

            // 根据创建时间过滤，判断为true移除，否则false不移除
            boolean notMatchCreateTime = DateTimeUtils.filterDate(
                    condStartCreateTime, condEndCreateTime, commonDefectEntity.getCreateTime());
            if (notMatchCreateTime) {
                it.remove();
                continue;
            }

            // 根据修复时间过滤，判断为true移除，否则false不移除
            boolean notMatchFixTime = DateTimeUtils.filterDate(
                    condStartFixTime, condEndFixTime, commonDefectEntity.getFixedTime());
            if (notMatchFixTime) {
                it.remove();
                continue;
            }

            // 根据状态过滤，注：已修复和其他状态（忽略，路径屏蔽，规则屏蔽）不共存，已忽略状态优先于屏蔽
            int status = commonDefectEntity.getStatus();
            if (DefectStatus.NEW.value() == status) {
                existCount++;
            } else if ((DefectStatus.FIXED.value() & status) > 0) {
                fixCount++;
            } else if ((DefectStatus.IGNORE.value() & status) > 0) {
                ignoreCount++;
            } else if ((DefectStatus.PATH_MASK.value() & status) > 0
                    || (DefectStatus.CHECKER_MASK.value() & status) > 0) {
                maskCount++;
            }
            // 严重程度条件不为空且与当前数据的严重程度不匹配时，判断为true移除，否则false不移除
            if (notMatchStatus) {
                it.remove();
                continue;
            }
            // 按忽略类型筛选
            if (condIgnoreTypesNotEmpty && !ignoreReasonTypes.contains(commonDefectEntity.getIgnoreReasonType())) {
                it.remove();
                continue;
            }

            // 根据严重等级过滤
            int severity = commonDefectEntity.getSeverity();
            if (ComConstants.SERIOUS == severity) {
                seriousCount++;
            } else if (ComConstants.NORMAL == severity) {
                normalCount++;
            } else if (ComConstants.PROMPT == severity) {
                promptCount++;
            }
            // 严重程度条件不为空且与当前数据的严重程度不匹配时，判断为true移除，否则false不移除
            boolean notMatchSeverity = CollectionUtils.isNotEmpty(condSeverityList)
                    && !condSeverityList.contains(String.valueOf(severity));
            if (notMatchSeverity) {
                it.remove();
                continue;
            }

            newCount++;
            totalCount++;
        }

        DefectQueryRspVO defectQueryRspVO = (DefectQueryRspVO) queryRspVO;
        defectQueryRspVO.setSeriousCount(seriousCount);
        defectQueryRspVO.setNormalCount(normalCount);
        defectQueryRspVO.setPromptCount(promptCount);
        defectQueryRspVO.setExistCount(existCount);
        defectQueryRspVO.setFixCount(fixCount);
        defectQueryRspVO.setMaskCount(maskCount);
        defectQueryRspVO.setIgnoreCount(ignoreCount);
        defectQueryRspVO.setNewCount(newCount);
        defectQueryRspVO.setHistoryCount(historyCount);
        defectQueryRspVO.setTotalCount(totalCount);
        defectQueryRspVO.setCheckerMap(checkerMap);
        defectQueryRspVO.setAuthorMap(authorMap);
        return defectPaths;
    }

    @Override
    public DeptTaskDefectRspVO processDeptTaskDefectReq(DeptTaskDefectReqVO deptTaskDefectReqVO) {
        log.info("DeptTaskDefectReqVO content: {}", deptTaskDefectReqVO);
        String toolName = deptTaskDefectReqVO.getToolName();
        long startTime = DateTimeUtils.getTimeStampStart(deptTaskDefectReqVO.getStartDate());
        long endTime = DateTimeUtils.getTimeStampEnd(deptTaskDefectReqVO.getEndDate());

        DeptTaskDefectRspVO taskDefectRspVO = new DeptTaskDefectRspVO();
        List<TaskDefectVO> taskDefectList = Lists.newArrayList();

        List<TaskDetailVO> taskInfoVoList = getTaskDetailVoList(deptTaskDefectReqVO);

        if (CollectionUtils.isNotEmpty(taskInfoVoList)) {
            // 按部门和中心排序
            taskInfoVoList.sort((Comparator.comparingInt(TaskDetailVO::getDeptId)
                    .thenComparingInt(TaskDetailVO::getCenterId)));

            // 获取代码语言类型元数据
            List<MetadataVO> metadataVoList = getCodeLangMetadataVoList();
            // 获取组织架构信息
            Map<String, String> deptInfo =
                    (Map<String, String>) redisTemplate.opsForHash().entries(RedisKeyConstants.KEY_DEPT_INFOS);

            Set<Long> taskIdSet = taskInfoVoList.stream().map(TaskDetailVO::getTaskId).collect(Collectors.toSet());

            // 批量获取最近分析日志
            Map<Long, TaskLogVO> taskLogVoMap = getTaskLogVoMap(taskIdSet, toolName);

            // 获取所有状态的告警,组装成Map映射
            List<CommonDefectEntity> commonDefectEntityList = defectDao.batchQueryDefect(
                    toolName, taskIdSet, null, null);
            Map<Long, List<CommonDefectEntity>> defectMap = getLongListDefectMap(commonDefectEntityList);

            for (TaskDetailVO taskDetailVO : taskInfoVoList) {
                TaskDefectVO taskDefectVO = new TaskDefectVO();
                BeanUtils.copyProperties(taskDetailVO, taskDefectVO);
                taskDefectVO.setCodeLang(ConvertUtil.convertCodeLang(taskDetailVO.getCodeLang(), metadataVoList));
                taskDefectVO.setBgName(deptInfo.get(String.valueOf(taskDetailVO.getBgId())));
                taskDefectVO.setDeptName(deptInfo.get(String.valueOf(taskDetailVO.getDeptId())));
                taskDefectVO.setCenterName(deptInfo.get(String.valueOf(taskDetailVO.getCenterId())));
                String projectId = taskDefectVO.getProjectId();
                long taskId = taskDetailVO.getTaskId();
                taskDefectVO.setRepoUrl(
                        String.format("http://%s/codecc/%s/task/%s/detail", codeccGateWay, projectId, taskId));
                setAnalyzeDateStatus(taskId, taskLogVoMap, taskDefectVO);

                CommonChartAuthorVO newAddCount = new CommonChartAuthorVO();
                taskDefectVO.setNewAddCount(newAddCount);
                taskDefectVO.setCreatedDate(taskDetailVO.getCreatedDate());

                List<CommonDefectEntity> defectList = defectMap.get(taskId);
                if (CollectionUtils.isEmpty(defectList)) {
                    taskDefectVO.setTimeoutDefectNum(0);
                    taskDefectList.add(taskDefectVO);
                    continue;
                }

                int defectTimeoutNum = 0;
                for (CommonDefectEntity defect : defectList) {
                    long createTime = defect.getCreateTime();
                    int status = defect.getStatus();
                    int severity = defect.getSeverity();
                    if (createTime > startTime && createTime <= endTime) {
                        // 不是屏蔽忽略的告警（有效新增）
                        if ((DefectStatus.CHECKER_MASK.value() & status) == 0
                                && (DefectStatus.PATH_MASK.value() & status) == 0
                                && (DefectStatus.IGNORE.value() & status) == 0) {
                            taskDefectVO.getNewAddCount().count(severity);
                        }
                    }
                    // 已修复告警
                    long fixedTime = defect.getFixedTime();
                    if (fixedTime > startTime && fixedTime <= endTime) {
                        if ((DefectStatus.FIXED.value() & status) > 0) {
                            taskDefectVO.getFixedCount().count(severity);
                        }
                    }
                    // 遗留未修复的告警
                    if (createTime <= endTime && DefectStatus.NEW.value() == status) {
                        taskDefectVO.getExistCount().count(severity);
                        // 统计超时告警数
                        if ((endTime - createTime) / DateTimeUtils.DAY_TIMESTAMP
                                > deptTaskDefectReqVO.getTimeoutDays()) {
                            defectTimeoutNum++;
                        }
                    }
                }
                taskDefectVO.setTimeoutDefectNum(defectTimeoutNum);
                taskDefectList.add(taskDefectVO);
            }
        }

        taskDefectRspVO.setTaskDefectVoList(taskDefectList);

        return taskDefectRspVO;
    }

    @Override
    public ToolDefectRspVO processDeptDefectList(DeptTaskDefectReqVO defectQueryReq, Integer pageNum, Integer pageSize,
            String sortField, Sort.Direction sortType) {
        log.info("processDeptDefectList req content: {}", defectQueryReq);

        String toolName = defectQueryReq.getToolName();
        long endTime = DateTimeUtils.getTimeStamp(defectQueryReq.getEndDate() + " 23:59:59");
        boolean severityFlag = defectQueryReq.getSeverity() != null;

        ToolDefectRspVO defectRspVO = new ToolDefectRspVO();

        List<TaskDetailVO> taskDetailVoList = getTaskDetailVoList(defectQueryReq);
        if (CollectionUtils.isNotEmpty(taskDetailVoList)) {
            // 取出任务ID集合
            Set<Long> taskIdSet = taskDetailVoList.stream().map(TaskDetailVO::getTaskId).collect(Collectors.toSet());

            // 获取遗留状态的告警
            List<CommonDefectEntity> commonDefectEntityList =
                    defectDao.batchQueryDefect(toolName, taskIdSet, null, DefectStatus.NEW.value());

            List<DefectDetailExtVO> defectDetailExtVOs = Lists.newArrayList();
            if (CollectionUtils.isNotEmpty(commonDefectEntityList)) {
                for (CommonDefectEntity defect : commonDefectEntityList) {
                    // 是否过滤严重级别
                    if (severityFlag) {
                        if (defect.getSeverity() != defectQueryReq.getSeverity()) {
                            continue;
                        }
                    }
                    // 遗留未修复的告警
                    long createTime = defect.getCreateTime();
                    if (createTime <= endTime && DefectStatus.NEW.value() == defect.getStatus()) {
                        DefectDetailExtVO defectVO = new DefectDetailExtVO();
                        BeanUtils.copyProperties(defect, defectVO, "filePathname");
                        defectVO.setFilePathName(defect.getFilePath());
                        defectDetailExtVOs.add(defectVO);
                    }
                }
                Page<DefectDetailExtVO> defectDetailExtVoPage =
                        sortAndPage(pageNum, pageSize, sortField, sortType, defectDetailExtVOs);

                defectRspVO.setDefectList(defectDetailExtVoPage);

                /* 有告警的任务信息 */
                List<DefectDetailExtVO> records = defectDetailExtVoPage.getRecords();
                Map<Long, TaskDetailVO> taskDetailVoMap = Maps.newHashMap();
                if (CollectionUtils.isNotEmpty(records)) {
                    Set<Long> defectTaskIdSet =
                            records.stream().map(DefectDetailExtVO::getTaskId).collect(Collectors.toSet());
                    taskDetailVoMap =
                            taskDetailVoList.stream().filter(task -> defectTaskIdSet.contains(task.getTaskId()))
                                    .collect(Collectors
                                            .toMap(TaskDetailVO::getTaskId, Function.identity(), (k, v) -> v));
                }
                defectRspVO.setTaskDetailVoMap(taskDetailVoMap);
            }
        }
        return defectRspVO;
    }


    @NotNull
    private Map<Long, List<CommonDefectEntity>> getLongListDefectMap(List<CommonDefectEntity> commonDefectEntityList) {
        Map<Long, List<CommonDefectEntity>> defectMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(commonDefectEntityList)) {
            for (CommonDefectEntity commonDefectEntity : commonDefectEntityList) {
                /*----------------工蜂扫描特殊处理----------------*/
                if (gongfengFilterPathComponent.judgeGongfengFilter(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value(),
                        commonDefectEntity.getFilePath())) {
                    continue;
                }
                long taskId = commonDefectEntity.getTaskId();

                List<CommonDefectEntity> defectList = defectMap.computeIfAbsent(taskId, val -> Lists.newArrayList());
                defectList.add(commonDefectEntity);
            }
        }
        return defectMap;
    }

    @NotNull
    private <T> Page<T> sortAndPage(Integer pageNum, Integer pageSize, String sortField,
            Sort.Direction sortType, List<T> defectBaseVoList) {
        if (StringUtils.isEmpty(sortField)) {
            sortField = "createTime";
        }
        if (null == sortType) {
            sortType = Sort.Direction.ASC;
        }

        ListSortUtil.sort(defectBaseVoList, sortField, sortType.name());
        int total = defectBaseVoList.size();
        pageNum = pageNum == null || pageNum - 1 < 0 ? 0 : pageNum - 1;

        int pageSizeNum = 10;
        if (pageSize != null && pageSize >= 0) {
            pageSizeNum = pageSize;
        }

        int totalPageNum = 0;
        if (total > 0) {
            totalPageNum = (total + pageSizeNum - 1) / pageSizeNum;
        }
        int subListBeginIdx = pageNum * pageSizeNum;
        int subListEndIdx = subListBeginIdx + pageSizeNum;
        if (subListBeginIdx > total) {
            subListBeginIdx = 0;
        }
        defectBaseVoList = defectBaseVoList.subList(subListBeginIdx, subListEndIdx > total ? total : subListEndIdx);

        return new Page<>(total, pageNum + 1, pageSizeNum, totalPageNum, defectBaseVoList);
    }


    protected Set<String> getConditionFilterFiles(DefectQueryReqVO defectQueryReqVO) {
        return defectQueryReqVO.getFileList();
    }

    protected boolean checkIfMaskByPath(String filePathname, Set<String> condFileList) {
        return PathUtils.checkIfMaskByPath(filePathname, condFileList).getFirst();
    }

    /**
     * 用文件具体的代码仓库URL代替文件名展示
     *
     * @param defectBaseVO
     * @param codeRepoUrlMap
     */
    private void replaceFileNameWithURL(DefectBaseVO defectBaseVO, Map<String, CodeFileUrlEntity> codeRepoUrlMap) {
        String filePathname = trimWinPathPrefix(defectBaseVO.getFilePath());

        if (StringUtils.isBlank(filePathname)) {
            return;
        }

        int fileNameIndex = filePathname.lastIndexOf("/");
        if (fileNameIndex == -1) {
            fileNameIndex = filePathname.lastIndexOf("\\");
        }
        String fileName = filePathname.substring(fileNameIndex + 1);
        defectBaseVO.setFileName(fileName);
        CodeFileUrlEntity codeRepoUrlEntity = codeRepoUrlMap.get(filePathname);
        if (codeRepoUrlEntity != null) {
            defectBaseVO.setFilePath(codeRepoUrlEntity.getUrl());
            defectBaseVO.setFileVersion(codeRepoUrlEntity.getVersion());
        }
    }

    protected String trimWinPathPrefix(String filePath) {
        return filePath;
    }

    /**
     * 查询所有的告警ID
     *
     * @param taskId
     * @return
     */
    public Set<Long> queryIds(Long taskId, String toolName) {
        List<CommonDefectEntity> defectList = defectRepository.findIdsByTaskIdAndToolName(taskId, toolName);

        Set<Long> cidSet = new HashSet<>();
        if (CollectionUtils.isNotEmpty(defectList)) {
            defectList.forEach(defect -> cidSet.add(Long.valueOf(defect.getId())));
        }
        log.info("task [{}] cidSet size: {}", taskId, cidSet.size());
        return cidSet;
    }


    @Override
    public int getSubmitStepNum() {
        return ComConstants.Step4Cov.DEFECT_SYNS.value();
    }

    /**
     * 全选告警操作查询告警id
     *
     * @return 工具告警id
     */
    @Override
    public List<ToolDefectIdVO> queryDefectsByQueryCond(long taskId, DefectQueryReqVO reqVO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ToolDefectPageVO queryDefectsByQueryCondWithPage(long taskId, DefectQueryReqVO reqVO, Integer pageNum,
            Integer pageSize) {
        // DEFECT 已不走这个类
        log.warn("queryDefectsByQueryCond is unimplemented: taskId: {} reqVO: {}, pageNum: {}, pageSize: {}",
                taskId, reqVO, pageNum, pageSize);
        return new ToolDefectPageVO(taskId, null, Collections.emptyList(), 0L);
    }
}

