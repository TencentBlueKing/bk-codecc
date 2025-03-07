package com.tencent.bk.codecc.defect.service.impl.file;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.component.ScmJsonComponent;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CodeRepoInfoRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.ToolBuildStackRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.file.ScmFileInfoCacheRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.file.ScmFileInfoCacheDao;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.file.ScmFileInfoSnapshotDao;
import com.tencent.bk.codecc.defect.model.file.ScmFileInfoCacheEntity;
import com.tencent.bk.codecc.defect.model.file.ScmFileInfoSnapshotEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.pojo.FileMD5SingleModel;
import com.tencent.bk.codecc.defect.pojo.FileMD5TotalModel;
import com.tencent.bk.codecc.defect.service.file.ScmFileInfoService;
import com.tencent.bk.codecc.defect.vo.customtool.RepoSubModuleVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameChangeRecordVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.defect.vo.customtool.ScmInfoVO;
import com.tencent.bk.codecc.defect.vo.file.ScmFileMd5Info;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.codecc.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ScmFileInfoServiceImpl implements ScmFileInfoService {

    @Autowired
    private ScmFileInfoCacheDao scmFileInfoCacheDao;
    @Autowired
    private ScmJsonComponent scmJsonComponent;
    @Autowired
    private ScmFileInfoCacheRepository scmFileInfoCacheRepository;
    @Autowired
    private ToolBuildStackRepository toolBuildStackRepository;
    @Autowired
    private CodeRepoInfoRepository codeRepoInfoRepository;
    @Autowired
    private ScmFileInfoSnapshotDao scmFileInfoSnapshotDao;

    @Override
    public List<ScmFileMd5Info> listMd5FileInfos(long taskId, String toolName, String buildId) {
        log.info("listMd5FileInfos, taskId: {} toolName: {}, buildId: {}", taskId, toolName, buildId);

        // 如果前后两次分析的仓库地址或者分支发生了变化，则缓存失效
        boolean isCodeRepoChange = false;
        ToolBuildStackEntity toolBuildStack =
                toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        if (toolBuildStack != null) {
            String lastBuildId = toolBuildStack.getBaseBuildId();
            if (StringUtils.isNotEmpty(lastBuildId)) {
                List<CodeRepoInfoEntity> codeRepoInfoList = codeRepoInfoRepository.findByTaskIdAndBuildIdIn(taskId,
                        Sets.newHashSet(buildId, lastBuildId));
                if (codeRepoInfoList.size() == 2) {
                    List<CodeRepoEntity> codeRepoInfo1 = codeRepoInfoList.get(0).getRepoList();
                    List<CodeRepoEntity> codeRepoInfo2 = codeRepoInfoList.get(1).getRepoList();
                    Set<String> codeRepoInfoKeys1 = codeRepoInfo1.stream().map(it -> {
                        if (StringUtils.isNotEmpty(it.getRepoId())) {
                            return String.format("%s_%s", it.getRepoId(), it.getBranch());
                        } else {
                            return String.format("%s_%s", it.getUrl(), it.getBranch());
                        }
                    }).collect(Collectors.toSet());
                    Set<String> codeRepoInfoKeys2 = codeRepoInfo2.stream().map(it -> {
                        if (StringUtils.isNotEmpty(it.getRepoId())) {
                            return String.format("%s_%s", it.getRepoId(), it.getBranch());
                        } else {
                            return String.format("%s_%s", it.getUrl(), it.getBranch());
                        }
                    }).collect(Collectors.toSet());

                    isCodeRepoChange = !CollectionUtils.isEqualCollection(codeRepoInfoKeys1, codeRepoInfoKeys2);
                }
            }
        }
        if (isCodeRepoChange) {
            return Collections.emptyList();
        }

        List<ScmFileInfoCacheEntity> result =
                scmFileInfoCacheRepository.findSimpleByTaskIdAndToolName(taskId, toolName);
        return result.stream().map((entity) -> {
            ScmFileMd5Info fileMd5Info = new ScmFileMd5Info();
            BeanUtils.copyProperties(entity, fileMd5Info);
            return fileMd5Info;
        }).collect(Collectors.toList());
    }

    @Override
    @Async("asyncTaskExecutor")
    public void parseFileInfo(long taskId, String streamName, String toolName, String buildId) {
        log.info("start to parse file md5 info: {}, {}, {}", taskId, toolName, buildId);
        try {
            doParseFileInfo(taskId, streamName, toolName, buildId);
        } catch (Exception e) {
            log.error("parse file info fail for {}, {}", taskId, toolName, e);
        }
    }

    private void doParseFileInfo(long taskId, String streamName, String toolName, String buildId) {
        Map<String, ScmBlameVO> fileChangeRecordsMap = loadAuthorInfoMap(taskId, streamName, toolName, buildId);

        // 缓存scm文件信息
        cacheScmFileInfo(taskId, toolName, buildId, fileChangeRecordsMap);

        // 保存告警文件scm快照信息
        saveScmInfoSnapshot(taskId, streamName, toolName, buildId, fileChangeRecordsMap);
    }

    /**
     * 保存告警文件scm快照信息，按taskId来保存，因为每个工具都会执行一遍这个方法，所以只有不存在的时候才会保存，存在就不更新任何信息
     * @param taskId
     * @param streamName
     * @param toolName
     * @param buildId
     * @param fileChangeRecordsMap
     */
    private void saveScmInfoSnapshot(long taskId, String streamName, String toolName, String buildId,
                                     Map<String, ScmBlameVO> fileChangeRecordsMap) {
        Map<String, RepoSubModuleVO> codeRepoIdMap = loadRepoInfoMap(streamName, toolName, buildId);
        List<ScmFileInfoSnapshotEntity> scmFileInfoSnapshotEntityList = fileChangeRecordsMap.values().stream()
                .map(scmBlame -> {
                    ScmFileInfoSnapshotEntity scmFileInfoSnapshotEntity = new ScmFileInfoSnapshotEntity();
                    scmFileInfoSnapshotEntity.setTaskId(taskId);
                    scmFileInfoSnapshotEntity.setBuildId(buildId);
                    scmFileInfoSnapshotEntity.setFilePath(scmBlame.getFilePath());
                    scmFileInfoSnapshotEntity.setRelPath(scmBlame.getFileRelPath());
                    scmFileInfoSnapshotEntity.setMd5(scmBlame.getExtraInfoMap().get("md5"));
                    scmFileInfoSnapshotEntity.setUpdateTime(scmBlame.getFileUpdateTime());
                    scmFileInfoSnapshotEntity.setFileAuthor(scmBlame.getFileAuthor());
                    scmFileInfoSnapshotEntity.setScmType(scmBlame.getScmType());
                    scmFileInfoSnapshotEntity.setUrl(scmBlame.getUrl());
                    scmFileInfoSnapshotEntity.setBranch(scmBlame.getBranch());
                    scmFileInfoSnapshotEntity.setRevision(scmBlame.getRevision());
                    //如果是svn用rootUrl关联
                    if (ComConstants.CodeHostingType.SVN.name().equalsIgnoreCase(scmBlame.getScmType())) {
                        scmFileInfoSnapshotEntity.setRootUrl(scmBlame.getRootUrl());
                        if (codeRepoIdMap.get(scmBlame.getRootUrl()) != null) {
                            scmFileInfoSnapshotEntity.setRepoId(codeRepoIdMap.get(scmBlame.getRootUrl()).getRepoId());
                        }
                    } else { //其他用url关联
                        if (codeRepoIdMap.get(scmBlame.getUrl()) != null) {
                            RepoSubModuleVO repoSubModuleVO = codeRepoIdMap.get(scmBlame.getUrl());
                            scmFileInfoSnapshotEntity.setRepoId(repoSubModuleVO.getRepoId());
                            if (StringUtils.isNotEmpty(repoSubModuleVO.getSubModule())) {
                                scmFileInfoSnapshotEntity.setSubModule(repoSubModuleVO.getSubModule());
                            }
                        }
                    }
                    return scmFileInfoSnapshotEntity;
                }).collect(Collectors.toList());
        scmFileInfoSnapshotDao.batchSave(scmFileInfoSnapshotEntityList);
        log.info("save scm file info snapshot size: {}, {}, {}", scmFileInfoSnapshotEntityList.size(), taskId, buildId);
    }

    /**
     * 缓存scm文件信息
     *
     * @param taskId
     * @param toolName
     * @param buildId
     * @param fileChangeRecordsMap
     */
    private void cacheScmFileInfo(long taskId, String toolName, String buildId,
                                  Map<String, ScmBlameVO> fileChangeRecordsMap) {
        List<ScmFileInfoCacheEntity> fileInfoEntities = fileChangeRecordsMap.values().stream().map(scmBlame -> {
            // 保存md5文件信息
            ScmFileInfoCacheEntity entity = new ScmFileInfoCacheEntity();
            BeanUtils.copyProperties(scmBlame, entity);
            entity.setTaskId(taskId);
            entity.setToolName(toolName);
            entity.setMd5(scmBlame.getExtraInfoMap().get("md5"));
            entity.setBuildId(buildId);
            // 转化数据
            entity.setChangeRecords(scmBlame.getChangeRecords().stream().map((it) -> {
                ScmFileInfoCacheEntity.ScmBlameChangeRecordVO
                        record = new ScmFileInfoCacheEntity.ScmBlameChangeRecordVO();
                record.setAuthor(it.getAuthor());
                record.setLines(it.getLines());
                record.setLineUpdateTime(it.getLineUpdateTime());
                return record;
            }).collect(Collectors.toList()));

            return entity;
        }).collect(Collectors.toList());

        scmFileInfoCacheDao.batchSave(fileInfoEntities);

        log.info("cache scm file info size: {}, {}, {}, {}", fileInfoEntities.size(), taskId, toolName, buildId);
    }

    @Override
    public Map<String, ScmBlameVO> loadAuthorInfoMap(long taskId, String streamName, String toolName, String buildId) {
        List<ScmBlameVO> fileChangeRecords = new ArrayList<>();

        // MD5文件是全量的，以这个为准
        FileMD5TotalModel fileMD5TotalModel = scmJsonComponent.loadFileMD5(streamName, toolName, buildId);
        Map<String, FileMD5SingleModel> fileMd5INfoMap = new HashMap<>();
        fileMD5TotalModel.getFileList().forEach((fileInfo) -> {
            // 考虑到多仓库情况，优先使用绝对路径，多仓库之间的relPath可能相同
            String path = StringUtils.isNotEmpty(fileInfo.getFilePath())
                    ? fileInfo.getFilePath() : fileInfo.getFileRelPath();
            fileMd5INfoMap.put(path, fileInfo);
        });

        // 读之前缓存的文件
        fileChangeRecords.addAll(listScmBlameFileInfos(taskId, toolName));

        // 读最新的文件
        fileChangeRecords.addAll(scmJsonComponent.loadAuthorInfo(streamName, toolName, buildId));

        Map<String, ScmBlameVO> fileChangeRecordsMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(fileChangeRecords)) {
            for (ScmBlameVO fileLineAuthor : fileChangeRecords) {
                FileMD5SingleModel fileMD5SingleModel = fileMd5INfoMap.get(fileLineAuthor.getFilePath());
                FileMD5SingleModel md5Info = fileMD5SingleModel != null ? fileMD5SingleModel
                        : fileMd5INfoMap.get(fileLineAuthor.getFileRelPath());

                // 以md5.json的文件列表为准
                fileLineAuthor.setExtraInfoMap(new HashMap<>());
                if (md5Info != null) {
                    // md5.json文件才是最新的
                    fileLineAuthor.getExtraInfoMap().put("md5", md5Info.getMd5());
                    fileLineAuthor.setFilePath(md5Info.getFilePath());
                }

                fileChangeRecordsMap.put(fileLineAuthor.getFilePath(), fileLineAuthor);
            }
        }
        return fileChangeRecordsMap;
    }

    @Override
    public Map<String, RepoSubModuleVO> loadRepoInfoMap(String streamName, String toolName, String buildId) {
        JSONArray repoInfoJsonArr = scmJsonComponent.loadRepoInfo(streamName, toolName, buildId);
        Map<String, RepoSubModuleVO> codeRepoIdMap = Maps.newHashMap();
        if (repoInfoJsonArr != null && repoInfoJsonArr.length() > 0) {
            for (int i = 0; i < repoInfoJsonArr.length(); i++) {
                JSONObject codeRepoJson = repoInfoJsonArr.getJSONObject(i);
                ScmInfoVO codeRepoInfo = JsonUtil.INSTANCE.to(codeRepoJson.toString(), ScmInfoVO.class);
                //需要判断是svn还是git，svn采用rootUrl做key，git采用url做key
                RepoSubModuleVO repoSubModuleVO = new RepoSubModuleVO();
                repoSubModuleVO.setRepoId(codeRepoInfo.getRepoId());
                if (ComConstants.CodeHostingType.SVN.name().equalsIgnoreCase(codeRepoInfo.getScmType())) {
                    codeRepoIdMap.put(codeRepoInfo.getRootUrl(), repoSubModuleVO);
                } else {
                    codeRepoIdMap.put(codeRepoInfo.getUrl(), repoSubModuleVO);
                    if (CollectionUtils.isNotEmpty(codeRepoInfo.getSubModules())) {
                        for (RepoSubModuleVO subModuleVO : codeRepoInfo.getSubModules()) {
                            RepoSubModuleVO subRepoSubModuleVO = new RepoSubModuleVO();
                            subRepoSubModuleVO.setRepoId(codeRepoInfo.getRepoId());
                            subRepoSubModuleVO.setSubModule(subModuleVO.getSubModule());
                            codeRepoIdMap.put(subModuleVO.getUrl(), subRepoSubModuleVO);
                        }
                    }
                }
            }
        }
        return codeRepoIdMap;
    }

    private List<ScmBlameVO> listScmBlameFileInfos(long taskId, String toolName) {
        List<ScmFileInfoCacheEntity> result = scmFileInfoCacheRepository.findByTaskIdAndToolName(taskId, toolName);
        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return result.stream().map((entity) -> {
            ScmBlameVO scmBlameVO = new ScmBlameVO();
            BeanUtils.copyProperties(entity, scmBlameVO);
            if (CollectionUtils.isNotEmpty(entity.getChangeRecords())) {
                scmBlameVO.setChangeRecords(entity.getChangeRecords().stream().map((it) -> {
                    ScmBlameChangeRecordVO record = new ScmBlameChangeRecordVO();
                    record.setAuthor(it.getAuthor());
                    record.setLines(it.getLines());
                    record.setLineUpdateTime(it.getLineUpdateTime());
                    return record;
                }).collect(Collectors.toList()));
            }
            return scmBlameVO;
        }).collect(Collectors.toList());
    }
}
