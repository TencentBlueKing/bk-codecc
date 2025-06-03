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

package com.tencent.bk.codecc.task.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.reflect.TypeToken;
import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.audit.annotations.AuditInstanceRecord;
import com.tencent.bk.codecc.defect.api.BuildCheckerRestResource;
import com.tencent.bk.codecc.task.constant.TaskConstants.ToolPattern;
import com.tencent.bk.codecc.task.constant.TaskMessageCode;
import com.tencent.bk.codecc.task.dao.ToolMetaCacheServiceImpl;
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.GrayToolProjectRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolMetaRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.ToolControlInfoDao;
import com.tencent.bk.codecc.task.dao.mongotemplate.ToolMetaDao;
import com.tencent.bk.codecc.task.model.BaseDataEntity;
import com.tencent.bk.codecc.task.model.GrayToolProjectEntity;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolBinaryEntity;
import com.tencent.bk.codecc.task.model.ToolControlInfoEntity;
import com.tencent.bk.codecc.task.model.ToolEnvEntity;
import com.tencent.bk.codecc.task.model.ToolMetaEntity;
import com.tencent.bk.codecc.task.model.ToolOptionEntity;
import com.tencent.bk.codecc.task.model.ToolVersionEntity;
import com.tencent.bk.codecc.task.model.VarOptionEntity;
import com.tencent.bk.codecc.task.service.GrayToolProjectService;
import com.tencent.bk.codecc.task.service.ToolMetaService;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.bk.codecc.task.vo.gongfeng.ToolVersionExtVO;
import com.tencent.devops.common.api.BKToolBasicInfoVO;
import com.tencent.devops.common.api.RefreshDockerImageHashReqVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.ToolOption;
import com.tencent.devops.common.api.ToolVersionVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.Tool;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.MultenantConstants;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.constant.ToolConstants;
import com.tencent.devops.common.constant.audit.ActionAuditRecordContents;
import com.tencent.devops.common.constant.audit.ActionIds;
import com.tencent.devops.common.constant.audit.ResourceTypes;
import com.tencent.devops.common.util.AESUtil;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.GsonUtils;
import com.tencent.devops.common.web.mq.ConstantsKt;
import com.tencent.devops.image.api.ServiceDockerImageResource;
import com.tencent.devops.image.pojo.CheckDockerImageRequest;
import com.tencent.devops.image.pojo.CheckDockerImageResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.STRING_SPLIT;

/**
 * 工具元数据注册接口实现
 *
 * @version V1.0
 * @date 2020/4/8
 */
@Service
@Slf4j
public class ToolMetaServiceImpl implements ToolMetaService {

    private static final String TOOL_TYPE = "TOOL_TYPE";
    private static final String LANG = "LANG";
    private static final String DOCKER_IMAGE_DEFAULT_ACCOUNT = "DOCKER_IMAGE_DEFAULT_ACCOUNT";
    private static final String ALL_LANGUAGE_STRING = "ALL";
    private static final String TOOL_REGISTER_DEFAULT_PROJECT_ID = "TOOL_REGISTER_DEFAULT_PROJECT_ID";

    @Value("${aes.encryptor.key:#{null}}")
    private String encryptorKey;
    @Autowired
    private ToolMetaRepository toolMetaRepository;
    @Autowired
    private BaseDataRepository baseDataRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private ToolMetaDao toolMetaDao;
    @Autowired
    private ToolMetaCacheServiceImpl toolMetaCacheService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private Client client;
    @Autowired
    private GrayToolProjectService grayToolProjectService;
    @Autowired
    private ToolControlInfoDao toolControlInfoDao;
    @Autowired
    private GrayToolProjectRepository grayToolProjectRepository;

    @Override
    public List<String> getLangNamesByLangDigits(Long langDigits) {
        if (langDigits == null || langDigits == 0L) {
            return new ArrayList<>();
        }

        List<BaseDataEntity> baseDataEntities = baseDataRepository.findAllByParamType(LANG);
        return baseDataEntities.stream()
                .filter(it -> StringUtils.isNotBlank(it.getClocLang())
                        && (langDigits & Long.parseLong(it.getParamCode())) > 0)
                .map(BaseDataEntity::getClocLang)
                .collect(Collectors.toList());
    }

    @Override
    public List<ToolMetaDetailVO> obtainAllToolMetaDataList() {
        List<ToolMetaEntity> toolMetaEntities = toolMetaDao.findAllToolMetasSelectingNameTypePattern();
        if (CollectionUtils.isEmpty(toolMetaEntities)) {
            return Collections.emptyList();
        }
        List<ToolMetaDetailVO> vos = new ArrayList<>();
        for (ToolMetaEntity entity : toolMetaEntities) {
            ToolMetaDetailVO vo = new ToolMetaDetailVO();
            BeanUtils.copyProperties(entity, vo);
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public List<String> queryToolMetaNameListDataByType(String type) {
        List<ToolMetaEntity> toolMetaEntityList = toolMetaRepository.findNameByType(type);
        return toolMetaEntityList.stream().map(ToolMetaEntity::getName).collect(Collectors.toList());
    }

    @Override
    @ActionAuditRecord(
            actionId = ActionIds.REGISTER_TOOL,
            instance = @AuditInstanceRecord(
                    resourceType = ResourceTypes.TOOL,
                    instanceNames = "#toolMetaDetailVO?.name"
            ),
            content = ActionAuditRecordContents.REGISTER_TOOL
    )
    public ToolMetaDetailVO register(String userName, ToolMetaDetailVO toolMetaDetailVO) {
        log.info("begin register tool: {}", toolMetaDetailVO);

        if (StringUtils.isNotBlank(toolMetaDetailVO.getPattern()) && toolMetaDetailVO.getPattern()
                .equalsIgnoreCase(ToolPattern.STAT.name())) {
            if (toolMetaDetailVO.getCustomToolInfo() == null) {
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                        new String[]{"STAT 工具必须有自定义参数信息"});
            }

            if (toolMetaDetailVO.getCustomToolInfo().getCustomToolDimension() == null
                    || toolMetaDetailVO.getCustomToolInfo().getCustomToolDimension().isEmpty()) {
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                        new String[]{"STAT 工具必须有自定义参数信息"});
            }

            if (toolMetaDetailVO.getCustomToolInfo().getCustomToolParam() == null
                    || toolMetaDetailVO.getCustomToolInfo().getCustomToolParam().isEmpty()) {
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                        new String[]{"STAT 工具必须有自定义参数信息"});
            }
        }

        List<BaseDataEntity> baseDataEntityList = baseDataRepository.findByParamTypeIn(
                Lists.newArrayList(TOOL_TYPE, LANG, DOCKER_IMAGE_DEFAULT_ACCOUNT, TOOL_REGISTER_DEFAULT_PROJECT_ID));

        // 工具支持的代码语言列表包含"ALL" 则将语言替换成所有语言
        if (CollectionUtils.isNotEmpty(toolMetaDetailVO.getSupportedLanguages())
                && toolMetaDetailVO.getSupportedLanguages().contains(ALL_LANGUAGE_STRING)) {
            // 所有语言
            List<String> codeLangList =
                    baseDataEntityList.stream().map(BaseDataEntity::getLangFullKey)
                            .filter(StringUtils::isNotBlank).collect(Collectors.toList());
            toolMetaDetailVO.setSupportedLanguages(codeLangList);
        }
        log.info("begin register tool: {}", toolMetaDetailVO);

        // 参数校验
        validateParam(toolMetaDetailVO, baseDataEntityList);

        // 获取默认账号密码信息
        BaseDataEntity dockerImageAccount = baseDataEntityList.stream()
                .filter(baseDataEntity -> DOCKER_IMAGE_DEFAULT_ACCOUNT.equals(baseDataEntity.getParamType()))
                .findFirst().get();

        String toolName = toolMetaDetailVO.getName();
        boolean fromBkplugins =
                ToolConstants.RegisterRequestSource.BKPLUGINS.getName().equals(toolMetaDetailVO.getRequestSource());

        /**
         * 目前判断是否是注册新工具有 2 套并存的逻辑:
         * 1. 旧逻辑根据 debugPipelineId 判断;
         * 2. 新逻辑根据 toolName 判断.
         * 后续的趋势是去掉旧逻辑.
         * 现只保证使用旧逻辑创建的工具, 使用新逻辑可以再次集成, 并且后续还可使用旧逻辑再集成;
         * 不保证使用新逻辑创建的工具, 使用旧逻辑可以再次集成.
         */
        ToolMetaEntity toolMetaEntity = null;
        if (fromBkplugins) {
            toolMetaEntity = toolMetaRepository.findFirstByName(toolName);
        } else if (StringUtils.isNotBlank(toolMetaDetailVO.getDebugPipelineId())) {
            toolMetaEntity = toolMetaRepository.findByDebugPipelineId(toolMetaDetailVO.getDebugPipelineId());
        }

        if (toolMetaEntity == null) {   // 注册新工具
            // 旧逻辑：注册新工具时，需要校验工具名是否已经存在，存在则不能注册
            if (!fromBkplugins && toolMetaRepository.existsByName(toolName)) {
                log.error("tool has register: {}", toolName);
                throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{toolName}, null);
            }

            if (StringUtils.isBlank(toolMetaDetailVO.getPattern())) {
                toolMetaDetailVO.setPattern(ToolPattern.LINT.name());
            }
            toolMetaEntity = new ToolMetaEntity();
            BeanUtils.copyProperties(toolMetaDetailVO, toolMetaEntity);
            toolMetaEntity.setPattern(toolMetaDetailVO.getPattern());
            toolMetaEntity.setDockerImageAccount(dockerImageAccount.getParamCode());
            toolMetaEntity.setDockerImagePasswd(dockerImageAccount.getParamValue());
            toolMetaEntity.setCreatedBy(userName);
            toolMetaEntity.setCreatedDate(System.currentTimeMillis());

            // 给工具排序
            resetToolOrder(toolMetaEntity, baseDataEntityList);
            // 给工具关联默认调试项目
            toolProjectIdRelationship(baseDataEntityList, toolName);
        } else {    // 更新已有工具
            // 更新已有工具时，工具名（name）不能修改
            if (!toolMetaEntity.getName().equals(toolName)) {
                log.error("can not change tool name: {} -> {}", toolMetaEntity.getName(), toolName);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                        new String[]{ComConstants.ToolIntegrateErrorCode.NOCHANGE_NAME.getValue() + " "
                                + toolMetaEntity.getName() + " -> " + toolName}, null);
            }

            // 更新已有工具时，工具模式（pattern）不能修改
            String pattern = toolMetaDetailVO.getPattern();
            if (StringUtils.isNotEmpty(pattern) && !toolMetaEntity.getPattern().equals(pattern)) {
                log.error("can not change tool pattern: {} -> {}", toolMetaEntity.getPattern(), pattern);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                        new String[]{ComConstants.ToolIntegrateErrorCode.NOCHANGE_PATTERN.getValue() + " "
                                + toolMetaEntity.getPattern() + " -> " + pattern}, null);
            }

            toolMetaEntity.setName(toolName);

            toolMetaEntity.setDisplayName(toolMetaDetailVO.getDisplayName());
            toolMetaEntity.setType(toolMetaDetailVO.getType());
            toolMetaEntity.setBriefIntroduction(toolMetaDetailVO.getBriefIntroduction());
            toolMetaEntity.setDockerTriggerShell(toolMetaDetailVO.getDockerTriggerShell());
            toolMetaEntity.setToolScanCommand(toolMetaDetailVO.getToolScanCommand());
            toolMetaEntity.setDockerImageURL(toolMetaDetailVO.getDockerImageURL());
            toolMetaEntity.setLogo(toolMetaDetailVO.getLogo());
            toolMetaEntity.setProcessList(toolMetaDetailVO.getProcessList());
            toolMetaEntity.setClusterType(toolMetaDetailVO.getClusterType());
            toolMetaEntity.setGitDiffRequired(toolMetaDetailVO.getGitDiffRequired());

            toolMetaEntity.setDockerImageAccount(dockerImageAccount.getParamCode());
            toolMetaEntity.setDockerImagePasswd(dockerImageAccount.getParamValue());

            toolMetaEntity.applyAuditInfoOnUpdate(userName);
            String oldType = toolMetaEntity.getType();
            // 如果工具类型变更了，需要重新对工具排序
            if (oldType.equals(toolMetaDetailVO.getType())) {
                resetToolOrder(toolMetaEntity, baseDataEntityList);
            }
        }

        toolMetaEntity.setTenantId(toolMetaDetailVO.getTenantId());

        // 转换语言
        List<String> supportedLanguages = toolMetaDetailVO.getSupportedLanguages();
        long lang = convertLang(supportedLanguages, baseDataEntityList);
        toolMetaEntity.setLang(lang);

        // 转换个性化参数
        toolMetaEntity.setParams(CollectionUtils.isEmpty(toolMetaDetailVO.getToolOptions())
                ? null : GsonUtils.toJson(toolMetaDetailVO.getToolOptions()));

        // 将工具自定义的参数保存在 tool_options 字段
        copyVarOptionList(toolMetaDetailVO, toolMetaEntity);

        List<ToolVersionEntity> toolVersionSet = toolMetaEntity.getToolVersions() == null
                ? new ArrayList<>() : toolMetaEntity.getToolVersions();
        ToolVersionEntity toolVersionEntity = toolVersionSet.stream()
                .filter(it -> it.getVersionType().equals(ToolIntegratedStatus.T.name())).findFirst().orElse(null);
        long curTime = System.currentTimeMillis();
        if (toolVersionEntity == null) {
            if (Objects.isNull(toolMetaDetailVO.getBinary())) {
                ToolMetaDetailVO.Binary binary = new ToolMetaDetailVO.Binary();
                toolMetaDetailVO.setBinary(binary);
            }
            List<ToolEnvEntity> toolEnvEntityList = new ArrayList<>();
            if (Objects.nonNull(toolMetaDetailVO.getBinary().getToolEnvs())) {
                BeanUtils.copyProperties(toolMetaDetailVO.getBinary().getToolEnvs(), toolEnvEntityList);
            }
            ToolBinaryEntity toolBinaryEntity = new ToolBinaryEntity();
            BeanUtils.copyProperties(toolMetaDetailVO.getBinary(), toolBinaryEntity);
            toolBinaryEntity.setToolEnvs(toolEnvEntityList);
            toolVersionEntity = new ToolVersionEntity(
                    ToolIntegratedStatus.T.name(),
                    toolMetaDetailVO.getDockerTriggerShell(),
                    toolMetaDetailVO.getDockerImageURL(),
                    toolMetaDetailVO.getDockerImageVersion(),
                    toolMetaDetailVO.getForeignDockerImageVersion(),
                    null,
                    toolBinaryEntity,
                    curTime,
                    userName,
                    curTime,
                    userName
            );
            toolVersionSet.add(toolVersionEntity);
            toolMetaEntity.setToolVersions(toolVersionSet);
        } else {
            log.info("exit toolVersionEntity: {} ", toolVersionEntity);
            toolVersionEntity.setDockerTriggerShell(toolMetaDetailVO.getDockerTriggerShell());
            toolVersionEntity.setDockerImageURL(toolMetaDetailVO.getDockerImageURL());
            toolVersionEntity.setDockerImageVersion(toolMetaDetailVO.getDockerImageVersion());
            toolVersionEntity.setForeignDockerImageVersion(toolMetaDetailVO.getForeignDockerImageVersion());
            toolVersionEntity.setCreatedDate(curTime);
            toolVersionEntity.setCreatedBy(userName);
            toolVersionEntity.setUpdatedDate(curTime);
            toolVersionEntity.setUpdatedBy(userName);
            if (Objects.isNull(toolMetaDetailVO.getBinary())) {
                ToolMetaDetailVO.Binary binary = new ToolMetaDetailVO.Binary();
                toolMetaDetailVO.setBinary(binary);
            }
            List<ToolEnvEntity> toolEnvEntityList = new ArrayList<>();
            if (Objects.nonNull(toolMetaDetailVO.getBinary().getToolEnvs())) {
                BeanUtils.copyProperties(toolMetaDetailVO.getBinary().getToolEnvs(), toolEnvEntityList);
            }
            ToolBinaryEntity toolBinaryEntity = new ToolBinaryEntity();
            BeanUtils.copyProperties(toolMetaDetailVO.getBinary(), toolBinaryEntity);
            toolBinaryEntity.setToolEnvs(toolEnvEntityList);
            toolVersionEntity.setBinary(toolBinaryEntity);
        }

        if (StringUtils.isNotBlank(toolMetaDetailVO.getDockerImageURL())) {
            String newDockerImageHash = getDockerImageHash(toolMetaEntity, toolMetaDetailVO.getDockerImageVersion());
            if (StringUtils.isNotEmpty(newDockerImageHash)) {
                log.info("set docker image hash! toolName: {}, newDockerImageHash: {}", toolName, newDockerImageHash);
                toolVersionEntity.setDockerImageHash(newDockerImageHash);
            }
        }

        if (toolMetaDetailVO.getPattern().equals(ToolPattern.STAT.name())) {
            toolMetaEntity.setCustomToolInfo(JsonUtil.INSTANCE.toJson(toolMetaDetailVO.getCustomToolInfo()));
        }

        // 更新工具元数据
        updateToolMeta(toolMetaEntity);

        BeanUtils.copyProperties(toolMetaEntity, toolMetaDetailVO);
        toolMetaDetailVO.setSupportedLanguages(supportedLanguages);

        return toolMetaDetailVO;
    }

    /**
     * 将 ToolMetaDetailVO 中 varOptionList 字段的值赋值到 ToolOptionEntity 中的 varOptionList 字段
     */
    private void copyVarOptionList(ToolMetaDetailVO source, ToolMetaEntity target) {
        List<ToolOptionEntity> toolOptions = new ArrayList<>();
        if (CollectionUtils.isEmpty(source.getToolOptions())) {
            target.setToolOptions(toolOptions);
            return;
        }

        source.getToolOptions().forEach(toolOption -> {
            ToolOptionEntity toolOptionEntity = new ToolOptionEntity();
            BeanUtils.copyProperties(toolOption, toolOptionEntity, "varOptionList");

            if (CollectionUtils.isNotEmpty(toolOption.getVarOptionList())) {
                List<VarOptionEntity> varOptionEntities = new ArrayList<>();
                toolOption.getVarOptionList().forEach(varOption -> {
                    VarOptionEntity varOptionEntity = new VarOptionEntity();
                    BeanUtils.copyProperties(varOption, varOptionEntity);
                    varOptionEntities.add(varOptionEntity);
                });
                toolOptionEntity.setVarOptionList(varOptionEntities);
            }

            toolOptions.add(toolOptionEntity);
        });
        target.setToolOptions(toolOptions);
    }

    /**
     * 为新工具添加默认集成调试项目
     */
    private void toolProjectIdRelationship(@NotNull List<BaseDataEntity> baseDataEntityList, String toolName) {
        BaseDataEntity toolRegisterProjIdEntity = baseDataEntityList.stream()
                .filter(entity -> TOOL_REGISTER_DEFAULT_PROJECT_ID.equals(entity.getParamType())).findFirst().get();

        // 查询toolName是否绑定默认测试/灰度项目
        GrayToolProjectEntity entity =
                grayToolProjectRepository.findFirstByProjectIdAndToolName(toolRegisterProjIdEntity.getParamValue(),
                        toolName);
        if (null != entity) {
            log.warn("grayToolProjectVO is existed: {}", entity);
            return;
        }
        GrayToolProjectVO grayToolProjectVO = new GrayToolProjectVO();
        grayToolProjectVO.setProjectId(toolRegisterProjIdEntity.getParamValue());
        grayToolProjectVO.setToolName(toolName);
        grayToolProjectVO.setToolNameList(Collections.singletonList(toolName));
        grayToolProjectVO.setReason(ComConstants.SYSTEM_USER);
        // 初始设置为测试阶段
        grayToolProjectVO.setStatus(ToolIntegratedStatus.T.value());
        grayToolProjectService.save(ComConstants.SYSTEM_USER, grayToolProjectVO);
        log.info("new tool successfully associated with integration debugging project: {}", toolName);
    }

    @Override
    public List<ToolMetaDetailVO> queryToolMetaDataList(String tenantId, String projectId, Long taskId) {
        List<ToolMetaDetailVO> beforeFilter = queryToolMetaDataList(projectId, taskId);

        if (beforeFilter == null || StringUtils.isBlank(tenantId)) {
            return new ArrayList<>();
        }

        return beforeFilter.stream()
                .filter(it -> (tenantId.equals(it.getTenantId())
                        || MultenantConstants.SYSTEM_TENANT.equals(it.getTenantId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<ToolMetaDetailVO> queryToolMetaDataList(String projectId, Long taskId) {
        Map<String, ToolMetaBaseVO> toolMetaDetailVOMap =
                toolMetaCacheService.getToolMetaListFromCache(Boolean.TRUE, Boolean.TRUE);

        //工具版本，T-测试版本，G-灰度版本，P-正式发布版本
        String toolV = ToolIntegratedStatus.P.name();
        Map<String, String> finalToolVersionMap = Maps.newHashMap();
        boolean hasToolVersionMap = false;

        // 是否预发布
        TaskInfoEntity taskDetail = null;
        if (taskId != null) {
            taskDetail = taskRepository.findFirstByTaskId(taskId);
        }
        if (taskDetail != null
            && taskDetail.getCheckerSetEnvType() != null
            && taskDetail.getCheckerSetEnvType().equals(ComConstants.CheckerSetEnvType.PRE_PROD.getKey())) {
            toolV = ToolIntegratedStatus.PRE_PROD.name();
            log.info("get tool meta from pre prod: {}", taskId);
        } else {
            hasToolVersionMap = true;
            List<GrayToolProjectVO> grayToolProjectList =
                    grayToolProjectService.getGrayToolListByProjectId(projectId);
            finalToolVersionMap = grayToolProjectList.stream().collect(Collectors.toMap(GrayToolProjectVO::getToolName,
                    vo -> ToolIntegratedStatus.getInstance(vo.getStatus()).name(), (a, b) -> b));
        }

        List<ToolMetaDetailVO> toolMetaDetailVOList = new ArrayList<>(toolMetaDetailVOMap.size());
        String finalToolV = toolV;

        for (Map.Entry<String, ToolMetaBaseVO> entry : toolMetaDetailVOMap.entrySet()) {
            String toolName = entry.getKey();
            ToolMetaBaseVO tool = entry.getValue();
            boolean match = false;
            // 如果不是任务预发布状态，优先取灰度项目的工具状态，没有则默认生产
            if (hasToolVersionMap) {
                finalToolV = finalToolVersionMap.getOrDefault(toolName, ToolIntegratedStatus.P.name());
            }

            ToolMetaDetailVO toolMetaDetailVO = (ToolMetaDetailVO) tool;
            toolMetaDetailVO.setGraphicDetails(null);
            toolMetaDetailVO.setLogo(null);
            //通过灰度状态toolV，获取相应版本，并赋值到toolMetaDetailVO的dockerImageVersion
            for (ToolVersionVO toolversion: toolMetaDetailVO.getToolVersions()) {
                if (finalToolV.equals(toolversion.getVersionType())) {
                    toolMetaDetailVO.setDockerTriggerShell(toolversion.getDockerTriggerShell());
                    toolMetaDetailVO.setDockerImageURL(toolversion.getDockerImageURL());
                    toolMetaDetailVO.setDockerImageVersion(toolversion.getDockerImageVersion());
                    toolMetaDetailVO.setDockerImageVersionType(finalToolV);
                    match = true;
                }
            }

            if (!match) {
                log.info("start to find prod tool meta for : {}, {}", projectId, taskId);
                for (ToolVersionVO toolversion: toolMetaDetailVO.getToolVersions()) {
                    if (ToolIntegratedStatus.P.name().equals(toolversion.getVersionType())) {
                        toolMetaDetailVO.setDockerTriggerShell(toolversion.getDockerTriggerShell());
                        toolMetaDetailVO.setDockerImageURL(toolversion.getDockerImageURL());
                        toolMetaDetailVO.setDockerImageVersion(toolversion.getDockerImageVersion());
                        toolMetaDetailVO.setDockerImageVersionType(ToolIntegratedStatus.P.name());
                    }
                }
            }

            toolMetaDetailVOList.add(toolMetaDetailVO);
        }

        return toolMetaDetailVOList;
    }

    /**
     * 刷新工具镜像版本
     *
     * @param refreshDockerImageHashReqVO
     * @return
     */
    @Override
    public Boolean refreshDockerImageHash(RefreshDockerImageHashReqVO refreshDockerImageHashReqVO) {
        log.info("refresh dockerImageHash: {}", GsonUtils.toJson(refreshDockerImageHashReqVO));
        String toolName = refreshDockerImageHashReqVO.getToolName();
        String versionType = refreshDockerImageHashReqVO.getVersionType();
        ToolMetaEntity toolMetaEntity = toolMetaRepository.findFirstByName(toolName);

        if (toolMetaEntity == null) {
            log.error("not found tool by toolName: {}", toolName);
            throw new CodeCCException(CommonMessageCode.THIRD_PARTY_SYSTEM_FAIL,
                    String.format("not found tool by toolName: %s", toolName));
        }

        // coverity,klocwork,pinpoint工具没有镜像，通过工具版本号来判断是否有变化
        if (Tool.COVERITY.name().equals(toolName)
                || Tool.KLOCWORK.name().equals(toolName)
                || Tool.PINPOINT.name().equals(toolName)) {
            String newToolVersion = refreshDockerImageHashReqVO.getToolVersion();
            toolMetaEntity.setToolVersion(newToolVersion);
            updateToolMeta(toolMetaEntity);
        } else {
            ToolVersionEntity toolVersion = toolMetaEntity.getToolVersions().stream()
                    .filter(it -> it.getVersionType().equals(versionType)).findFirst().get();
            if (toolVersion == null) {
                log.error("tool[{}] not exist such version type[{}]! ", toolName, versionType);
                return false;
            }
            String newDockerImageHash = getDockerImageHash(toolMetaEntity, toolVersion.getDockerImageVersion());
            if (StringUtils.isNotEmpty(newDockerImageHash)
                    && !newDockerImageHash.equals(toolVersion.getDockerImageHash())) {
                log.info("update docker image hash! toolName: {}, dockerImageHash: {}", toolName, newDockerImageHash);
                toolVersion.setDockerImageHash(newDockerImageHash);
                updateToolMeta(toolMetaEntity);
            }
        }

        log.info("refresh dockerImageHash finish. {}", toolName);
        return true;
    }

    private void updateToolMeta(ToolMetaEntity toolMetaEntity) {
        toolMetaEntity = toolMetaRepository.save(toolMetaEntity);

        // 刷新工具缓存
        // toolMetaRepository.save可能不立即生效
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        toolMetaCacheService.loadToolDetailCache();

        // 刷新Redis缓存后，需要同步刷新task的其他节点以及defect和codeccjob的缓存，确保每个节点的缓存都是最新的工具信息
//        rabbitTemplate.convertAndSend(ConstantsKt.EXCHANGE_REFRESH_TOOLMETA_CACHE,
//                "", toolMetaEntity.getName());
        redisTemplate.convertAndSend(ConstantsKt.EXCHANGE_REFRESH_TOOLMETA_CACHE, toolMetaEntity.getName());
    }

    @Nullable
    private String getDockerImageHash(ToolMetaEntity toolMetaEntity, String imageVersion) {
        String toolName = toolMetaEntity.getName();
        String userId = toolMetaEntity.getDockerImageAccount();
        String imageUrl = toolMetaEntity.getDockerImageURL();
        if (StringUtils.isNotEmpty(imageVersion)) {
            imageUrl = String.format("%s:%s", imageUrl, imageVersion);
        }
        String passwd = toolMetaEntity.getDockerImagePasswd();
        if (StringUtils.isNotEmpty(passwd)) {
            passwd = AESUtil.INSTANCE.decrypt(encryptorKey, passwd);
        }
        String registryHost = imageUrl.split("/")[0];
        List<CheckDockerImageRequest> requestList =
                Lists.newArrayList(new CheckDockerImageRequest(imageUrl, registryHost, userId, passwd));
        try {
            com.tencent.devops.common.api.pojo.Result<List<CheckDockerImageResponse>> imageResult =
                    client.getDevopsService(ServiceDockerImageResource.class).checkDockerImage(userId, requestList);
            if (imageResult.isNotOk() || null == imageResult.getData()
                    || null == imageResult.getData().get(0) || imageResult.getData().get(0).getErrorCode() != 0) {
                String errMsg = String.format("get image list fail! toolName: %s, imageUrl: %s, imageResult: %s",
                        toolName, imageUrl, imageResult);
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.THIRD_PARTY_SYSTEM_FAIL, errMsg);
            }
            String dockerImageHash = imageResult.getData().get(0).getId();
            log.info("getDockerImageHash success. toolName: {}, dockerImageHash: {}", toolName, dockerImageHash);
            return dockerImageHash;
        } catch (Throwable throwable) {
            String errMsg = String.format("get image list fail! toolName: %s, imageUrl: %s", toolName, imageUrl);
            log.error(errMsg, throwable);
            throw new CodeCCException(CommonMessageCode.THIRD_PARTY_SYSTEM_FAIL, errMsg, throwable);
        }
    }

    private long convertLang(List<String> supportedLanguages, List<BaseDataEntity> baseDataEntityList) {
        Map<String, BaseDataEntity> langMap = new HashMap<>();
        baseDataEntityList.forEach(baseDataEntity -> {
            if (LANG.equals(baseDataEntity.getParamType())) {
                langMap.put(baseDataEntity.getLangFullKey(), baseDataEntity);
            }
        });

        long lang = 0;
        if (CollectionUtils.isNotEmpty(supportedLanguages)) {
            for (int i = 0; i < supportedLanguages.size(); i++) {
                String langStr = supportedLanguages.get(i);
                lang += Long.valueOf(langMap.get(langStr).getParamCode());
            }
        }

        return lang;
    }

    private void validateParam(ToolMetaDetailVO toolMetaDetailVO, List<BaseDataEntity> baseDataEntityList) {
        // 检查工具类型
        validateToolType(toolMetaDetailVO.getType());

        // 校验语言
        validateLanguage(toolMetaDetailVO.getSupportedLanguages());
    }

    @Override
    public Boolean validateToolType(String toolType) {
        List<BaseDataEntity> baseDataEntityList = baseDataRepository.findByParamTypeIn(Lists.newArrayList(TOOL_TYPE));
        Set<String> toolTypeSet = new HashSet<>();
        baseDataEntityList.forEach(baseDataEntity -> toolTypeSet.add(baseDataEntity.getParamCode()));

        if (!toolTypeSet.contains(toolType)) {
            String errMsg = String.format("%s 输入的工具类型type:[%s]",
                    ComConstants.ToolIntegrateErrorCode.INVALID_TYPE.getValue(), toolType);
            log.error("{}不在取值范围内: {}", errMsg, toolTypeSet);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
        }
        return true;
    }

    @Override
    public Boolean validateLanguage(List<String> languages) {
        List<BaseDataEntity> baseDataEntityList = baseDataRepository.findByParamTypeIn(Lists.newArrayList(LANG));
        Map<String, BaseDataEntity> langMap = new HashMap<>();
        baseDataEntityList.forEach(baseDataEntity -> {
            langMap.put(baseDataEntity.getLangFullKey(), baseDataEntity);
        });

        if (CollectionUtils.isNotEmpty(languages) && !langMap.keySet().containsAll(languages)) {
            String errMsg = String.format("输入的工具支持语言: %s, 不在取值范围内: %s", languages, langMap.keySet());
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
        }
        return true;
    }

    /**
     * 获取工具元数据信息
     *
     * @param toolName
     */
    @Override
    public ToolMetaDetailVO obtainToolMetaData(String toolName) {
        ToolMetaEntity toolMetaEntity = toolMetaRepository.findFirstByName(toolName);
        ToolMetaDetailVO toolMetaDetailVO = new ToolMetaDetailVO();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(toolMetaEntity.getCustomToolInfo())) {
            ToolMetaDetailVO.CustomToolInfo customToolInfo;
            customToolInfo = GsonUtils.fromJson(toolMetaEntity.getCustomToolInfo(),
                    new TypeToken<ToolMetaDetailVO.CustomToolInfo>() {
                    }.getType());
            toolMetaDetailVO.setCustomToolInfo(customToolInfo);
        }

        BeanUtils.copyProperties(toolMetaEntity, toolMetaDetailVO);
        log.info("obtain tool meta info from task, toolName: {}", toolName);
        return toolMetaDetailVO;
    }


    /**
     * 首次添加是给工具默认排序，排在同类型工具的最后一个
     *
     * @param toolMetaEntity
     * @param baseDataEntityList
     */
    private void resetToolOrder(ToolMetaEntity toolMetaEntity, List<BaseDataEntity> baseDataEntityList) {
        String toolID = toolMetaEntity.getName();
        String type = toolMetaEntity.getType();
        List<ToolMetaEntity> allTools = toolMetaRepository.findAllByEntityIdIsNotNull();
        Map<String, ToolMetaEntity> toolMap = allTools.stream()
                .collect(Collectors.toMap(ToolMetaEntity::getName, Function.identity(), (a, b) -> a));

        List<BaseDataEntity> toolTypeList = baseDataEntityList.stream()
                .filter(baseDataEntity -> TOOL_TYPE.equals(baseDataEntity.getParamType()))
                .sorted(Comparator.comparing(BaseDataEntity::getParamExtend3))
                .collect(Collectors.toList());

        BaseDataEntity toolOrderEntity = baseDataRepository.findFirstByParamType(RedisKeyConstants.KEY_TOOL_ORDER);
        String toolOrder = toolOrderEntity.getParamValue();
        String[] toolOrderArr = toolOrder.split(STRING_SPLIT);

        // 1.分组
        Map<String, List<String>> groupToolByTypeMap = new HashMap<>();
        for (int i = 0; i < toolOrderArr.length; i++) {
            String name = toolOrderArr[i];
            ToolMetaEntity tool = toolMap.get(name);
            if (tool == null) {
                continue;
            }
            String tmpType = tool.getType();

            if (!toolID.equalsIgnoreCase(name)) {
                groupToolByType(groupToolByTypeMap, name, tmpType);
            }
        }
        groupToolByType(groupToolByTypeMap, toolID, type);

        // 2.按组的顺序叠加工具
        StringBuffer newToolOrder = new StringBuffer();
        for (BaseDataEntity toolType : toolTypeList) {
            List<String> toolList = groupToolByTypeMap.get(toolType.getParamCode());
            if (CollectionUtils.isNotEmpty(toolList)) {
                // 每种工具类型中的工具 根据首字母正序排序
                toolList.sort(String::compareTo);
                for (String toolId : toolList) {
                    newToolOrder.append(toolId).append(",");
                }
            }
        }

        // 去掉最后一个逗号
        if (newToolOrder.length() > 0) {
            newToolOrder.deleteCharAt(newToolOrder.length() - 1);
        }

        toolOrderEntity.setParamValue(newToolOrder.toString());
        baseDataRepository.save(toolOrderEntity);

        // 同步刷新redis缓存中工具的顺序
        redisTemplate.opsForValue().set(RedisKeyConstants.KEY_TOOL_ORDER, toolOrder);
    }

    private void groupToolByType(Map<String, List<String>> groupToolByTypeMap, String name, String tmpType) {
        List<String> toolList = groupToolByTypeMap.get(tmpType);
        if (CollectionUtils.isEmpty(toolList)) {
            toolList = new ArrayList<>();
            groupToolByTypeMap.put(tmpType, toolList);
        }
        toolList.add(name);
    }

    /**
     * 往前推工具状态
     *
     * @param toolName
     * @param toolImageTag
     * @param fromStatus
     * @param toStatus
     * @param username
     * @return 函数执行结果信息
     */
    @Override
    public String updateToolMetaToStatus(
            String toolName,
            String toolImageTag,
            ToolIntegratedStatus fromStatus,
            ToolIntegratedStatus toStatus,
            String username
    ) {
        if (toStatus == ToolIntegratedStatus.T) {
            // do nothing
            return "Test integrated status update successfully";
        }

        ToolMetaEntity toolEntity = toolMetaRepository.findFirstByName(toolName);
        if (toolEntity == null) {
            throw new CodeCCException(CommonMessageCode.INVALID_TOOL_NAME, new String[]{toolName}, null);
        }

        if (StringUtils.isNotBlank(toolEntity.getStatus())
                && toolEntity.getStatus().equals(ToolIntegratedStatus.D.name())) {
            if (toStatus == ToolIntegratedStatus.D) {
                return String.format("Tool(%s) has been deprecated", toolName);
            }

            // 已下架工具不能转成其他状态
            throw new CodeCCException(
                    TaskMessageCode.TOOL_STATUS_UPDATE_FAIL,
                    new String[]{String.format("Tool(%s) has been deprecated", toolName)}
            );
        }

        return changeToolStatus(toolEntity, toolImageTag, fromStatus, toStatus, username);
    }

    @Override
    public String revertToolMetaStatus(String toolName, ToolIntegratedStatus status, String username) {
        ToolMetaEntity toolEntity = toolMetaRepository.findFirstByName(toolName);

        if (toolEntity == null) {
            throw new CodeCCException(CommonMessageCode.INVALID_TOOL_NAME, new String[]{toolName}, null);
        }

        List<ToolVersionEntity> lastToolVersions = toolEntity.getLastToolVersions();
        if (CollectionUtils.isEmpty(lastToolVersions)) {
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                    new String[]{"lastToolVersions is null, can not be revert"}, null);
        }

        if (status == ToolIntegratedStatus.T) {
            return "do nothing";
        }

        List<ToolVersionEntity> toolVersions = toolEntity.getToolVersions();

        ToolVersionEntity toolVersionEntity = toolVersions.stream().filter(it ->
                it.getVersionType().equals(status.name())).findFirst().orElse(new ToolVersionEntity());
        ToolVersionEntity lastToolVersionEntity = lastToolVersions.stream().filter(it ->
                it.getVersionType().equals(status.name())).findFirst().orElse(new ToolVersionEntity());

        String resultMsg = String.format("from: %s \n to: %s", lastToolVersionEntity, toolVersionEntity);
        BeanUtils.copyProperties(lastToolVersionEntity, toolVersionEntity);

        updateToolMeta(toolEntity);

        return String.format("revert tool status successfully: %s, %s\n%s", toolName, status, resultMsg);
    }

    private String changeToolStatus(ToolMetaEntity toolEntity,
                                    String toolImageTag,
                                    ToolIntegratedStatus fromStatus,
                                    ToolIntegratedStatus toStatus,
                                    String username) {
        log.info("change tool status: {}, update {} {} to {}",
                toolEntity.getName(), toolImageTag, fromStatus, toStatus);


        List<ToolVersionEntity> toolVersions =
                toolEntity.getToolVersions() != null ? toolEntity.getToolVersions() : new ArrayList<>();
        List<ToolVersionEntity> lastToolVersions =
                toolEntity.getLastToolVersions() != null ? toolEntity.getLastToolVersions() : new ArrayList<>();

        Map<String, ToolVersionEntity> toolVersionsMap = new HashMap<>();
        Map<String, ToolVersionEntity> lastToolVersionsMap = new HashMap<>();

        toolVersions.forEach(it -> toolVersionsMap.put(it.getVersionType(), it));

        lastToolVersions.forEach(it -> lastToolVersionsMap.put(it.getVersionType(), it));

        ToolVersionEntity lastToolVersion = lastToolVersionsMap.getOrDefault(toStatus.name(), new ToolVersionEntity());
        ToolVersionEntity toEntity = toolVersionsMap.getOrDefault(toStatus.name(), null);
        log.info("backup test data: {}", toolEntity.getName());
        if (toEntity != null) {
            BeanUtils.copyProperties(toEntity, lastToolVersion);
            lastToolVersion.setUpdatedBy(username);
            lastToolVersion.setUpdatedDate(System.currentTimeMillis());
            lastToolVersionsMap.put(toStatus.name(), lastToolVersion);
        } else {
            toEntity = new ToolVersionEntity();
            ToolVersionEntity prodToolVersion = toolVersionsMap.getOrDefault(ToolIntegratedStatus.P.name(), null);
            if (prodToolVersion != null) {
                BeanUtils.copyProperties(prodToolVersion, lastToolVersion);
                lastToolVersion.setVersionType(toStatus.name());
                lastToolVersion.setUpdatedBy(username);
                lastToolVersion.setUpdatedDate(System.currentTimeMillis());
                lastToolVersionsMap.put(toStatus.name(), lastToolVersion);
            }
        }

        log.info("copy data: {}", toolEntity.getName());
        ToolVersionEntity fromEntity = toolVersionsMap.getOrDefault(fromStatus.name(), new ToolVersionEntity());
        BeanUtils.copyProperties(fromEntity, toEntity);
        toEntity.setVersionType(toStatus.name());
        toEntity.setUpdatedBy(username);
        toEntity.setUpdatedDate(System.currentTimeMillis());
        //如果传递的工具镜像版本不为空，这保存该版本号
        if (!StringUtils.isEmpty(toolImageTag)) {
            toEntity.setDockerImageVersion(toolImageTag);
        }
        toolVersionsMap.put(toStatus.name(), toEntity);

        // update toolEntity
        String resultMsg = "";
        if (CollectionUtils.isNotEmpty(toolVersionsMap.values())) {
            List<ToolVersionEntity> newToolVersions = new ArrayList<>(toolVersionsMap.values());
            resultMsg = String.format("from : %s ===> %s", toolEntity.getToolVersions(), newToolVersions);
            toolEntity.setToolVersions(newToolVersions);
        }
        if (CollectionUtils.isNotEmpty(lastToolVersionsMap.values())) {
            toolEntity.setLastToolVersions(new ArrayList<>(lastToolVersionsMap.values()));
        }

        updateToolMeta(toolEntity);
        return String.format("%s integrated status update to %s status successfully: %s", fromStatus, toStatus,
                resultMsg);
    }

    @Override
    public List<ToolMetaDetailVO> queryAllToolMetaDataList() {
        Map<String, ToolMetaBaseVO> toolMetaDetailVOMap =
                toolMetaCacheService.getToolMetaListFromCache(Boolean.TRUE, Boolean.TRUE);

        //设置P-正式发布版本
        String toolV = ToolIntegratedStatus.P.name();
        String finalToolV = toolV;
        List<ToolMetaDetailVO> toolMetaDetailVOList = new ArrayList<>(toolMetaDetailVOMap.size());
        toolMetaDetailVOMap.forEach((toolName, tool) -> {
            ToolMetaDetailVO toolMetaDetailVO = (ToolMetaDetailVO) tool;
            toolMetaDetailVO.setGraphicDetails(null);
            toolMetaDetailVO.setLogo(null);
            //获取正式版本的镜像信息填充toolMetaDetailVO
            toolMetaDetailVO.getToolVersions().forEach(toolversion -> {
                if (finalToolV.equals(toolversion.getVersionType())) {
                    //保存镜像信息
                    toolMetaDetailVO.setDockerTriggerShell(toolversion.getDockerTriggerShell());
                    toolMetaDetailVO.setDockerImageURL(toolversion.getDockerImageURL());
                    toolMetaDetailVO.setDockerImageVersion(toolversion.getDockerImageVersion());
                    toolMetaDetailVO.setToolImageRevision(toolversion.getDockerImageHash());
                    toolMetaDetailVO.setDockerImageVersionType(finalToolV);
                    //保存二进制信息
                    toolMetaDetailVO.setBinary(toolversion.getBinary());
                }
            });

            toolMetaDetailVOList.add(toolMetaDetailVO);
        });

        return toolMetaDetailVOList;
    }

    @Override
    public List<ToolOption> findToolOptionsByToolName(String toolName) {
        if (StringUtils.isNotBlank(toolName)) {
            return toolMetaCacheService.getToolOptionsByToolName(toolName);
        }

        return new ArrayList<>();
    }

    @Override
    public List<ToolMetaDetailVO> getToolsByToolName(List<String> toolNameList) {
        Map<String, ToolMetaBaseVO> toolMetaDetailVOMap =
                toolMetaCacheService.getToolMetaListFromCache(Boolean.TRUE, Boolean.TRUE);

        //设置P-正式发布版本
        String finalToolV = ToolIntegratedStatus.P.name();
        List<ToolMetaDetailVO> toolMetaDetailVOList = new ArrayList<>(toolMetaDetailVOMap.size());
        toolMetaDetailVOMap.forEach((toolName, tool) -> {
            if (toolNameList.contains(toolName)) {
                ToolMetaDetailVO toolMetaDetailVO = (ToolMetaDetailVO) tool;
                toolMetaDetailVO.setGraphicDetails(null);
                toolMetaDetailVO.setLogo(null);
                //获取正式版本的镜像信息填充toolMetaDetailVO
                toolMetaDetailVO.getToolVersions().forEach(toolversion -> {
                    if (finalToolV.equals(toolversion.getVersionType())) {
                        //保存镜像信息
                        toolMetaDetailVO.setDockerTriggerShell(toolversion.getDockerTriggerShell());
                        toolMetaDetailVO.setDockerImageURL(toolversion.getDockerImageURL());
                        toolMetaDetailVO.setDockerImageVersion(toolversion.getDockerImageVersion());
                        toolMetaDetailVO.setToolImageRevision(toolversion.getDockerImageHash());
                        toolMetaDetailVO.setDockerImageVersionType(finalToolV);
                        //保存二进制信息
                        toolMetaDetailVO.setBinary(toolversion.getBinary());
                    }
                });
                toolMetaDetailVOList.add(toolMetaDetailVO);
            }
        });

        return toolMetaDetailVOList;
    }

    @Override
    public String getSCCLangFilterForPreCI() {
        BaseDataEntity entity = baseDataRepository
                .findFirstByParamTypeAndParamCode(ComConstants.PRECI_SCC_LANG_FILTER,
                        ComConstants.PRECI_SCC_LANG_FILTER);

        if (entity == null) {
            log.error("basedata config error: PRECI_SCC_LANG_FILTER is null");

            return "";
        }

        return entity.getParamValue();
    }

    @Override
    public BKToolBasicInfoVO getBKToolBasicInfo(String toolName) {
        BKToolBasicInfoVO result = new BKToolBasicInfoVO(toolName);

        ToolControlInfoEntity toolControlInfo = toolControlInfoDao.findByToolNameAndStatus(toolName, null);
        if (toolControlInfo != null) {
            result.setDevLanguage(toolControlInfo.getDevLanguage());
            result.setToolCnTypes(toolControlInfo.getToolCnTypes());
            result.setNeedBuildScript(toolControlInfo.getNeedBuildScript());
        }

        ToolMetaEntity toolMeta = toolMetaDao.findBasicInfoByToolName(toolName);
        if (toolMeta != null) {
            result.setDisplayName(toolMeta.getDisplayName());
            List<String> langNames = getLangNamesByLangDigits(toolMeta.getLang());
            result.setLangList(langNames);

            result.setBriefIntroduction(toolMeta.getBriefIntroduction());
            if (StringUtils.isBlank(toolMeta.getDescription())) {
                result.setDescription(toolMeta.getBriefIntroduction());
            } else {
                result.setDescription(toolMeta.getDescription());
            }

            Result<Long> checkerNumResult = client.get(BuildCheckerRestResource.class)
                    .getCheckerNumByToolName(toolName);
            if (checkerNumResult.isOk() && checkerNumResult.getData() != null) {
                result.setCheckerNum(checkerNumResult.getData());
            } else {
                log.warn("can not get checker num by tool name({})", toolName);
            }
        }

        return result;
    }

    /**
     * 更新开源扫描工具版本信息
     *
     * @param reqVO    请求体
     * @param userName 更新人
     * @return boolean
     */
    @Override
    public Boolean updateOpenSourceToolVersionInfo(ToolVersionExtVO reqVO, String userName) {
        log.info("updateOpenSourceToolVersionInfo userName: {}, reqVO: {}", userName, reqVO);

        String toolName = reqVO.getToolName();
        String dockerImageVersion = reqVO.getDockerImageVersion();
        String dockerImageHash = reqVO.getDockerImageHash();
        if (StringUtils.isBlank(toolName) || StringUtils.isBlank(dockerImageVersion)
                || StringUtils.isBlank(dockerImageHash)) {
            log.warn("Abort update, param is blank!");
            return false;
        }

        ToolMetaEntity toolMetaEntity = toolMetaRepository.findFirstByName(toolName);
        if (null == toolMetaEntity) {
            log.warn("Abort update, unknown tool: {}", toolName);
            return false;
        }

        List<ToolVersionEntity> toolVersions = toolMetaEntity.getToolVersions();
        if (CollectionUtils.isNotEmpty(toolVersions)) {
            // 取出当前工具版本
            Map<String, ToolVersionEntity> currToolVersionsMap = toolVersions.stream()
                    .collect(Collectors.toMap(ToolVersionEntity::getVersionType, Function.identity(), (k, v) -> v));
            ToolVersionEntity currToolVersion = currToolVersionsMap.get(ToolIntegratedStatus.O.name());
            if (null == currToolVersion) {
                log.warn("Abort update, currToolVersion is not exist!");
                return false;
            }

            // 记录当前的版本添加到last版本
            List<ToolVersionEntity> lastToolVersions =
                    toolMetaEntity.getLastToolVersions() != null ? toolMetaEntity.getLastToolVersions()
                            : Lists.newArrayList();
            Map<String, ToolVersionEntity> lastToolVersionsMap = lastToolVersions.stream()
                    .collect(Collectors.toMap(ToolVersionEntity::getVersionType, Function.identity(), (k, v) -> v));
            ToolVersionEntity lastToolVersion =
                    lastToolVersionsMap.computeIfAbsent(ToolIntegratedStatus.O.name(), k -> new ToolVersionEntity());
            BeanUtils.copyProperties(currToolVersion, lastToolVersion);
            toolMetaEntity.setLastToolVersions(Lists.newArrayList(lastToolVersionsMap.values()));

            // 记录更新时间、更新人
            currToolVersion.setUpdatedDate(System.currentTimeMillis());
            currToolVersion.setUpdatedBy(userName);
            currToolVersion.setDockerImageVersion(dockerImageVersion);
            currToolVersion.setDockerImageHash(dockerImageHash);

            toolMetaEntity.setToolVersions(Lists.newArrayList(currToolVersionsMap.values()));
        }

        toolMetaRepository.save(toolMetaEntity);
        return true;
    }

    @Override
    public List<ToolMetaDetailVO> getToolsByPattern(String pattern) {
        List<ToolMetaEntity> entities = toolMetaRepository.findByPattern(pattern);
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        List<ToolMetaDetailVO> vos = new ArrayList<>();
        for (ToolMetaEntity entity : entities) {
            ToolMetaDetailVO vo = new ToolMetaDetailVO();
            BeanUtils.copyProperties(entity, vo);
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public List<ToolMetaDetailVO> getToolsByType(String type) {
        List<ToolMetaEntity> entities = toolMetaRepository.findByType(type);
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        List<ToolMetaDetailVO> vos = new ArrayList<>();
        for (ToolMetaEntity entity : entities) {
            ToolMetaDetailVO vo = new ToolMetaDetailVO();
            BeanUtils.copyProperties(entity, vo);
            vos.add(vo);
        }
        return vos;
    }
}
