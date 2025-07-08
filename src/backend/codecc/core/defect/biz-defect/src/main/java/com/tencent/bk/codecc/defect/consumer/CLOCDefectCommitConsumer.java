/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 Tencent. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.consumer;

import com.alibaba.fastjson.JSONReader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.codecc.defect.constant.DefectMessageCode;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CLOCDefectRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.CLOCDefectDao;
import com.tencent.bk.codecc.defect.model.DefectJsonFileEntity;
import com.tencent.bk.codecc.defect.model.HeadFileEntity;
import com.tencent.bk.codecc.defect.model.defect.CLOCDefectEntity;
import com.tencent.bk.codecc.defect.model.ignore.IgnoreCommentDefectModel;
import com.tencent.bk.codecc.defect.model.ignore.IgnoreCommentDefectSubModel;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.pojo.FileMD5TotalModel;
import com.tencent.bk.codecc.defect.service.IHeadFileService;
import com.tencent.bk.codecc.defect.service.IIgnoreDefectService;
import com.tencent.bk.codecc.defect.service.statistic.CLOCDefectStatisticService;
import com.tencent.bk.codecc.defect.vo.CLOCLanguageVO;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadCLOCStatisticVO;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import com.tencent.devops.common.util.PathUtils;
import com.tencent.devops.common.util.ThreadUtils;
import com.tencent.devops.common.web.mq.ConstantsKt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DUPC告警提交消息队列的消费者
 *
 * @version V1.0
 * @date 2020/3/12
 */
@Component("clocDefectCommitConsumer")
@Slf4j
public class CLOCDefectCommitConsumer extends AbstractDefectCommitConsumer {
    @Autowired
    private CLOCDefectDao clocDefectDao;
    @Autowired
    private CLOCDefectRepository clocDefectRepository;
    @Autowired
    private CLOCDefectStatisticService clocDefectStatisticService;
    @Autowired
    private IIgnoreDefectService iIgnoreDefectService;
    @Autowired
    private IHeadFileService iHeadFileService;

    private static final Integer MAX_PER_SIZE = 10000;

    @Override
    protected boolean uploadDefects(
            CommitDefectVO commitDefectVO,
            Map<String, ScmBlameVO> fileChangeRecordsMap,
            Map<String, RepoSubModuleVO> codeRepoIdMap
    ) {
        long taskId = commitDefectVO.getTaskId();
        String streamName = commitDefectVO.getStreamName();
        String toolName = commitDefectVO.getToolName();
        String buildId = commitDefectVO.getBuildId();

        // 判断本次是增量还是全量扫描
        ToolBuildStackEntity toolBuildStackEntity =
                toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        boolean isFullScan = toolBuildStackEntity == null || toolBuildStackEntity.isFullScan();
        //区分增量与全量进行更新
        if (isFullScan) {
            fullUploadDefects(taskId, buildId, toolName, streamName, toolBuildStackEntity);
        } else {
            incUploadDefects(taskId, buildId, toolName, streamName, toolBuildStackEntity);
        }
        //提单后操作，统计等
        afterDefeatUpdate(taskId, buildId, toolName, streamName);
        return true;
    }

    /**
     * 全量更新
     * @param taskId
     * @param buildId
     * @param toolName
     * @param streamName
     * @param toolBuildStackEntity
     */
    private void fullUploadDefects(Long taskId, String buildId, String toolName, String streamName,
                                   ToolBuildStackEntity toolBuildStackEntity) {
        //更新忽略信息，具体如下：
        /*
          全量扫描，先将map字段置空，然后再逐个更新
         */
        String ignoreListJson = scmJsonComponent.loadRawIgnores(streamName, toolName, buildId);
        log.info("upsert ignore comment defect model, task id: {}, tool name: {}, build id: {}",
                taskId, toolName, buildId);
        iIgnoreDefectService.deleteIgnoreDefectMap(taskId, null);
        if (StringUtils.isNotBlank(ignoreListJson)) {
            upsertIgnoreDefectInfo(taskId, ignoreListJson);
        }

        //更新头文件信息，具体如下：
        /*
           全量扫描，将set全部删除后更新
         */
        String headFileJson = scmJsonComponent.loadRawHeadFiles(streamName, toolName, buildId);
        log.info("upsert head file model, task id: {}, tool name: {}, build id: {}",
                taskId, toolName, buildId);
        iHeadFileService.deleteHeadFileInfo(taskId, null);
        if (StringUtils.isNotBlank(headFileJson)) {
            upsertHeadFileInfo(taskId, headFileJson);
        }

        // 全量告警将当前任务所有告警设置为失效
        log.info("start to disable all {} defect entity, taskId: {} | buildId: {} | stream Name: {}",
                toolName, taskId, buildId, streamName);
        clocDefectDao.batchDisableClocInfo(taskId, toolName);

        //分批读取
        Map<String, String> filePathMap = getFilePathMap(buildId, toolName, streamName);
        batchUpdateWhenFullUpload(taskId, buildId, toolName, streamName, filePathMap);
    }

    /**
     * 全量更新 - 分批更新缺陷
     * @param taskId
     * @param buildId
     * @param toolName
     * @param streamName
     * @param filePathMap
     */
    private void batchUpdateWhenFullUpload(Long taskId, String buildId, String toolName, String streamName,
                                           Map<String, String> filePathMap) {
        String fileIndex = scmJsonComponent.getDefectFileIndex(streamName, toolName, buildId);
        if (StringUtils.isEmpty(fileIndex)) {
            log.warn("Can not find raw defect file:{}, {}, {}", streamName, toolName, buildId);
            throw new CodeCCException(DefectMessageCode.DEFECT_FILE_NOT_FOUND, null, String.format(
                    "找不到的告警文件: %s, %s, %s", streamName, toolName, buildId), null);
        }
        File defectFile = new File(fileIndex);
        if (!defectFile.exists()) {
            log.warn("文件不存在: {}", fileIndex);
            throw new CodeCCException(DefectMessageCode.DEFECT_FILE_NOT_FOUND, null, String.format("找不到告警文件: %s",
                    fileIndex), null);
        }
        try (FileInputStream fileInputStream = new FileInputStream(defectFile);
             InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
             JSONReader reader = new JSONReader(inputStreamReader)) {
            reader.startObject();
            while (reader.hasNext()) {
                String keys = reader.readString();
                if (keys.equals("defects")) {
                    reader.startArray();
                    List<CLOCDefectEntity> clocDefectEntityList = new LinkedList<>();
                    while (reader.hasNext()) {
                        clocDefectEntityList.add(JsonUtil.INSTANCE.to(reader.readString(), CLOCDefectEntity.class));
                        if (clocDefectEntityList.size() > MAX_PER_SIZE) {
                            fillDefectInfo(clocDefectEntityList, filePathMap, true);
                            batchUpsertClocInfo(taskId, toolName, streamName, clocDefectEntityList);
                            clocDefectEntityList = new LinkedList<>();
                            ThreadUtils.sleep(100);
                        }
                    }
                    if (clocDefectEntityList.size() > 0) {
                        fillDefectInfo(clocDefectEntityList, filePathMap, true);
                        batchUpsertClocInfo(taskId, toolName, streamName, clocDefectEntityList);
                    }
                    reader.endArray();
                } else {
                    reader.readObject();
                }
            }
            reader.endObject();
        } catch (IOException e) {
            log.warn("Read defect file exception: {}", fileIndex, e);
        }
    }

    /**
     * 增量更新
     * @param taskId
     * @param buildId
     * @param toolName
     * @param streamName
     * @param toolBuildStackEntity
     */
    private void incUploadDefects(Long taskId, String buildId, String toolName, String streamName,
                                  ToolBuildStackEntity toolBuildStackEntity) {
        // 读取原生（未经压缩）告警文件
        String defectListJson = scmJsonComponent.loadRawDefects(streamName, toolName, buildId);
        DefectJsonFileEntity<CLOCDefectEntity> defectJsonFileEntity =
                JsonUtil.INSTANCE.to(defectListJson, new TypeReference<DefectJsonFileEntity<CLOCDefectEntity>>() {
                });
        List<CLOCDefectEntity> clocDefectEntityList = defectJsonFileEntity.getDefects();

        Map<String, String> filePathMap = getFilePathMap(buildId, toolName, streamName);

        Set<String> currentFileSet = fillDefectInfo(clocDefectEntityList, filePathMap, false);

        //更新忽略信息，具体如下：
        /*
           增量扫描，先将上报变化的文件和删除的文件置空，然后再逐个更新
         */
        String ignoreListJson = scmJsonComponent.loadRawIgnores(streamName, toolName, buildId);
        log.info("upsert ignore comment defect model, task id: {}, tool name: {}, build id: {}",
                taskId, toolName, buildId);
        if (CollectionUtils.isNotEmpty(toolBuildStackEntity.getDeleteFiles())) {
            currentFileSet.addAll(toolBuildStackEntity.getDeleteFiles());
        }
        iIgnoreDefectService.deleteIgnoreDefectMap(taskId, currentFileSet);
        if (StringUtils.isNotBlank(ignoreListJson)) {
            upsertIgnoreDefectInfo(taskId, ignoreListJson);
        }

        //更新头文件信息，具体如下：
        /*
          增量扫描，将delete文件删除后，更新新增的
         */
        String headFileJson = scmJsonComponent.loadRawHeadFiles(streamName, toolName, buildId);
        log.info("upsert head file model, task id: {}, tool name: {}, build id: {}",
                taskId, toolName, buildId);
        if (CollectionUtils.isNotEmpty(toolBuildStackEntity.getDeleteFiles())) {
            iHeadFileService.deleteHeadFileInfo(taskId, new HashSet<>(toolBuildStackEntity.getDeleteFiles()));
        }
        if (StringUtils.isNotBlank(headFileJson)) {
            upsertHeadFileInfo(taskId, headFileJson);
        }

        // 增量告警，获取删除文件列表，设置失效位
        if (CollectionUtils.isNotEmpty(toolBuildStackEntity.getDeleteFiles())) {
            log.info("start to disable deleted {} defect entity, taskId: {} | buildId: {} | stream Name: {}",
                    toolName, taskId, buildId, streamName);
            clocDefectDao.batchDisableClocInfoByFileName(taskId, toolName, toolBuildStackEntity.getDeleteFiles());
        }
        //更新
        batchUpsertClocInfo(taskId, toolName, streamName, clocDefectEntityList);
    }

    private Map<String, String> getFilePathMap(String buildId, String toolName, String streamName) {
        FileMD5TotalModel md5File = scmJsonComponent.loadFileMD5(streamName, toolName, buildId);
        Map<String, String> filePathMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(md5File.getFileList())) {
            md5File.getFileList().forEach(fileMD5SingleModel -> {
                if (StringUtils.isNotBlank(fileMD5SingleModel.getFileOriginalPath())) {
                    filePathMap.put(fileMD5SingleModel.getFileOriginalPath(), fileMD5SingleModel.getFileRelPath());
                }
            });
        }
        return filePathMap;
    }

    /**
     * 更新忽略文件信息
     *
     * @param taskId
     * @param ignoreListJson
     */
    private void upsertIgnoreDefectInfo(Long taskId, String ignoreListJson) {
        if (StringUtils.isNotBlank(ignoreListJson)) {
            Map<String, List<IgnoreCommentDefectSubModel>> ignoreJsonFileEntity = JsonUtil.INSTANCE.to(
                    ignoreListJson, new TypeReference<Map<String, List<IgnoreCommentDefectSubModel>>>() {
                    });
            IgnoreCommentDefectModel ignoreCommentDefectModel = new IgnoreCommentDefectModel();
            ignoreCommentDefectModel.setTaskId(taskId);
            ignoreCommentDefectModel.setIgnoreDefectMap(ignoreJsonFileEntity);
            iIgnoreDefectService.upsertIgnoreDefectInfo(ignoreCommentDefectModel);
        }
    }

    /**
     * 更新头文件信息
     *
     * @param taskId
     * @param headFileJson
     */
    private void upsertHeadFileInfo(Long taskId, String headFileJson) {
        if (StringUtils.isNotBlank(headFileJson)) {
            Set<String> headFileSet = JsonUtil.INSTANCE.to(
                    headFileJson, new TypeReference<Set<String>>() {
                    });
            HeadFileEntity headFileEntity = new HeadFileEntity();
            headFileEntity.setTaskId(taskId);
            headFileEntity.setHeadFileSet(headFileSet);
            iHeadFileService.upsertHeadFileInfo(headFileEntity);
        }
    }

    /**
     * 更新代码行缺陷
     *
     * @param taskId
     * @param toolName
     * @param streamName
     * @param clocDefectEntityList
     */
    private void batchUpsertClocInfo(Long taskId, String toolName, String streamName,
                                     List<CLOCDefectEntity> clocDefectEntityList) {
        if (CollectionUtils.isNotEmpty(clocDefectEntityList)) {
            Long currentTime = System.currentTimeMillis();
            clocDefectEntityList.forEach(clocDefectEntity -> {
                clocDefectEntity.setTaskId(taskId);
                clocDefectEntity.setToolName(toolName);
                clocDefectEntity.setStreamName(streamName);
                clocDefectEntity.setCreatedDate(currentTime);
                clocDefectEntity.setUpdatedDate(currentTime);
            });
        }
        //告警详情再upsert
        clocDefectDao.batchUpsertClocInfo(clocDefectEntityList);
    }

    private void afterDefeatUpdate(Long taskId, String buildId, String toolName, String streamName) {
        log.info("start to insert {} defect statistic, taskId: {} | buildId: {} | streamName: {}",
                toolName, taskId, buildId, streamName);
        List<CLOCDefectEntity> clocDefectEntityList;
        // 获取全量告警记录，用于统计信息
        if (Tool.SCC.name().equals(toolName)) {
            clocDefectEntityList = clocDefectRepository.findByTaskIdAndToolNameInAndStatusIsNot(
                    taskId,
                    Collections.singletonList(toolName),
                    "DISABLED");
        } else {
            clocDefectEntityList = clocDefectRepository.findByTaskIdAndToolNameInAndStatusIsNot(
                    taskId,
                    Arrays.asList(toolName, null),
                    "DISABLED");
        }
        //上报统计信息
        UploadCLOCStatisticVO uploadCLOCStatisticVO = new UploadCLOCStatisticVO();
        uploadCLOCStatisticVO.setTaskId(taskId);
        uploadCLOCStatisticVO.setToolName(toolName);
        uploadCLOCStatisticVO.setStreamName(streamName);

        Map<String, List<CLOCDefectEntity>> clocLanguageMap = clocDefectEntityList.stream()
                .collect(Collectors.groupingBy(CLOCDefectEntity::getLanguage));
        // 获取路径黑/白名单
        TaskDetailVO taskVO = thirdPartySystemCaller.getTaskInfo(streamName);
        Set<String> filterPaths = filterPathService.getFilterPaths(taskVO, toolName);
        Set<String> whitePaths = buildService.getWhitePaths(buildId, taskVO);

        List<String> pathMaskDefectList = new ArrayList<>();
        List<CLOCLanguageVO> languageVOList = clocLanguageMap.entrySet()
                .stream()
                .map(stringListEntry -> {
                    CLOCLanguageVO clocLanguageVO = new CLOCLanguageVO();
                    clocLanguageVO.setLanguage(stringListEntry.getKey());
                    // 判断过滤路径，被屏蔽的记录需要更新状态
                    List<CLOCDefectEntity> clocInfoVOS
                            = checkMaskByPath(stringListEntry.getValue(),
                            filterPaths,
                            whitePaths,
                            pathMaskDefectList);

                    clocLanguageVO.setCodeSum(clocInfoVOS.stream()
                            .map(CLOCDefectEntity::getCode)
                            .reduce(Long::sum)
                            .orElse(0L));
                    clocLanguageVO.setBlankSum(clocInfoVOS.stream()
                            .map(CLOCDefectEntity::getBlank)
                            .reduce(Long::sum)
                            .orElse(0L));
                    clocLanguageVO.setCommentSum(clocInfoVOS.stream()
                            .map(CLOCDefectEntity::getComment)
                            .reduce(Long::sum)
                            .orElse(0L));
                    if (toolName.equals(Tool.SCC.name())) {
                        clocLanguageVO.setEfficientCommentSum(clocInfoVOS.stream()
                                .map(CLOCDefectEntity::getEfficientComment)
                                .reduce(Long::sum)
                                .orElse(0L));
                    }
                    return clocLanguageVO;
                }).collect(Collectors.toList());

        // 路径屏蔽的告警需要置为失效
        if (CollectionUtils.isNotEmpty(pathMaskDefectList)) {
            clocDefectDao.batchDisableClocInfoByFileName(taskId, toolName, pathMaskDefectList);
        }

        uploadCLOCStatisticVO.setToolName(toolName);
        uploadCLOCStatisticVO.setLanguageCodeList(languageVOList);
        uploadCLOCStatisticVO.setLanguages(clocDefectEntityList
                .stream()
                .map(CLOCDefectEntity::getLanguage)
                .distinct()
                .collect(Collectors.toList()));
        clocDefectStatisticService.statistic(uploadCLOCStatisticVO, clocLanguageMap, buildId, streamName);

        // 更新告警快照基准构建ID
        toolBuildInfoDao.updateDefectBaseBuildId(taskId, toolName, buildId);
    }

    /**
     * 路径黑/白名单检查
     * 只有存在于 黑名单和白名单 交集中的文件才被记录
     *
     * @param clocDefectEntities 被检测告警
     * @param filterPath 路径黑名单
     * @param pathMaskDefectList 被路径屏蔽的文件，记录下来之后需要更新失效位
     * @param whitePaths 路径白名单
     */
    private List<CLOCDefectEntity> checkMaskByPath(List<CLOCDefectEntity> clocDefectEntities,
                                                   Set<String> filterPath,
                                                   Set<String> whitePaths,
                                                   List<String> pathMaskDefectList) {
        return clocDefectEntities.stream().filter(it -> {
            // 命中黑名单 或 没有命中白名单
            if (PathUtils.checkIfMaskByPath(it.getFileName(), filterPath).getFirst()
                    || (CollectionUtils.isNotEmpty(whitePaths)
                    && !PathUtils.checkIfMaskByPath(it.getFileName(), whitePaths).getFirst())) {
                pathMaskDefectList.add(it.getFileName());
                return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    private Set<String> fillDefectInfo(
        List<CLOCDefectEntity> defectList, Map<String, String> filePathMap, Boolean isFullScan) {
        Set<String> currentFileSet = new HashSet<>();
        defectList.forEach(defectEntity -> {
            defectEntity.setRelPath(filePathMap.get(defectEntity.getFileName()));
            //如果是增量的话，需要统计变化量
            if (!isFullScan) {
                currentFileSet.add(defectEntity.getFileName());
            }
        });
        return currentFileSet;
    }

    @Override
    protected String getRecommitMQExchange(CommitDefectVO vo) {
        return ConstantsKt.PREFIX_EXCHANGE_DEFECT_COMMIT + ToolPattern.CLOC.name().toLowerCase(Locale.ENGLISH);
    }

    @Override
    protected String getRecommitMQRoutingKey(CommitDefectVO vo) {
        return ConstantsKt.PREFIX_ROUTE_DEFECT_COMMIT + ToolPattern.CLOC.name().toLowerCase(Locale.ENGLISH);
    }
}
