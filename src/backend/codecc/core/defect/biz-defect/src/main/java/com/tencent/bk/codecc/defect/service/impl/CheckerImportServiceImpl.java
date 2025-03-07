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
import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.audit.annotations.AuditAttribute;
import com.tencent.bk.audit.annotations.AuditInstanceRecord;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CustomCheckerProjectRelationshipRepository;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerRepository;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerSetProjectRelationshipRepository;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.CustomCheckerProjectRelationshipEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerPropsEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetProjectRelationshipEntity;
import com.tencent.bk.codecc.defect.service.CheckerImportService;
import com.tencent.bk.codecc.defect.service.ICheckerSetManageBizService;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.CheckerImportVO;
import com.tencent.bk.codecc.defect.vo.enums.CheckerCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerGranularityType;
import com.tencent.bk.codecc.defect.vo.enums.CheckerRecommendType;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSource;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.checkerset.CheckerPropVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.checkerset.CreateCheckerSetReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CheckerConstants;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.audit.ActionAuditRecordContents;
import com.tencent.devops.common.constant.audit.ActionIds;
import com.tencent.devops.common.constant.audit.ResourceTypes;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.GsonUtils;
import com.tencent.devops.common.util.StringCompress;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.ComConstants.KEY_CODE_LANG;
import static com.tencent.devops.common.constant.audit.CodeccAuditAttributeNames.TASK_ID;
import static com.tencent.devops.common.constant.audit.CodeccAuditAttributeNames.TOOL_NAME;

/**
 * 规则导入逻辑实现
 *
 * @version V1.0
 * @date 2020/4/10
 */
@Service
@Slf4j
@Validated
public class CheckerImportServiceImpl implements CheckerImportService {
    @Autowired
    private CheckerRepository checkerRepository;
    @Autowired
    private CheckerSetRepository checkerSetRepository;
    @Autowired
    private ICheckerSetManageBizService checkerSetManageBizService;
    @Autowired
    private ToolMetaCacheService toolMetaCacheService;
    @Autowired
    private Client client;
    @Autowired
    private CheckerSetProjectRelationshipRepository projectRelationshipRepository;
    @Autowired
    private CustomCheckerProjectRelationshipRepository customCheckerProjectRelationshipRepository;

    private static final String ALL_LANGUAGE_STRING = "ALL";

    @Override
    public Map<String, List<CheckerPropVO>> checkerImport(String userName, String projectId,
                                                          CheckerImportVO checkerImportVO) {
        log.info("do checker import, userName:{}, projectId:{}, request:{}",
                userName, projectId, GsonUtils.toJson(checkerImportVO));

        // 查询语言参数列表
        List<BaseDataVO> codeLangParams = getCodeLangParams();

        for (CheckerDetailVO checkerDetailVO : checkerImportVO.getCheckerDetailVOList()) {
            // 规则语言包含"ALL" 则将语言替换成所有语言
            if (CollectionUtils.isNotEmpty(checkerDetailVO.getCheckerLanguage())
                    && checkerDetailVO.getCheckerLanguage().contains(ALL_LANGUAGE_STRING)) {
                Set<String> codeLangSet =
                        codeLangParams.stream().map(BaseDataVO::getLangFullKey).collect(Collectors.toSet());
                checkerDetailVO.setCheckerLanguage(codeLangSet);
            }

            // 如果 publisher 字段为空，用 createdBy 字段填充
            if (StringUtils.isBlank(checkerDetailVO.getPublisher())) {
                checkerDetailVO.setPublisher(checkerDetailVO.getCreatedBy());
            }
        }
        log.info("do checker import checkerImportVO:{}", GsonUtils.toJson(checkerImportVO));

        // 1.校验入参
        validateParam(checkerImportVO, codeLangParams);

        String toolName = checkerImportVO.getToolName();
        List<CheckerDetailEntity> oldCheckerDetailEntityList = checkerRepository.findByToolName(toolName);

        // 2.整理数据
        List<CheckerDetailVO> checkerDetailVOList = checkerImportVO.getCheckerDetailVOList();

        // 重复checker_key规则集成报错
        if (!checkerDetailVOList.isEmpty()) {
            Map<String, List<CheckerDetailVO>> checkerMap = checkerDetailVOList.stream()
                    .collect(Collectors.groupingBy(CheckerDetailVO::getCheckerName));
            List<String> repeatChecker = new ArrayList<>();
            checkerMap.forEach((k, v) -> {
                if (v.size() > 1) {
                    repeatChecker.add(k);
                }
            });
            if (repeatChecker.size() > 0) {
                log.error("import same checker_key, please check it.");
                throw new CodeCCException(CommonMessageCode.KEY_IS_EXIST, new String[]{repeatChecker.toString()});
            }
        }

        // 3.用户自定义类型规则处理
        if (!checkerDetailVOList.isEmpty()
            && CheckerSource.CUSTOM.name().equals(checkerDetailVOList.get(0).getCheckerSource())) {
            //3.1 本次导入规则若为"用户自定义"，则调用saveCustomCheckers方法进行处理
            return saveCustomCheckers(userName, projectId, checkerImportVO, codeLangParams, oldCheckerDetailEntityList);
        }
        // 3.2 校验工具集成的规则列表是否与已存在的用户自定义规则重复
        Set<String> oldCustomCheckersCheckerName = oldCheckerDetailEntityList.stream()
            .filter(checker -> CheckerSource.CUSTOM.name().equals(checker.getCheckerSource()))
            .map(CheckerDetailEntity::getCheckerName)
            .collect(Collectors.toSet());
        String dupCheckers = checkerDetailVOList.stream()
            .map(CheckerDetailVO::getCheckerName)
            .filter(oldCustomCheckersCheckerName::contains)
            .findFirst()
            .orElse(null);
        if (dupCheckers != null) {
            String errMsg = String.format("记录已存在：输入工具 %s 的规则 %s ",checkerImportVO.getToolName(), dupCheckers);
            log.error(errMsg);
            throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{errMsg}, null);
        }
        // 3.3 本次导入规则若为"工具集成"，则过滤掉查询出的工具旧规则列表中的用户自定义规则
        Map<String, CheckerDetailEntity> oldCheckerEntityMap = oldCheckerDetailEntityList.stream()
            .collect(Collectors.toMap(CheckerDetailEntity::getCheckerName, Function.identity()));
        Map<String, CheckerDetailEntity> oldIntegratedCheckerEntityMap = oldCheckerEntityMap.values().stream()
            .filter(checker -> !CheckerSource.CUSTOM.name().equals(checker.getCheckerSource()))
            .collect(Collectors.toMap(CheckerDetailEntity::getCheckerName,Function.identity()));

        // 4.本次集成是否有增加新规则
        String toolDisplayName = toolMetaCacheService.getToolDisplayName(toolName);
        Map<String, List<CheckerPropVO>> checkerSetPropsMap = new HashMap<>();
        Map<String, CreateCheckerSetReqVO> createCheckerSetMap = new HashMap<>();
        List<CheckerDetailEntity> newCheckerDetailEntityList = checkerDetailVOList.stream().map(checkerDetailVO -> {
            // 初始化规则详情对象
            CheckerDetailEntity checkerDetailEntity = getCheckerDetailEntity(userName, toolName,
                oldIntegratedCheckerEntityMap, checkerDetailVO);
            checkerDetailEntity.setLanguage(convertLang(checkerDetailVO.getCheckerLanguage(), codeLangParams));

            // 初始化规则集
            initCheckerSet(toolName, toolDisplayName, checkerSetPropsMap, createCheckerSetMap, checkerDetailEntity,
                    codeLangParams);
            return checkerDetailEntity;
        }).collect(Collectors.toList());

        /* 如果规则不在本次导入规则列表中，则抛异常，不允许删除规则
         * 对于测试状态的规则，则不需要报错，因为存在一种场景：
         * 同时有多个开发同学在开发，其中有A同学在分支A中增加一条规则1，B同学在分支B开发，他们都把代码合入test分支调试，
         * 这时规则1已经导入到数据库中，并且状态为-1（测试），后来B同学测试验证ok，先合并分支B到master发布到生产，
         * 这时问题就来了，分支B中没有规则1，那么集成到时候就会报错"不允许删除规则"。
         * 为了兼容这种场景，对于测试状态的规则，不被当成删除来报错
         */
        Map<String, CheckerDetailEntity> closeCheckerEntityMap = oldIntegratedCheckerEntityMap.values().stream()
                .filter(checker -> checker.getCheckerVersion() != ComConstants.ToolIntegratedStatus.T.value())
                .collect(Collectors.toMap(CheckerDetailEntity::getCheckerKey, Function.identity(), (k, v) -> v));
        if (closeCheckerEntityMap.size() > 0) {
            // 如果规则不在本次导入规则列表中，则将规则状态变更为已关闭
            //            closeCheckerEntityMap.forEach((checkerName, checkerDetailEntity) ->
            //            {
            //                checkerDetailEntity.setStatus(CheckerConstants.CheckerOpenStatus.CLOSE.value());
            //                checkerDetailEntity.setUpdatedBy(userName);
            //                checkerDetailEntity.setUpdatedDate(System.currentTimeMillis());
            //            });
            //            newCheckerDetailEntityList.addAll(oldCheckerEntityMap.values());
            String errMsg = String.format("%s not allow to close checker, please contact the codecc administrator: %s",
                    ComConstants.CheckerImportErrorCode.NODELETE_CHECKER.getValue(), closeCheckerEntityMap.keySet());
            throw new CodeCCException(CommonMessageCode.JSON_PARAM_IS_INVALID, new String[]{errMsg}, null);
        }

        // 5.规则数据入库
        Map<String, String> codeLangParamsMap =
                codeLangParams.stream().collect(Collectors.toMap(BaseDataVO::getLangFullKey, BaseDataVO::getParamName));
        newCheckerDetailEntityList.forEach(it -> {
            Set<String> checkerLanguage =
                    it.getCheckerLanguage().stream().map(codeLangParamsMap::get).collect(Collectors.toSet());
            it.setCheckerLanguage(checkerLanguage);
        });
        checkerRepository.saveAll(newCheckerDetailEntityList);

        // 6.创建或更新全量规则集
        Set<String> checkerSetIds = createCheckerSetMap.keySet();
        List<CheckerSetEntity> checkerSetList = checkerSetRepository.findByCheckerSetIdIn(checkerSetIds);
        Map<String, CheckerSetEntity> checkerSetMap = checkerSetList.stream()
                .collect(Collectors.toMap(CheckerSetEntity::getCheckerSetId, Function.identity(), (k, v) -> k));
        checkerSetPropsMap.forEach((checkerSetId, checkerProps) -> {
            // 如果规则集没有创建过，先创建规则集
            if (checkerSetMap.get(checkerSetId) == null) {
                CreateCheckerSetReqVO createCheckerSetReqVO = createCheckerSetMap.get(checkerSetId);
                createCheckerSetReqVO.setVersion(ComConstants.ToolIntegratedStatus.T.value());
                checkerSetManageBizService.createCheckerSet(userName, projectId, createCheckerSetReqVO);
            }

            // 更新规则集中的规则
            checkerSetManageBizService.updateCheckersOfSet(checkerSetId, userName, checkerProps,
                    Pair.of(ComConstants.ToolIntegratedStatus.T.value(), toolName));
        });

        // 7.更新开源规则集
        List<CheckerSetVO> standardCheckerSetVOList = checkerImportVO.getStandardCheckerSetList();
        if (CollectionUtils.isNotEmpty(standardCheckerSetVOList)) {
            standardCheckerSetVOList.forEach(standardCheckerSetVO -> {
                Pair<String, List<CheckerPropVO>> standardCheckerSetPair = updateStandardCheckerSet(userName, projectId,
                        checkerImportVO, codeLangParams, standardCheckerSetVO);
                checkerSetPropsMap.put(standardCheckerSetPair.getFirst(), standardCheckerSetPair.getSecond());
            });
        }

        return checkerSetPropsMap;
    }

    private Map<String, List<CheckerPropVO>> saveCustomCheckers(String userName, String projectId,
                                                                CheckerImportVO checkerImportVO,
                                                                List<BaseDataVO> codeLangParams,
                                                                List<CheckerDetailEntity> oldCheckerDetailEntityList
                                                                ) {
        // 1.过滤出数据库中该工具已存在的规则名列表
        Set<String> oldCheckersCheckerName = oldCheckerDetailEntityList.stream()
            .map(CheckerDetailEntity::getCheckerName)
            .collect(Collectors.toSet());

        // 2.过滤出数据库中该工具已存在的用户自定义规则
        Map<String, CheckerDetailEntity> oldCustomCheckerEntityMap = oldCheckerDetailEntityList.stream()
            .filter(checker -> CheckerSource.CUSTOM.name().equals(checker.getCheckerSource()))
            .collect(Collectors.toMap(CheckerDetailEntity::getCheckerName, Function.identity(), (k, v) -> v));

        Map<String, List<CheckerPropVO>> checkerPropsMap = new HashMap<>();
        List<CheckerPropVO> checkerPropVOList = new ArrayList<>();
        List<CheckerDetailVO> checkerDetailVOList = checkerImportVO.getCheckerDetailVOList();
        Map<String, String> codeLangParamsMap =
            codeLangParams.stream().collect(Collectors.toMap(BaseDataVO::getLangFullKey, BaseDataVO::getParamName));

        // 3.初始化新增规则对象列表
        List<CheckerDetailEntity> newCheckerDetailEntityList = checkerDetailVOList.stream().map(checkerDetailVO -> {
            // 3.1 该工具的规则已存在
            if (!oldCheckersCheckerName.isEmpty()
                && oldCheckersCheckerName.contains(checkerDetailVO.getCheckerName())) {
                String errMsg = String.format("记录已存在：输入工具 %s 的规则 %s ",
                    checkerImportVO.getToolName(),
                    checkerDetailVO.getCheckerName());
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{errMsg}, null);
            }

            // 3.2 初始化规则详情对象
            CheckerDetailEntity checkerDetailEntity = getCheckerDetailEntity(userName, checkerImportVO.getToolName(),
                oldCustomCheckerEntityMap, checkerDetailVO);
            checkerDetailEntity.setLanguage(convertLang(checkerDetailVO.getCheckerLanguage(), codeLangParams));
            checkerDetailEntity.setCheckerVersion(ComConstants.ToolIntegratedStatus.P.value());
            checkerDetailEntity.setCreatedByProjectId(projectId);
            checkerDetailEntity.setCheckerSource(CheckerSource.CUSTOM.name());
            checkerDetailEntity.setCodeExample(StringCompress.compress(checkerDetailEntity.getCodeExample()));
            Set<String> checkerLanguage =
                checkerDetailEntity.getCheckerLanguage()
                    .stream()
                    .map(codeLangParamsMap::get)
                    .collect(Collectors.toSet());
            checkerDetailEntity.setCheckerLanguage(checkerLanguage);

            // 3.3 初始化方法返回的规则集中的规则props
            CheckerPropVO checkerPropVO = new CheckerPropVO();
            BeanUtils.copyProperties(checkerDetailEntity, checkerPropVO);
            checkerPropVOList.add(checkerPropVO);
            checkerPropsMap.put(checkerPropVO.getCheckerName(),checkerPropVOList);
            return checkerDetailEntity;
        }).collect(Collectors.toList());

        // 4. 保存规则详情
        checkerRepository.saveAll(newCheckerDetailEntityList);


        // 5. 保存规则-项目关系
        List<CustomCheckerProjectRelationshipEntity> customCheckerProjectRelationshipEntityList = checkerDetailVOList
            .stream()
            .map(checkerDetailVO -> {
                CustomCheckerProjectRelationshipEntity entity = new CustomCheckerProjectRelationshipEntity();
                entity.setToolName(checkerImportVO.getToolName());
                entity.setCheckerName(checkerDetailVO.getCheckerName());
                entity.setProjectId(projectId);
                return entity;
            })
            .collect(Collectors.toList());
        customCheckerProjectRelationshipRepository.saveAll(customCheckerProjectRelationshipEntityList);

        return checkerPropsMap;
    }

    @ActionAuditRecord(
            actionId = ActionIds.CREATE_REGEX_RULE,
            instance = @AuditInstanceRecord(
                    resourceType = ResourceTypes.CHECKER,
                    instanceNames = "#checkerImportVO.getCheckerNames()",
                    instanceIds = "#projectId"
            ),
            attributes = {
                    @AuditAttribute(name = TOOL_NAME, value = "#checkerImportVO.toolName")
            },
            content = ActionAuditRecordContents.CREATE_REGEX_RULE
    )
    @Override
    public Boolean customCheckerImport(String userName, String projectId, CheckerImportVO checkerImportVO) {
        String toolName = checkerImportVO.getToolName();
        checkerImportVO.setToolName(toolName.toUpperCase(Locale.ENGLISH));

        // 查询语言参数列表
        List<BaseDataVO> codeLangParams = getCodeLangParams();
        Map<String, String> langMap = codeLangParams.stream()
            .collect(Collectors.toMap(
                BaseDataVO::getParamName,
                BaseDataVO::getLangFullKey
            ));

        for (CheckerDetailVO checkerDetailVO : checkerImportVO.getCheckerDetailVOList()) {
            if (StringUtils.isBlank(checkerDetailVO.getCheckerName())) {
                String errMsg = "输入规则名不能为空";
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }

            // 设置用户自定义规则的默认值
            checkerDetailVO.setCheckerSource(CheckerSource.CUSTOM.name());
            checkerDetailVO.setCheckerRecommend(CheckerRecommendType.USER_DEFINED.name());
            checkerDetailVO.setCheckGranularity(CheckerGranularityType.SINGLE_LINE.getNameEn());
            checkerDetailVO.setEditable(false);

            //将规则支持的语言转换为FullKey形式
            Set<String> convertedLanguages = new HashSet<>();
            for (String language : checkerDetailVO.getCheckerLanguage()) {
                String langKey = langMap.get(language);
                if (langKey == null) {
                    String errMsg = String.format("输入的工具支持语言: %s, 不在取值范围内", language);
                    throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{errMsg}, null);
                } else {
                    convertedLanguages.add(langKey);
                }
            }
            checkerDetailVO.setCheckerLanguage(convertedLanguages);
        }
        return checkerImport(userName, projectId, checkerImportVO).values().stream()
            .anyMatch(CollectionUtils::isNotEmpty);
    }

    /**
     * 更新规范规则集
     *
     * @param userName
     * @param projectId
     * @param checkerImportVO
     * @param codeLangParams
     * @param standardCheckerSetVO
     */
    private Pair<String, List<CheckerPropVO>> updateStandardCheckerSet(
            String userName, String projectId, CheckerImportVO checkerImportVO,
            List<BaseDataVO> codeLangParams, CheckerSetVO standardCheckerSetVO) {
        String standardCheckerSetId = standardCheckerSetVO.getCheckerSetId();
        List<CheckerSetEntity> standardCheckerSetList = checkerSetRepository.findByCheckerSetId(standardCheckerSetId);

        // 转换规则集语言
        long newCodeLang = convertLang(Sets.newHashSet(standardCheckerSetVO.getCheckerSetLang()), codeLangParams);

        if (CollectionUtils.isEmpty(standardCheckerSetList)) {
            CreateCheckerSetReqVO createCheckerSetReqVO = new CreateCheckerSetReqVO();
            BeanUtils.copyProperties(standardCheckerSetVO, createCheckerSetReqVO);
            createCheckerSetReqVO.setVersion(ComConstants.ToolIntegratedStatus.T.value());
            createCheckerSetReqVO.setCodeLang(newCodeLang);
            createCheckerSetReqVO.setCatagories(Lists.newArrayList(CheckerSetCategory.FORMAT.name()));
            checkerSetManageBizService.createCheckerSet(userName, projectId, createCheckerSetReqVO);

            // 更新规则集中的规则
            checkerSetManageBizService.updateCheckersOfSet(standardCheckerSetId, userName,
                    standardCheckerSetVO.getCheckerProps(),
                    Pair.of(ComConstants.ToolIntegratedStatus.T.value(), checkerImportVO.getToolName()));
        } else {
            CheckerSetEntity latestCheckerSet = standardCheckerSetList.stream()
                    .max(Comparator.comparing(CheckerSetEntity::getVersion)).get();
            CheckerSetEntity testCheckerSet = standardCheckerSetList.stream()
                    .filter(it -> ComConstants.ToolIntegratedStatus.T.value() == it.getVersion())
                    .findFirst().orElse(null);
            if (testCheckerSet == null) {
                testCheckerSet = latestCheckerSet;
                testCheckerSet.setEntityId(null);
                testCheckerSet.setVersion(ComConstants.ToolIntegratedStatus.T.value());
            }

            // 校验规则集语言是否改变（规则集语言不允许改变）
            if (newCodeLang != latestCheckerSet.getCodeLang()) {
                log.error("can not change standardCheckerSet Lang! new id:{}, old id:{}", newCodeLang,
                        latestCheckerSet.getCodeLang(), checkerImportVO.getToolName());
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                        new String[]{"standardCheckerSetLang"}, null);
            }
            // 判断规则集是否有变化，包括基本信息(checker_set_name、description)，如果有变化则更新规则集的基本信息
            String newCheckerSetName = standardCheckerSetVO.getCheckerSetName();
            String newDescription = standardCheckerSetVO.getDescription();
            if (newCheckerSetName != null && !newCheckerSetName.equals(latestCheckerSet.getCheckerSetName())) {
                testCheckerSet.setCheckerSetName(newCheckerSetName);
            }
            if (newDescription != null && !newDescription.equals(latestCheckerSet.getDescription())) {
                testCheckerSet.setDescription(newDescription);
            }

            List<CheckerPropsEntity> checkerPropsEntities = Lists.newArrayList();
            if (CollectionUtils.isNotEmpty(standardCheckerSetVO.getCheckerProps())) {
                for (CheckerPropVO checkerPropVO : standardCheckerSetVO.getCheckerProps()) {
                    CheckerPropsEntity checkerPropsEntity = new CheckerPropsEntity();
                    BeanUtils.copyProperties(checkerPropVO, checkerPropsEntity);
                    checkerPropsEntities.add(checkerPropsEntity);
                }
            }
            List<CheckerPropsEntity> oldCheckerProps = testCheckerSet.getCheckerProps();
            testCheckerSet.setCheckerProps(checkerPropsEntities);
            testCheckerSet.setLastUpdateTime(System.currentTimeMillis());
            testCheckerSet.setUpdatedBy(userName);
            checkerSetRepository.save(testCheckerSet);

            // 查询已关联此规则集，且选择了latest版本自动更新的项目数据
            List<CheckerSetProjectRelationshipEntity> projectRelationships =
                    projectRelationshipRepository.findByCheckerSetIdAndUselatestVersion(standardCheckerSetId, true);
            if (CollectionUtils.isNotEmpty(projectRelationships)) {
                // 如果是测试或灰度规则集，且项目是测试，则设置测试或灰度的项目为强制全量，且更新工具
                CheckerSetEntity fromCheckerSet = new CheckerSetEntity();
                fromCheckerSet.setCheckerProps(oldCheckerProps);
                checkerSetManageBizService.updateTaskAfterChangeCheckerSet(testCheckerSet, fromCheckerSet,
                        projectRelationships, userName);
            }
        }
        return Pair.of(standardCheckerSetId, standardCheckerSetVO.getCheckerProps());
    }

    /**
     * 校验入参
     *
     * @param checkerImportVO
     * @param codeLangParams
     */
    private void validateParam(CheckerImportVO checkerImportVO, List<BaseDataVO> codeLangParams) {
        Map<String, BaseDataVO> langMap = new HashMap<>();
        codeLangParams.forEach(baseDataEntity -> {
            langMap.put(baseDataEntity.getLangFullKey(), baseDataEntity);
        });

        List<String> languages = new ArrayList<>();
        checkerImportVO.getCheckerDetailVOList().forEach(item -> languages.addAll(item.getCheckerLanguage()));

        if (CollectionUtils.isNotEmpty(languages) && !langMap.keySet().containsAll(languages)) {
            String errMsg = String.format("%s 输入的工具支持语言: %s, 不在取值范围内: %s",
                    ComConstants.CheckerImportErrorCode.INVALID_LANG.getValue(), languages, langMap.keySet());
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
        }

        Set<String> checkerCategorySet =
                Arrays.stream(CheckerCategory.values()).map(CheckerCategory::name).collect(Collectors.toSet());
        Set<String> checkerRecommendSet = Arrays.stream(CheckerRecommendType.values())
                .map(CheckerRecommendType::name).collect(Collectors.toSet());
        checkerImportVO.getCheckerDetailVOList().forEach(it -> {
            // 检查 checkerCategory
            if (!checkerCategorySet.contains(it.getCheckerCategory())) {
                String errMsg = String.format("输入的规则类型: %s, 不在取值范围内: %s",
                        it.getCheckerCategory(), checkerCategorySet);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
            // 检查 checkerRecommend
            if (!checkerRecommendSet.contains(it.getCheckerRecommend())) {
                String errMsg = String.format("输入的规则推荐类型: %s, 不在取值范围内: %s", it.getCheckerRecommend(),
                        checkerRecommendSet);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
        });

        // 检验开源规则集
        List<CheckerSetVO> standardCheckerSetList = checkerImportVO.getStandardCheckerSetList();
        if (CollectionUtils.isNotEmpty(standardCheckerSetList)) {
            // 查询工具对应的规范规则集ID的配置
            Result<List<BaseDataVO>> result =
                    client.get(ServiceBaseDataResource.class).getInfoByTypeAndCode(ComConstants.STANDARD_CHECKER_SET_ID,
                            checkerImportVO.getToolName());
            if (result.isNotOk() || CollectionUtils.isEmpty(result.getData())
                    || StringUtils.isEmpty(result.getData().get(0).getParamValue())) {
                log.error("param list is empty! paramType: {}, paramCode: {}", ComConstants.STANDARD_CHECKER_SET_ID,
                        checkerImportVO.getToolName());
                throw new CodeCCException(CommonMessageCode.JSON_PARAM_IS_INVALID, String.format(
                        "后台未给工具[%s]配置工具对应的 '规范规则集'，请联系CodeCC配置", checkerImportVO.getToolName()));
            }

            String standardCheckerSetIds = result.getData().get(0).getParamValue();
            List<String> standardCheckerSetIdList =
                    Lists.newArrayList(standardCheckerSetIds.split(ComConstants.STRING_SPLIT));

            Set<String> checkers = checkerImportVO.getCheckerDetailVOList().stream()
                    .map(CheckerDetailVO::getCheckerName).collect(Collectors.toSet());

            Map<String, List<String>> invalidCheckerMap = Maps.newTreeMap();
            checkerImportVO.getStandardCheckerSetList().forEach(standardCheckerSetVO -> {
                String standardCheckerSetId = standardCheckerSetVO.getCheckerSetId();

                // 校验规则集ID是否改变（ID不允许改变）
                if (!standardCheckerSetIdList.contains(standardCheckerSetId)) {
                    log.error("can not change standard checkerSetId! new id:{}, old ids:{}", standardCheckerSetId,
                            standardCheckerSetIdList, checkerImportVO.getToolName());
                    throw new CodeCCException(CommonMessageCode.JSON_PARAM_IS_INVALID, String.format(
                            "checker_set.json中配置的规则集Id与工具[%s]对应的 '规范规则集Id'不匹配，请联系CodeCC确认正确配置",
                            checkerImportVO.getToolName()));
                }

                // 校验是否开源规则集里面的规则是否属于工具的规则
                List<String> invalidCheckers = Lists.newArrayList();
                standardCheckerSetVO.getCheckerProps().forEach(checkerPropVO -> {
                    if (!checkers.contains(checkerPropVO.getCheckerKey())) {
                        invalidCheckers.add(checkerPropVO.getCheckerKey());
                    }
                });
                if (CollectionUtils.isNotEmpty(invalidCheckers)) {
                    invalidCheckerMap.put(standardCheckerSetId, invalidCheckers);
                }
            });
            if (invalidCheckerMap.size() > 0) {
                StringBuffer errMsg = new StringBuffer();
                invalidCheckerMap.forEach((standardCheckerSetId, invalidCheckers) -> errMsg.append(
                        String.format("规则集%s中的规则[%s]不是工具的合法规则; ", standardCheckerSetId, invalidCheckers)));
                log.error(errMsg.toString());
                throw new CodeCCException(CommonMessageCode.JSON_PARAM_IS_INVALID, errMsg.toString());
            }
        }
    }

    /**
     * 初始化规则集，每种语言创建一个规则集
     *
     * @param toolName
     * @param toolDisplayName
     * @param checkerSetPropsMap
     * @param createCheckerSetMap
     * @param checkerDetailEntity
     * @param codeLangParams
     */
    protected void initCheckerSet(String toolName, String toolDisplayName,
                                  Map<String, List<CheckerPropVO>> checkerSetPropsMap,
                                  Map<String, CreateCheckerSetReqVO> createCheckerSetMap,
                                  CheckerDetailEntity checkerDetailEntity, List<BaseDataVO> codeLangParams) {
        // 按语言归类规则，每种语言创建一个规则集
        CheckerPropVO checkerPropVO = new CheckerPropVO();
        BeanUtils.copyProperties(checkerDetailEntity, checkerPropVO);
        Set<String> checkerLanguageSet = checkerDetailEntity.getCheckerLanguage();
        checkerLanguageSet.forEach(lang -> {
            // 默认规则集ID的命名格式：工具名小写_语言_all_checkers, 比如：occheck_oc_all_checkers
            String checkerSetId = String.format("%s_%s_all_checkers", toolName.toLowerCase(), lang.toLowerCase());
            List<CheckerPropVO> checkerPropVOList = checkerSetPropsMap.get(checkerSetId);
            if (checkerPropVOList == null) {
                checkerPropVOList = new ArrayList<>();
                checkerSetPropsMap.put(checkerSetId, checkerPropVOList);

                // 初始化创建规则集请求对象
                String langDisplay = getLangDisplay(lang, codeLangParams);
                String checkerSetName = String.format("%s所有规则(%s)", toolDisplayName, langDisplay);
                CreateCheckerSetReqVO createCheckerSetReqVO = new CreateCheckerSetReqVO();
                createCheckerSetReqVO.setCheckerSetId(checkerSetId);
                createCheckerSetReqVO.setCheckerSetName(checkerSetName);
                createCheckerSetReqVO.setCodeLang(convertLang(Sets.newHashSet(lang), codeLangParams));
                createCheckerSetReqVO.setDescription(String.format("注册工具时系统自动创建的规则集，包含%s语言的所有规则",
                        langDisplay));
                createCheckerSetReqVO.setCatagories(Lists.newArrayList(CheckerSetCategory.DEFECT.name()));
                createCheckerSetMap.put(checkerSetId, createCheckerSetReqVO);
            }
            checkerPropVOList.add(checkerPropVO);
        });
    }

    /**
     * 初始化规则详情对象
     *
     * @param userName
     * @param toolName
     * @param oldCheckerEntityMap
     * @param checkerDetailVO
     * @return
     */
    @NotNull
    protected CheckerDetailEntity getCheckerDetailEntity(String userName, String toolName, Map<String,
            CheckerDetailEntity> oldCheckerEntityMap, CheckerDetailVO checkerDetailVO) {
        String checkerName = checkerDetailVO.getCheckerName();
        CheckerDetailEntity checkerDetailEntity = oldCheckerEntityMap.get(checkerName);
        if (checkerDetailEntity == null) {
            checkerDetailEntity = new CheckerDetailEntity();
            checkerDetailEntity.setCreatedBy(userName);
            checkerDetailEntity.setCreatedDate(System.currentTimeMillis());
            checkerDetailEntity.setCheckerVersion(ComConstants.ToolIntegratedStatus.T.value());
            checkerDetailEntity.setStatus(CheckerConstants.CheckerOpenStatus.OPEN.value());
        } else {
            checkerDetailEntity.setUpdatedBy(userName);
            checkerDetailEntity.setUpdatedDate(System.currentTimeMillis());
            oldCheckerEntityMap.remove(checkerName);
        }

        checkerDetailEntity.setToolName(toolName);
        checkerDetailEntity.setCheckerKey(checkerName);
        checkerDetailEntity.setCheckerName(checkerName);
        checkerDetailEntity.setSeverity(checkerDetailVO.getSeverity());
        checkerDetailEntity.setCheckerCategory(checkerDetailVO.getCheckerCategory());
        checkerDetailEntity.setCheckerDesc(checkerDetailVO.getCheckerDesc());
        checkerDetailEntity.setCheckerDescModel(checkerDetailVO.getCheckerDescModel());
        checkerDetailEntity.setCheckerLanguage(checkerDetailVO.getCheckerLanguage());
        checkerDetailEntity.setErrExample(checkerDetailVO.getErrExample());
        checkerDetailEntity.setCodeExample(checkerDetailVO.getCodeExample());
        checkerDetailEntity.setRightExample(checkerDetailVO.getCodeExample());
        checkerDetailEntity.setCheckerRecommend(checkerDetailVO.getCheckerRecommend());
        checkerDetailEntity.setCheckerTag(checkerDetailVO.getCheckerTag());
        checkerDetailEntity.setProps(CollectionUtils.isEmpty(checkerDetailVO.getCheckerProps()) ? null :
                GsonUtils.toJson(checkerDetailVO.getCheckerProps()));
        checkerDetailEntity.setNativeChecker(true);
        checkerDetailEntity.setPublisher(checkerDetailVO.getPublisher());

        if (checkerDetailVO.getEditable() != null) {
            checkerDetailEntity.setEditable(checkerDetailVO.getEditable());
        } else {
            checkerDetailEntity.setEditable(!CollectionUtils.isEmpty(checkerDetailVO.getCheckerProps()));
        }

        if (checkerDetailVO.getCheckGranularity() != null) {
            checkerDetailEntity.setCheckGranularity(checkerDetailVO.getCheckGranularity());
        }

        return checkerDetailEntity;
    }

    private long convertLang(Set<String> supportedLanguages, List<BaseDataVO> codeLangParams) {
        Map<String, BaseDataVO> langMap = codeLangParams.stream().collect(Collectors.toMap(BaseDataVO::getLangFullKey,
                Function.identity()));

        long lang = 0;
        if (CollectionUtils.isNotEmpty(supportedLanguages)) {
            for (String langStr : supportedLanguages) {
                lang += Long.valueOf(langMap.get(langStr).getParamCode());
            }
        }

        return lang;
    }

    private String getLangDisplay(String language, List<BaseDataVO> codeLangParams) {
        Map<String, BaseDataVO> langMap = codeLangParams.stream().collect(Collectors.toMap(BaseDataVO::getLangFullKey,
                Function.identity()));

        BaseDataVO langVO = langMap.get(language);
        if (langVO != null) {
            language = langVO.getParamName();
        }
        return language;
    }

    /**
     * 查询语言参数列表数据
     */
    private List<BaseDataVO> getCodeLangParams() {
        Result<List<BaseDataVO>> paramsResult =
            client.get(ServiceBaseDataResource.class).getParamsByType(KEY_CODE_LANG);
        if (paramsResult.isNotOk() || CollectionUtils.isEmpty(paramsResult.getData())) {
            log.error("param list is empty! param type: {}", KEY_CODE_LANG);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        return paramsResult.getData();
    }

}
