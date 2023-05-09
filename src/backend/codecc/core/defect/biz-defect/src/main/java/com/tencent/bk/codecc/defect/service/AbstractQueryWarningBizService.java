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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.service;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_REPO_USER_ID;
import static com.tencent.devops.common.auth.api.pojo.external.AuthExConstantsKt.KEY_CREATE_FROM;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildDefectSummaryRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintStatisticRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CLOCStatisticsDao;
import com.tencent.bk.codecc.defect.model.BuildDefectSummaryEntity;
import com.tencent.bk.codecc.defect.model.BuildDefectV2Entity;
import com.tencent.bk.codecc.defect.model.CodeCommentEntity;
import com.tencent.bk.codecc.defect.model.defect.DefectEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.utils.ConvertUtil;
import com.tencent.bk.codecc.defect.vo.CodeCommentVO;
import com.tencent.bk.codecc.defect.vo.CountDefectFileRequest;
import com.tencent.bk.codecc.defect.vo.DefectDetailVO;
import com.tencent.bk.codecc.defect.vo.DefectFilesInfoVO;
import com.tencent.bk.codecc.defect.vo.DefectInstanceVO;
import com.tencent.bk.codecc.defect.vo.GetFileContentSegmentReqVO;
import com.tencent.bk.codecc.defect.vo.SingleCommentVO;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectIdVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectPageVO;
import com.tencent.bk.codecc.defect.vo.ToolDefectRspVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.openapi.TaskDefectVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.api.UserMetaRestResource;
import com.tencent.bk.codecc.task.api.UserTaskRestResource;
import com.tencent.bk.codecc.task.vo.ListTaskNameCnRequest;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.auth.api.pojo.external.AuthExConstantsKt;
import com.tencent.devops.common.auth.api.util.AuthApiUtils;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.DateTimeUtils;
import com.tencent.devops.common.util.GitUtil;
import com.tencent.devops.common.util.ListSortUtil;
import com.tencent.devops.common.util.MD5Utils;
import com.tencent.devops.common.util.OkhttpUtils;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.util.Pair;

/**
 * 告警管理抽象类
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Slf4j
public abstract class AbstractQueryWarningBizService implements IQueryWarningBizService {

    protected static String EMPTY_FILE_CONTENT_TIPS = "无法获取代码片段。请确保你对代码库拥有权限，且该文件未从代码库中删除。";
    protected static String EMPTY_CONTENT_TOOL_TIPS = "此工具不支持代码片段查看";
    private static Logger logger = LoggerFactory.getLogger(AbstractQueryWarningBizService.class);
    @Autowired
    protected RabbitTemplate rabbitTemplate;
    @Autowired
    protected RedisTemplate<String, String> redisTemplate;
    @Autowired
    protected Client client;
    @Autowired
    protected TaskLogService taskLogService;
    @Autowired
    protected TaskLogOverviewService taskLogOverviewService;
    @Autowired
    protected PipelineScmService pipelineScmService;

    @Autowired
    protected BuildDefectV2Repository buildDefectV2Repository;
    @Autowired
    protected BuildSnapshotService buildSnapshotService;
    @Autowired
    protected BuildDefectSummaryRepository buildDefectSummaryRepository;
    @Value("${git.hosts:#{null}}")
    private String gitHosts;
    @Value("${github.repo.host:#{null}}")
    private String githubRepoHost;
    @Value("${github.repo.accessKey:#{null}}")
    private String githubRepoAccessKey;
    @Value("${github.repo.secretKey:#{null}}")
    private String githubRepoSecretKey;
    @Autowired
    private CLOCStatisticsDao clocStatisticsDao;
    @Autowired
    private LintStatisticRepository lintStatisticRepository;

    @Override
    public CommonDefectDetailQueryRspVO processGetFileContentSegmentRequest(long taskId, String userId,
            GetFileContentSegmentReqVO reqModel) {
        return new CommonDefectDetailQueryRspVO();
    }

    /**
     * 根据告警的开始行和结束行截取文件片段
     *
     * @param fileContent
     * @param beginLine
     * @param endLine
     * @param defectQueryRspVO
     * @return
     */
    protected String trimCodeSegment(
            String fileContent,
            int beginLine,
            int endLine,
            CommonDefectDetailQueryRspVO defectQueryRspVO
    ) {
        if (StringUtils.isBlank(fileContent)) {
            return EMPTY_FILE_CONTENT_TIPS;
        }

        String[] lines = fileContent.split("\n");
        if (lines.length <= 2000) {
            defectQueryRspVO.setTrimBeginLine(1);
            return fileContent;
        }

        int trimBeginLine = 1;
        int trimEndLine = lines.length;
        int limitLines = 500;
        if (beginLine - limitLines > 0) {
            trimBeginLine = beginLine - limitLines;
        }

        if (endLine + limitLines < lines.length) {
            trimEndLine = endLine + limitLines;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = trimBeginLine - 1; i < trimEndLine - 1; i++) {
            builder.append(lines[i] + "\n");
        }
        defectQueryRspVO.setTrimBeginLine(trimBeginLine);
        return builder.toString();
    }


    @Override
    public DeptTaskDefectRspVO processDeptTaskDefectReq(DeptTaskDefectReqVO deptTaskDefectReqVO) {
        return null;
    }


    @Override
    public Set<String> filterDefectByCondition(long taskId, List<?> defectList,
            Set<String> allChecker,
            DefectQueryReqVO queryCondObj,
            CommonDefectQueryRspVO defectQueryRspVO,
            List<String> toolNameSet) {
        return null;
    }

    @Override
    public ToolDefectRspVO processDeptDefectList(DeptTaskDefectReqVO defectQueryReq, Integer pageNum, Integer pageSize,
            String sortField, Sort.Direction sortType) {
        return null;
    }

    @Override
    public Object pageInit(String projectId, DefectQueryReqVO request) {
        return null;
    }

    /**
     * 排序并分页
     *
     * @param pageNum
     * @param pageSize
     * @param sortField
     * @param sortType
     * @param defectVOs
     * @param <T>
     * @return
     */
    protected <T> org.springframework.data.domain.Page<T> sortAndPage(int pageNum, int pageSize, String sortField,
            Sort.Direction sortType, List<T> defectVOs) {
        if (StringUtils.isEmpty(sortField)) {
            sortField = "severity";
        }
        if (null == sortType) {
            sortType = Sort.Direction.ASC;
        }

        // 严重程度要跟前端传入的排序类型相反
        if ("severity".equals(sortField)) {
            if (sortType.isAscending()) {
                sortType = Sort.Direction.DESC;
            } else {
                sortType = Sort.Direction.ASC;
            }
        }
        ListSortUtil.sort(defectVOs, sortField, sortType.name());
        int total = defectVOs.size();
        pageNum = pageNum - 1 < 0 ? 0 : pageNum - 1;
        pageSize = pageSize <= 0 ? 10 : pageSize;
        int subListBeginIdx = pageNum * pageSize;
        int subListEndIdx = subListBeginIdx + pageSize;
        if (subListBeginIdx > total) {
            subListBeginIdx = 0;
        }
        defectVOs = defectVOs.subList(subListBeginIdx, subListEndIdx > total ? total : subListEndIdx);

        //封装分页类
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(sortType, sortField));
        return new PageImpl<>(defectVOs, pageable, total);
    }

    /**
     * 判断是否匹配前端传入的状态条件，不匹配返回true,匹配返回false
     *
     * @param condStatusList
     * @param status
     * @return
     */
    protected boolean isNotMatchStatus(Set<String> condStatusList, int status) {
        boolean notMatchStatus = true;
        for (String condStatus : condStatusList) {
            // 查询条件是待修复，且告警状态是NEW
            if (ComConstants.DefectStatus.NEW.value() == Integer.parseInt(condStatus)
                    && ComConstants.DefectStatus.NEW.value() == status) {
                notMatchStatus = false;
                break;
            }
            // 查询条件是已修复或已忽略，且告警状态是匹配
            else if (ComConstants.DefectStatus.NEW.value() < Integer.parseInt(condStatus)
                    && (Integer.parseInt(condStatus) & status) > 0) {
                notMatchStatus = false;
                break;
            }
        }

        return notMatchStatus;
    }

    /**
     * 解析告警实例的跟踪事件，转换为Trace对象，并获取文件信息
     *
     * @param fileInfoMap
     * @param trace
     */
    protected void parseTrace(Map<String, DefectFilesInfoVO> fileInfoMap, DefectInstanceVO.Trace trace) {
        if (trace.getLinkTrace() != null) {
            for (int i = 0; i < trace.getLinkTrace().size(); i++) {
                DefectInstanceVO.Trace linkTrace = trace.getLinkTrace().get(i);
                if (linkTrace.getTraceNum() == null) {
                    linkTrace.setTraceNum(i + 1);
                }
                parseTrace(fileInfoMap, linkTrace);
            }
        }

        String fileName = trace.getFilePath();
        int lineNumber = trace.getLineNum();

        String md5 = trace.getFileMd5();
        if (StringUtils.isEmpty(md5)) {
            md5 = MD5Utils.getMD5(fileName);
            trace.setFileMd5(md5);
        }

        DefectFilesInfoVO fileInfo = fileInfoMap.get(md5);
        if (fileInfo == null) {
            fileInfo = new DefectFilesInfoVO();
            fileInfo.setFilePath(fileName);
            fileInfo.setFileMd5(md5);
            fileInfo.setMinLineNum(lineNumber);
            fileInfo.setMaxLineNum(lineNumber);
            fileInfoMap.put(md5, fileInfo);
        } else {
            if (lineNumber < fileInfo.getMinLineNum()) {
                fileInfo.setMinLineNum(lineNumber);
            } else if (lineNumber > fileInfo.getMaxLineNum()) {
                fileInfo.setMaxLineNum(lineNumber);
            }
        }
    }

    /**
     * 获取提单步骤的值，子类必须实现这个方法
     * 普通工具有4个分析步骤：1：代码下载，2、代码下载；3：代码扫描，4：代码缺陷提交
     * Klocwork/Coverity有5个分析步骤：1：上传，2：排队状态，3、分析中；4：缺陷提交，5：提单
     *
     * @return
     */
    public abstract int getSubmitStepNum();


    protected CodeCommentVO convertCodeComment(CodeCommentEntity codeCommentEntity) {
        //设置告警评论
        CodeCommentVO codeCommentVO = new CodeCommentVO();
        BeanUtils.copyProperties(codeCommentEntity, codeCommentVO);
        codeCommentVO.setCommentList(codeCommentEntity.getCommentList().stream().
                map(singleCommentEntity -> {
                    SingleCommentVO singleCommentVO = new SingleCommentVO();
                    BeanUtils.copyProperties(singleCommentEntity, singleCommentVO);
                    return singleCommentVO;
                }).collect(Collectors.toList())
        );
        return codeCommentVO;
    }

    /**
     * 根据标志修改时间与最近一次分析时间比较来判断告警是否是被标记后仍未被修改
     *
     * @param mark
     * @param markTime
     * @param lastAnalyzeTime
     * @return
     */
    public Integer convertMarkStatus(Integer mark, Long markTime, Long lastAnalyzeTime) {
        if (mark != null && mark == ComConstants.MarkStatus.MARKED.value() && markTime != null) {
            if (lastAnalyzeTime != null && markTime < lastAnalyzeTime) {
                mark = ComConstants.MarkStatus.NOT_FIXED.value();
            }
        }
        return mark;
    }

    /**
     * 多条件批量获取任务详情列表
     *
     * @param deptTaskDefectReqVO reqObj
     * @return list
     */
    protected List<TaskDetailVO> getTaskDetailVoList(DeptTaskDefectReqVO deptTaskDefectReqVO) {
        QueryTaskListReqVO queryTaskListReqVO = new QueryTaskListReqVO();
        queryTaskListReqVO.setTaskIds(deptTaskDefectReqVO.getTaskIds());
        queryTaskListReqVO.setBgId(deptTaskDefectReqVO.getBgId());
        queryTaskListReqVO.setDeptIds(deptTaskDefectReqVO.getDeptIds());
        queryTaskListReqVO.setCreateFrom(deptTaskDefectReqVO.getCreateFrom());
        queryTaskListReqVO.setStatus(ComConstants.Status.ENABLE.value());

        Result<List<TaskDetailVO>> batchGetTaskListResult =
                client.get(ServiceTaskRestResource.class).batchGetTaskList(queryTaskListReqVO);
        return batchGetTaskListResult.getData();
    }

    protected String getFileContent(long taskId, String projectId, String userId, String url, String repoId,
            String relPath, String revision, String branch, String subModule) {
        Pair<String, String> pair = getTaskCreateFrom(projectId, taskId);
        String createFrom = pair.getFirst();
        String realProjectId = pair.getSecond();

        // 先预设是github仓库，从github仓库缓存读取
        String ref = StringUtils.isBlank(revision) ? branch : revision;
        String content = tryGetGithubContent(url, ref, relPath, userId, projectId);

        // 预设成功，直接返回
        if (content != null) {
            return content;
        }

        // 判断是否是oauth
        if (isOauth(url, branch, createFrom, realProjectId)) {
            String oauthUserId = userId;
            if (!realProjectId.startsWith("CUSTOMPROJ_")
                    && AuthApiUtils.INSTANCE.isAdminMember(redisTemplate, userId)) {
                Result<TaskDetailVO> result = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
                oauthUserId = result.getData().getUpdatedBy();
            }

            content = getOathFileContent(userId, oauthUserId, url, relPath, revision, branch, realProjectId);
        } else if (projectId.startsWith("git_") || projectId.startsWith("github_")) {
            content = pipelineScmService.getStreamFileContent(projectId, userId, url, relPath, revision, branch);
        } else {
            try {
                content = pipelineScmService.getFileContent(
                        taskId, repoId, relPath, revision, branch, subModule, createFrom);
            } catch (Exception e) {
                if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(createFrom)
                        && isMatchGitHost(url)) {
                    content = getOathFileContent(userId, userId, url, relPath, revision, branch, realProjectId);
                } else {
                    throw e;
                }
            }
        }

        return content;
    }


    private String getOathFileContent(String userId, String oauthUserId, String url, String relPath, String revision,
            String branch, String realProjectId) {
        String content = pipelineScmService.getFileContentOauth(oauthUserId, GitUtil.INSTANCE.getProjectName(url),
                relPath, (revision != null ? revision : branch));

        // DUPC 和 CCN 传进来的 projectId 是空的
        if (StringUtils.isBlank(content)) {
            content = pipelineScmService.getStreamFileContent(
                    realProjectId, userId, url, relPath, revision, branch);
        }
        return content;
    }

    private String tryGetGithubContent(String url, String ref, String relPath, String userId, String projectId) {
        if (StringUtils.isEmpty(url) || !url.contains("github.com")) {
            return null;
        }

        try {
            String projectName = GitUtil.INSTANCE.getProjectName(url);
            String[] arr = projectName.split("/");
            String owner = arr[0];
            String repoName = arr[1];
            String repoUrl = String.format("http://%s/git/%s/%s/raw/%s/%s?hub_type=github&owner=%s",
                    githubRepoHost,
                    URLEncoder.encode(projectId, "UTF-8"),
                    URLEncoder.encode(repoName, "UTF-8"),
                    URLEncoder.encode(ref, "UTF-8"),
                    URLEncoder.encode(relPath, "UTF-8"),
                    URLEncoder.encode(owner, "UTF-8"));

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Platform " + new String(
                    Base64.getEncoder().encode((githubRepoAccessKey + ":" + githubRepoSecretKey).getBytes())));
            headers.put(AUTH_HEADER_DEVOPS_REPO_USER_ID, userId);
            String content = OkhttpUtils.INSTANCE.doGet(repoUrl, headers);
            logger.info("get from bkrepo of github: {}, {}", projectId, projectName);
            return content;
        } catch (Exception ex) {
            logger.error("fail to get github content from repo " + url, ex);
            return null;
        }
    }

    private boolean isMatchGitHost(String url) {
        if (StringUtils.isEmpty(gitHosts)) {
            return false;
        }
        List<String> gitHostsList =
                Arrays.stream(gitHosts.split(",")).map(String::trim).collect(Collectors.toList());
        boolean matchGitHost = false;
        for (String gitHost : gitHostsList) {
            if (StringUtils.isNotEmpty(url) && url.contains(gitHost)) {
                matchGitHost = true;
                break;
            }
        }
        return matchGitHost;
    }


    private boolean isOauth(String url, String branch, String createFrom, String realProjectId) {
        // 判断是否是可以走oauth的代码库类型
        boolean matchGitHost = isMatchGitHost(url);

        /*
         * 1. 是走oauth的代码库类型
         * 2. 不是pre merge
         * 3. 不是工蜂扫描
         * 4. 不是CUSTOMPROJ_xxx项目
         * 全部符合则走oauth
         */
        if (StringUtils.isNotBlank(url) && matchGitHost) {
            if (!ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(createFrom)
                    || realProjectId.startsWith("CUSTOMPROJ_")) {
                return true;
            }
        }

        return false;
    }

    private Pair<String, String> getTaskCreateFrom(String projectId, long taskId) {
        String createFrom = "";
        String realProjectId = projectId;
        // get from redis first
        Object value = redisTemplate.opsForHash().get(AuthExConstantsKt.PREFIX_TASK_INFO + taskId, KEY_CREATE_FROM);
        if (value instanceof String) {
            createFrom = (String) value;
        }

        // get from remote
        if (StringUtils.isBlank(createFrom) || StringUtils.isBlank(projectId)) {
            Result<TaskDetailVO> result = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
            if (result.isNotOk() || result.getData() == null) {
                logger.error("fail to get task info by id, taskId: {} | err: {}", taskId, result.getMessage());
                throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
            }
            createFrom = result.getData().getCreateFrom();
            realProjectId = result.getData().getProjectId();
        }

        return Pair.of(createFrom, realProjectId);
    }

    /**
     * 根据快照Id筛出当次告警Id集
     *
     * @param taskId
     * @param toolNameSet
     * @param buildId
     * @return
     */
    @NotNull
    protected Set<String> getDefectIdsByBuildId(long taskId, List<String> toolNameSet, String buildId) {
        if (StringUtils.isEmpty(buildId)) {
            return Sets.newHashSet();
        }

        Map<String, Boolean> commitResult =
                taskLogService.defectCommitSuccess(taskId, toolNameSet, buildId, getSubmitStepNum());
        // 筛出成功执行的工具
        Set<String> successTools = commitResult.entrySet().stream().filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(successTools)) {
            return Sets.newHashSet();
        }

        List<BuildDefectV2Entity> retList =
                buildDefectV2Repository.findByTaskIdAndBuildIdAndToolNameIn(taskId, buildId, successTools);

        return retList.stream().map(BuildDefectV2Entity::getDefectId).collect(Collectors.toSet());
    }

    /**
     * 最新构建该告警的状态
     *
     * @param taskId
     * @param buildId
     * @param defectId
     * @param respVO
     */
    protected void setDefectStatusOnLastBuild(
            long taskId,
            String buildId,
            String defectId,
            CommonDefectDetailQueryRspVO respVO
    ) {
        if (StringUtils.isEmpty(buildId)) {
            return;
        }

        Pair<String, Boolean> kv = buildSnapshotService.getDefectFixedStatusOnLastBuild(taskId, buildId, defectId);
        respVO.setLastBuildNumOfSameBranch(kv.getFirst());
        respVO.setDefectIsFixedOnLastBuildNumOfSameBranch(kv.getSecond());
    }

    @Override
    public Long countNewDefectFile(CountDefectFileRequest request) {
        return 0L;
    }

    /**
     * 校验构建id是无效的吗
     *
     * @param taskId
     * @param buildId
     * @return 无效返回true
     */
    protected boolean isInvalidBuildId(long taskId, String buildId) {
        if (StringUtils.isEmpty(buildId)) {
            return false;
        }

        BuildDefectSummaryEntity buildDefectSummary =
                buildDefectSummaryRepository.findFirstByTaskIdAndBuildId(taskId, buildId);

        return buildDefectSummary == null;
    }

    protected CommonDefectQueryRspVO getInvalidBuildIdResp() {
        return new CommonDefectQueryRspVO(0L, "", "",
                "CodeCC快照功能已升级，你使用的是升级前的构建，麻烦重新检查一次。");
    }

    @Override
    public List<ToolDefectIdVO> queryDefectsByQueryCond(long taskId, DefectQueryReqVO reqVO) {
        log.warn("queryDefectsByQueryCond is unimplemented: taskId: {} reqVO: {}", taskId, reqVO);
        return Collections.emptyList();
    }

    @Override
    public ToolDefectPageVO queryDefectsByQueryCondWithPage(long taskId, DefectQueryReqVO reqVO, Integer pageNum,
            Integer pageSize) {
        log.warn("queryDefectsByQueryCond is unimplemented: taskId: {} reqVO: {}, pageNum: {}, pageSize: {}",
                taskId, reqVO, pageNum, pageSize);
        return new ToolDefectPageVO(taskId, null, Collections.emptyList(), 0L);
    }

    /**
     * 构造common类工具获取文件内容的入参
     *
     * @param entity
     * @param fileInfoMap
     * @param defectInstances
     * @return
     */
    protected DefectDetailVO covertToCommonDefectFileContentRequest(
            LintDefectV2Entity entity,
            Map<String, DefectFilesInfoVO> fileInfoMap,
            List<DefectInstanceVO> defectInstances
    ) {
        DefectDetailVO retVO = new DefectDetailVO();
        retVO.setTaskId(entity.getTaskId());
        retVO.setToolName(entity.getToolName());
        retVO.setStreamName(entity.getStreamName());
        retVO.setFileInfoMap(fileInfoMap);
        retVO.setId(entity.getId());
        retVO.setPlatformBuildId(entity.getPlatformBuildId());
        retVO.setPlatformProjectId(entity.getPlatformProjectId());

        if (ToolPattern.COVERITY.name().equalsIgnoreCase(entity.getToolName())) {
            retVO.setDefectInstances(defectInstances);
            retVO.setMessage(entity.getMessage());
        }

        return retVO;
    }

    protected Map<Long, String> getTaskNameCnMap(
            DefectQueryReqVO request,
            List<? extends DefectEntity> defectList
    ) {
        if (!Boolean.TRUE.equals(request.getShowTaskNameCn()) || CollectionUtils.isEmpty(defectList)) {
            return Maps.newHashMap();
        }

        List<Long> taskIdList = defectList.stream().map(DefectEntity::getTaskId).collect(Collectors.toList());

        return client.get(UserTaskRestResource.class)
                .listTaskNameCn(new ListTaskNameCnRequest(taskIdList))
                .getData();
    }

    protected List<MetadataVO> getCodeLangMetadataVoList() {
        Result<Map<String, List<MetadataVO>>> metaDataResult =
                client.get(UserMetaRestResource.class).metadatas(ComConstants.KEY_CODE_LANG);
        if (metaDataResult.isNotOk() || metaDataResult.getData() == null) {
            log.error("meta data result is empty! meta data type {}", ComConstants.KEY_CODE_LANG);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return metaDataResult.getData().get(ComConstants.KEY_CODE_LANG);
    }

    /**
     * 批量获取最近分析日志
     *
     * @param taskIdSet 任务ID集合
     * @param toolName 指定工具名
     * @return map
     */
    protected Map<Long, TaskLogVO> getTaskLogVoMap(Set<Long> taskIdSet, String toolName) {
        Map<Long, TaskLogVO> taskLogVoMap = Maps.newHashMap();
        if (CollectionUtils.isEmpty(taskIdSet)) {
            return taskLogVoMap;
        }

        List<TaskLogVO> taskLogVoList = taskLogService.batchTaskLogList(taskIdSet, toolName);
        if (CollectionUtils.isNotEmpty(taskLogVoList)) {
            taskLogVoList.forEach(taskLogVO -> taskLogVoMap.put(taskLogVO.getTaskId(), taskLogVO));
        }

        return taskLogVoMap;
    }

    /**
     * 赋值任务最近分析状态
     *
     * @param taskId 任务ID
     * @param taskLogVoMap 最新分析日志
     * @param taskDefectVO 告警统计对象
     */
    protected void setAnalyzeDateStatus(long taskId, Map<Long, TaskLogVO> taskLogVoMap, TaskDefectVO taskDefectVO) {
        TaskLogVO taskLogVO = taskLogVoMap.get(taskId);
        String analyzeDateStr = "";
        if (taskLogVO != null) {
            int currStep = taskLogVO.getCurrStep();
            int flag = taskLogVO.getFlag();
            long analyzeStartTime = taskLogVO.getStartTime();
            String currStepStr = ConvertUtil.convertStep4Cov(currStep);
            String stepFlag = ConvertUtil.getStepFlag(flag);
            // 2019-10-11 分析成功
            analyzeDateStr = DateTimeUtils.second2DateString(analyzeStartTime) + " " + currStepStr + stepFlag;
        }
        taskDefectVO.setAnalyzeDate(analyzeDateStr);
    }
}
