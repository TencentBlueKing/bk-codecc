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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.codecc.defect.api.ServiceCheckerSetRestResource;
import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.dao.CommonDao;
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolMetaRepository;
import com.tencent.bk.codecc.task.model.BaseDataEntity;
import com.tencent.bk.codecc.task.model.OpenSourceCheckerSet;
import com.tencent.bk.codecc.task.model.SpecialCheckerSetConfig;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolMetaEntity;
import com.tencent.bk.codecc.task.service.MetaService;
import com.tencent.bk.codecc.task.vo.OpenScanAndEpcToolNameMapVO;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.vo.OpenScanAndPreProdCheckerSetMapVO;
import com.tencent.bk.codecc.task.vo.checkerset.OpenSourceCheckerSetVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.CompressionUtils;
import com.tencent.devops.common.codecc.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_USER_ID;

/**
 * 工具元数据业务逻辑处理类
 *
 * @version V1.0
 * @date 2019/4/25
 */
@Service
@Slf4j
public class MetaServiceImpl implements MetaService {
    @Autowired
    private AuthExPermissionApi bkAuthExPermissionApi;

    @Autowired
    private ToolMetaRepository toolMetaRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private BaseDataRepository baseDataRepository;

    @Autowired
    private ToolMetaCacheService toolMetaCache;

    @Autowired
    private CommonDao commonDao;

    @Autowired
    private Client client;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final LoadingCache<String, OpenScanAndEpcToolNameMapVO> openScanAndEpcToolCache = CacheBuilder.newBuilder()
            .refreshAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, OpenScanAndEpcToolNameMapVO>() {
                @Override
                public OpenScanAndEpcToolNameMapVO load(String mockId) {
                    return doGetOpenScanAndEpcToolNameMap();
                }
            });

    private final LoadingCache<String, OpenScanAndPreProdCheckerSetMapVO> openScanAndPreProdCheckerSetCache =
            CacheBuilder.newBuilder()
                    .refreshAfterWrite(30, TimeUnit.SECONDS)
                    .build(new CacheLoader<String, OpenScanAndPreProdCheckerSetMapVO>() {
                        @Override
                        public OpenScanAndPreProdCheckerSetMapVO load(String mockId) {
                            return doGetOpenScanAndPreProdCheckerSetMap();
                        }
                    });

    @Override
    public List<ToolMetaBaseVO> toolList(Boolean isDetail) {
        // 1.查询工具列表
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String userId = request.getHeader(AUTH_HEADER_DEVOPS_USER_ID);
        boolean isAdmin = bkAuthExPermissionApi.isAdminMember(userId);

        Map<String, ToolMetaBaseVO> toolMap = toolMetaCache.getToolMetaListFromCache(isDetail, isAdmin);
        if (toolMap.size() == 0) {
            toolMap = getToolMetaListFromDB(isDetail, isAdmin);
        }


        // 2.对工具进行排序
        List<ToolMetaBaseVO> toolList = null;
        if (toolMap.size() > 0) {
            toolList = new ArrayList<>(toolMap.size());
            String orderToolIds = commonDao.getToolOrder();
            String[] toolIDArr = orderToolIds.split(",");
            for (String id : toolIDArr) {
                ToolMetaBaseVO toolMetaVO = toolMap.get(id);
                if (toolMetaVO != null) {
                    if (ComConstants.Tool.PHPCS.name().equals(id) || ComConstants.Tool.ESLINT.name().equals(id)
                            || ComConstants.Tool.CCN.name().equals(id)) {
                        toolMetaVO.setParams(null);
                    }
                    toolList.add(toolMetaVO);
                }
            }

            // 3.判断工具是否推荐
            isRecommendTool(toolList);
        }

        return toolList;
    }

    /**
     * 获取工具顺序
     *
     * @return
     */
    @Override
    public String getToolOrder() {
        return commonDao.getToolOrder();
    }

    /**
     * 从数据库查询工具列表
     *
     * @param isDetail
     * @param isAdmin
     * @return
     */
    private Map<String, ToolMetaBaseVO> getToolMetaListFromDB(boolean isDetail, boolean isAdmin) {
        List<ToolMetaEntity> toolMetaList;
        if (isAdmin) {
            toolMetaList = toolMetaRepository.findAll();
        } else {
            toolMetaList = toolMetaRepository.findByStatus(ComConstants.ToolIntegratedStatus.P.name());
        }

        Map<String, ToolMetaBaseVO> toolMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(toolMetaList)) {
            for (ToolMetaEntity toolMetaEntity : toolMetaList) {
                // 列表查询不包括图文详情
                toolMetaEntity.setGraphicDetails(null);
                ToolMetaBaseVO toolMetaVO;

                if (Boolean.TRUE.equals(isDetail)) {
                    // 图标不为空时解压图标
                    String logo = toolMetaEntity.getLogo();
                    if (StringUtils.isNotEmpty(logo)) {
                        byte[] compressLogoBytes = logo.getBytes(StandardCharsets.ISO_8859_1);
                        byte[] afterDecompress = CompressionUtils.decompress(compressLogoBytes);
                        if (afterDecompress != null) {
                            logo = new String(afterDecompress, StandardCharsets.UTF_8);
                        }
                        toolMetaEntity.setLogo(logo);
                    }
                    toolMetaVO = new ToolMetaDetailVO();
                } else {
                    toolMetaVO = new ToolMetaBaseVO();
                }
                BeanUtils.copyProperties(toolMetaEntity, toolMetaVO);
                toolMap.put(toolMetaVO.getName(), toolMetaVO);
            }
        }
        return toolMap;
    }

    @Override
    public ToolMetaDetailVO queryToolDetail(String toolName) {
        ToolMetaDetailVO toolMetaDetailVO = toolMetaCache.getToolDetailFromCache(toolName);
        if (toolMetaDetailVO == null) {
            ToolMetaEntity toolMetaEntity = toolMetaRepository.findFirstByName(toolName);

            // 解压图标和图文详情
            String logo = toolMetaEntity.getLogo();
            if (StringUtils.isNotEmpty(logo)) {
                byte[] compressLogoBytes = logo.getBytes(StandardCharsets.ISO_8859_1);
                byte[] afterDecompress = CompressionUtils.decompress(compressLogoBytes);
                toolMetaEntity.setLogo(new String(afterDecompress, StandardCharsets.UTF_8));
            }

            String graphicDetails = toolMetaEntity.getGraphicDetails();
            if (StringUtils.isNotEmpty(graphicDetails)) {
                byte[] compressGraphicDetailsBytes = graphicDetails.getBytes(StandardCharsets.ISO_8859_1);
                byte[] afterDecompress = CompressionUtils.decompress(compressGraphicDetailsBytes);
                toolMetaEntity.setGraphicDetails(new String(afterDecompress, StandardCharsets.UTF_8));
            }

            toolMetaDetailVO = new ToolMetaDetailVO();
            BeanUtils.copyProperties(toolMetaEntity, toolMetaDetailVO);
        }

        return toolMetaDetailVO;
    }

    @Override
    public Map<String, List<MetadataVO>> queryMetadatas(String metadataType) {
        String[] metadataTypeArr = metadataType.split(";");

        List<String> metadataTypes = Arrays.asList(metadataTypeArr);
        Map<String, List<MetadataVO>> metadataMap = new HashMap<>(metadataTypeArr.length);

        // t_base_data表与MetadataVO的映射关系，如循环里面的赋值关系。另外，元数据在前端的展示顺序映射到param_extend3
        List<BaseDataEntity> baseDataList = baseDataRepository.findByParamTypeInOrderByParamExtend3(metadataTypes);

        // 按照数字而不是字符串顺序排序
        baseDataList.sort(Comparator.comparingInt(o -> NumberUtils.toInt(o.getParamExtend3())));

        for (BaseDataEntity baseDataEntity : baseDataList) {
            MetadataVO metadataVO = new MetadataVO();
            metadataVO.setKey(baseDataEntity.getParamCode());
            metadataVO.setName(baseDataEntity.getParamName());
            metadataVO.setFullName(baseDataEntity.getParamExtend1());
            metadataVO.setStatus(baseDataEntity.getParamStatus());
            metadataVO.setAliasNames(baseDataEntity.getParamExtend2());
            metadataVO.setCreator(baseDataEntity.getCreatedBy());
            metadataVO.setCreateTime(baseDataEntity.getCreatedDate());
            metadataVO.setLangFullKey(baseDataEntity.getLangFullKey());
            metadataVO.setLangType(baseDataEntity.getLangType());

            List<MetadataVO> metadataList = metadataMap.get(baseDataEntity.getParamType());
            if (CollectionUtils.isEmpty(metadataList)) {
                metadataList = new ArrayList<>();
                metadataMap.put(baseDataEntity.getParamType(), metadataList);
            }
            metadataList.add(metadataVO);
        }

        if (metadataMap.containsKey(ComConstants.METADATA_TYPE_LANG)
                && CollectionUtils.isNotEmpty(metadataMap.get(ComConstants.METADATA_TYPE_LANG))) {
            //获取排序
            List<String> langOrder = Arrays.asList(redisTemplate.opsForValue()
                    .get(RedisKeyConstants.KEY_LANG_ORDER).split(","));
            //重新排序
            metadataMap.put(ComConstants.METADATA_TYPE_LANG, metadataMap.get(ComConstants.METADATA_TYPE_LANG).stream()
                    .sorted(Comparator.comparingInt(o -> o != null && langOrder.contains(o.getName())
                            ? langOrder.indexOf(o.getName()) : Integer.MAX_VALUE)).collect(Collectors.toList()));
        }

        return metadataMap;
    }

    /**
     * 是否推荐该款工具
     *
     * @param toolList
     */
    private void isRecommendTool(List<ToolMetaBaseVO> toolList) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String taskId = request.getHeader(AUTH_HEADER_DEVOPS_TASK_ID);
        if (StringUtils.isNotEmpty(taskId)) {
            List<TaskInfoEntity> taskEntities = taskRepository.findCodeLangByTaskId(Long.parseLong(taskId));
            TaskInfoEntity taskInfoEntity = CollectionUtils.isEmpty(taskEntities) ? null : taskEntities.get(0);
            if (taskInfoEntity != null) {
                Long codeLang = taskInfoEntity.getCodeLang();
                if (null != codeLang) {
                    for (ToolMetaBaseVO tool : toolList) {
                        long lang = tool.getLang();

                        // 表示的是其他语言 1073741824 2^32
                        if ((codeLang & lang) > 0 || (lang & TaskConstants.OTHER_LANG) > 0) {
                            tool.setRecommend(true);
                        } else {
                            tool.setRecommend(false);
                        }
                    }
                }

            }
        }
    }

    @Override
    public List<String> convertCodeLangToBsString(Long langCode) {
        if (langCode == null) {
            return Collections.emptyList();
        }
        List<MetadataVO> metadataList = queryMetadatas(ComConstants.METADATA_TYPE_LANG)
                .get(ComConstants.METADATA_TYPE_LANG);
        List<String> languageList = new ArrayList<>();
        for (MetadataVO metadataVO : metadataList) {
            if ((Long.parseLong(metadataVO.getKey()) & langCode) != 0L) {
                languageList.add(metadataVO.getLangFullKey());
            }
        }
        if (languageList.isEmpty()) {
            languageList.add("OTHERS");
        }
        return languageList;
    }

    @Override
    public OpenScanAndEpcToolNameMapVO getOpenScanAndEpcToolNameMap() {
        return openScanAndEpcToolCache.getUnchecked("");
    }

    @Override
    public OpenScanAndPreProdCheckerSetMapVO getOpenScanAndPreProdCheckerSetMap() {
        return openScanAndPreProdCheckerSetCache.getUnchecked("");
    }

    private OpenScanAndEpcToolNameMapVO doGetOpenScanAndEpcToolNameMap() {
        //参考：PipelineTaskRegisterServiceImpl#setOpenScanCheckerSetsAccordingToLanguage
        //PipelineTaskRegisterServiceImpl#setEpcScanCheckerSetsAccordingToLanguage

        log.info("start to do get open scan and ecp tool name map");

        List<BaseDataEntity> baseDataList = baseDataRepository.findAllByParamType(ComConstants.KEY_CODE_LANG);
        if (CollectionUtils.isEmpty(baseDataList)) {
            return null;
        }

        HashMap<String, Set<String>> openScanToolNameMap = new HashMap<>();
        HashMap<String, Set<String>> epcToolNameMap = new HashMap<>();

        // 遍历所有语言元数据
        baseDataList.forEach(data -> {
            // 获取开源扫描规则集工具
            openScanToolNameMap.put(data.getLangFullKey(), new HashSet<>());
            if (!CollectionUtils.isEmpty(data.getOpenSourceCheckerSets())) {
                Set<CheckerSetVO> checkerSetVOS = data.getOpenSourceCheckerSets().stream()
                        .map(it -> {
                            CheckerSetVO checkerSetVO = new CheckerSetVO();
                            checkerSetVO.setCheckerSetId(it.getCheckerSetId());
                            checkerSetVO.setVersion(it.getVersion());
                            return checkerSetVO;
                        }).collect(Collectors.toSet());

                // 获取规则集对应的工具，并填充到map
                List<CheckerSetVO> checkerSetList = client.get(ServiceCheckerSetRestResource.class)
                        .queryCheckerSetsForOpenScan(checkerSetVOS).getData();
                if (checkerSetList != null) {
                    checkerSetList.forEach(it -> {
                        openScanToolNameMap.get(data.getLangFullKey()).addAll(it.getToolList());
                    });
                }
            }

            // 获取epc规范规则集工具
            epcToolNameMap.put(data.getLangFullKey(), new HashSet<>());
            if (!CollectionUtils.isEmpty(data.getEpcCheckerSets())) {
                Set<CheckerSetVO> checkerSetVOS = data.getEpcCheckerSets().stream()
                        .map(it -> {
                            CheckerSetVO checkerSetVO = new CheckerSetVO();
                            checkerSetVO.setCheckerSetId(it.getCheckerSetId());
                            checkerSetVO.setVersion(it.getVersion());
                            return checkerSetVO;
                        }).collect(Collectors.toSet());

                // 获取规则集对应的工具，并填充到map
                List<CheckerSetVO> checkerSetList = client.get(ServiceCheckerSetRestResource.class)
                        .queryCheckerSetsForOpenScan(checkerSetVOS).getData();
                if (checkerSetList != null) {
                    checkerSetList.forEach(it -> {
                        epcToolNameMap.get(data.getLangFullKey()).addAll(it.getToolList());
                    });
                }
            }
        });

        log.info("finish to do get open scan and ecp tool name map: {}, {}", openScanToolNameMap, epcToolNameMap);

        return new OpenScanAndEpcToolNameMapVO(openScanToolNameMap, epcToolNameMap);
    }

    private OpenScanAndPreProdCheckerSetMapVO doGetOpenScanAndPreProdCheckerSetMap() {

        log.info("start to get open and pre prod checker set map");

        List<BaseDataEntity> baseDataList = baseDataRepository.findAllByParamType(ComConstants.KEY_CODE_LANG);

        Map<String, List<OpenSourceCheckerSetVO>> openScanCheckerSetMap = new HashMap<>(8);
        Map<String, List<OpenSourceCheckerSetVO>> preProdOpenScanCheckerSetMap = new HashMap<>(8);
        Map<String, List<OpenSourceCheckerSetVO>> communityOpenScanCheckerSetMap = new HashMap<>(8);
        Map<String, List<OpenSourceCheckerSetVO>> preProdCommunityOpenScanCheckerSetMap = new HashMap<>(8);
        Map<String, List<OpenSourceCheckerSetVO>> epcScanCheckerSetMap = new HashMap<>(8);
        Map<String, List<OpenSourceCheckerSetVO>> preProdEpcScanCheckerSetMap = new HashMap<>(8);

        for (BaseDataEntity baseDataEntity : baseDataList) {
            openScanCheckerSetMap.put(baseDataEntity.getLangFullKey(), new ArrayList<>());
            preProdOpenScanCheckerSetMap.put(baseDataEntity.getLangFullKey(), new ArrayList<>());
            communityOpenScanCheckerSetMap.put(baseDataEntity.getLangFullKey(), new ArrayList<>());
            preProdCommunityOpenScanCheckerSetMap.put(baseDataEntity.getLangFullKey(), new ArrayList<>());
            epcScanCheckerSetMap.put(baseDataEntity.getLangFullKey(), new ArrayList<>());
            preProdEpcScanCheckerSetMap.put(baseDataEntity.getLangFullKey(), new ArrayList<>());

            SpecialCheckerSetConfig config = new SpecialCheckerSetConfig();
            if (!StringUtils.isBlank(baseDataEntity.getParamExtend6())) {
                config = JsonUtil.INSTANCE.to(
                        baseDataEntity.getParamExtend6(), new TypeReference<SpecialCheckerSetConfig>() {
                        });
            }

            List<OpenSourceCheckerSetVO> openCheckerSetVOList = getProdCheckerSets(config, baseDataEntity,
                    ComConstants.PROD_TENCENT_OPENSOURCE_CHECKER_SET_TIME_GAP);
            openScanCheckerSetMap.get(baseDataEntity.getLangFullKey()).addAll(openCheckerSetVOList);

            List<OpenSourceCheckerSetVO> epcCheckerSetVOList = getProdCheckerSets(config, baseDataEntity,
                    ComConstants.PROD_TENCENT_EPC_OPENSOURCE_CHECKER_SET_TIME_GAP);
            epcScanCheckerSetMap.get(baseDataEntity.getLangFullKey()).addAll(epcCheckerSetVOList);

            if (!CollectionUtils.isEmpty(config.getProdCommunityOpenScan())) {
                List<OpenSourceCheckerSetVO> checkerSetVOList = config.getProdCommunityOpenScan().stream().map(it -> {
                    OpenSourceCheckerSetVO checkerSetVO = new OpenSourceCheckerSetVO();
                    BeanUtils.copyProperties(it, checkerSetVO);
                    return checkerSetVO;
                }).collect(Collectors.toList());
                communityOpenScanCheckerSetMap.get(baseDataEntity.getLangFullKey()).addAll(checkerSetVOList);
            }

            //preProd
            List<OpenSourceCheckerSetVO> preProdOpenCheckerSetVOList = getPreProdCheckerSets(
                    config, baseDataEntity, ComConstants.PRE_PROD_TENCENT_OPENSOURCE_CHECKER_SET_TIME_GAP);
            preProdOpenScanCheckerSetMap.get(baseDataEntity.getLangFullKey()).addAll(preProdOpenCheckerSetVOList);

            List<OpenSourceCheckerSetVO> preProdEpcCheckerSetVOList = getPreProdCheckerSets(
                    config, baseDataEntity, ComConstants.PRE_PROD_TENCENT_EPC_OPENSOURCE_CHECKER_SET_TIME_GAP);
            preProdEpcScanCheckerSetMap.get(baseDataEntity.getLangFullKey()).addAll(preProdEpcCheckerSetVOList);

            if (!CollectionUtils.isEmpty(config.getPreProdCommunityOpenScan())) {
                List<OpenSourceCheckerSetVO> checkerSetVOList = config.getPreProdCommunityOpenScan()
                        .stream().map(it -> {
                            OpenSourceCheckerSetVO checkerSetVO = new OpenSourceCheckerSetVO();
                            BeanUtils.copyProperties(it, checkerSetVO);
                            return checkerSetVO;
                        }).collect(Collectors.toList());
                preProdCommunityOpenScanCheckerSetMap.get(baseDataEntity.getLangFullKey()).addAll(checkerSetVOList);
            }
        }

        log.info("start to get time gap");
        BaseDataEntity preProdBaseData =
                baseDataRepository.findFirstByParamType(ComConstants.PRE_PROD_TENCENT_OPENSOURCE_CHECKER_SET_TIME_GAP);
        BaseDataEntity prodBaseData =
                baseDataRepository.findFirstByParamType(ComConstants.PROD_TENCENT_OPENSOURCE_CHECKER_SET_TIME_GAP);

        log.info("finish to get prod and pre prod. prod start time is {}, end time is {}, "
                        + "preProd start time is {}, end time is {}", prodBaseData.getParamExtend1(),
                prodBaseData.getParamExtend2(), preProdBaseData.getParamExtend1(), preProdBaseData.getParamExtend2());
        OpenScanAndPreProdCheckerSetMapVO result = new OpenScanAndPreProdCheckerSetMapVO();
        result.setProdOpenScanTimeGap(new OpenScanAndPreProdCheckerSetMapVO.TimeUnit(
                prodBaseData.getParamExtend1(),
                prodBaseData.getParamExtend2()));
        result.setPreProdOpenScanTimeGap(new OpenScanAndPreProdCheckerSetMapVO.TimeUnit(
                preProdBaseData.getParamExtend1(),
                preProdBaseData.getParamExtend2()));

        BaseDataEntity prodCommunityBaseData = baseDataRepository
                .findFirstByParamType(ComConstants.PROD_TENCENT_COMMUNITY_OPENSOURCE_CHECKER_SET_TIME_GAP);
        result.setProdCommunityOpenScanTimeGap(new OpenScanAndPreProdCheckerSetMapVO.TimeUnit(
                prodCommunityBaseData.getParamExtend1(),
                prodCommunityBaseData.getParamExtend2()));
        BaseDataEntity preProdCommunityBaseData = baseDataRepository
                .findFirstByParamType(ComConstants.PRE_PROD_TENCENT_COMMUNITY_OPENSOURCE_CHECKER_SET_TIME_GAP);
        result.setPreProdCommunityOpenScanTimeGap(new OpenScanAndPreProdCheckerSetMapVO.TimeUnit(
                preProdCommunityBaseData.getParamExtend1(),
                preProdCommunityBaseData.getParamExtend2()));

        BaseDataEntity prodEpcBaseData = baseDataRepository
                .findFirstByParamType(ComConstants.PROD_TENCENT_EPC_OPENSOURCE_CHECKER_SET_TIME_GAP);
        result.setProdEpcScanTimeGap(new OpenScanAndPreProdCheckerSetMapVO.TimeUnit(
                prodEpcBaseData.getParamExtend1(),
                prodEpcBaseData.getParamExtend2()));
        BaseDataEntity preProdEpcBaseData = baseDataRepository
                .findFirstByParamType(ComConstants.PRE_PROD_TENCENT_EPC_OPENSOURCE_CHECKER_SET_TIME_GAP);
        result.setPreProdEpcScanTimeGap(new OpenScanAndPreProdCheckerSetMapVO.TimeUnit(
                preProdEpcBaseData.getParamExtend1(),
                preProdEpcBaseData.getParamExtend2()));

        result.setProdOpenScan(openScanCheckerSetMap);
        result.setProdCommunityOpenScan(communityOpenScanCheckerSetMap);
        result.setProdEpcScan(epcScanCheckerSetMap);

        result.setPreProdOpenScan(preProdOpenScanCheckerSetMap);
        result.setPreProdCommunityOpenScan(preProdCommunityOpenScanCheckerSetMap);
        result.setPreProdEpcScan(preProdEpcScanCheckerSetMap);
        return result;
    }

    private List<OpenSourceCheckerSetVO> getProdCheckerSets(SpecialCheckerSetConfig config,
                                                            BaseDataEntity baseDataEntity, String type) {
        List<OpenSourceCheckerSetVO> prodCheckerSetVOList;
        if (type.equals(ComConstants.PROD_TENCENT_OPENSOURCE_CHECKER_SET_TIME_GAP)) {
            if (CollectionUtils.isEmpty(config.getOpenSourceCheckerSets())) {
                log.info("{} open prod config is empty.", baseDataEntity.getLangFullKey());
                prodCheckerSetVOList = transferCheckerSetToCheckerSetVO(baseDataEntity.getOpenSourceCheckerSets());
            } else {
                log.info("{} open prod config is not empty: {}", baseDataEntity.getLangFullKey(),
                        config.getOpenSourceCheckerSets());
                prodCheckerSetVOList = transferCheckerSetToCheckerSetVO(config.getOpenSourceCheckerSets());
            }
        } else {
            if (CollectionUtils.isEmpty(config.getEpcCheckerSets())) {
                log.info("{} epc prod config is empty.", baseDataEntity.getLangFullKey());
                prodCheckerSetVOList = transferCheckerSetToCheckerSetVO(baseDataEntity.getEpcCheckerSets());
            } else {
                log.info("{} epc prod config is not empty: {}", baseDataEntity.getLangFullKey(),
                        config.getEpcCheckerSets());
                prodCheckerSetVOList = transferCheckerSetToCheckerSetVO(config.getEpcCheckerSets());
            }
        }
        return prodCheckerSetVOList;
    }

    private List<OpenSourceCheckerSetVO> getPreProdCheckerSets(SpecialCheckerSetConfig config,
                                                               BaseDataEntity baseDataEntity, String type) {
        List<OpenSourceCheckerSetVO> preProdCheckerSetVOList;
        if (type.equals(ComConstants.PRE_PROD_TENCENT_OPENSOURCE_CHECKER_SET_TIME_GAP)) {
            if (CollectionUtils.isEmpty(config.getPreProdOpenSourceCheckerSets())) {
                log.info("{} open preProd config is empty.", baseDataEntity.getLangFullKey());
                preProdCheckerSetVOList = transferCheckerSetToCheckerSetVO(
                        baseDataEntity.getOpenSourceCheckerSets());
            } else {
                log.info("{} open preProd config is not empty: {}", baseDataEntity.getLangFullKey(),
                        config.getPreProdOpenSourceCheckerSets());
                preProdCheckerSetVOList = transferCheckerSetToCheckerSetVO(
                        config.getPreProdOpenSourceCheckerSets());
            }
        } else {
            if (CollectionUtils.isEmpty(config.getPreProdEpcCheckerSets())) {
                log.info("{} epc prePreProd config is empty.", baseDataEntity.getLangFullKey());
                preProdCheckerSetVOList = transferCheckerSetToCheckerSetVO(baseDataEntity.getEpcCheckerSets());
            } else {
                log.info("{} epc preProd config is not empty: {}", baseDataEntity.getLangFullKey(),
                        config.getPreProdEpcCheckerSets());
                preProdCheckerSetVOList = transferCheckerSetToCheckerSetVO(config.getPreProdEpcCheckerSets());
            }
        }
        return preProdCheckerSetVOList;
    }

    public List<OpenSourceCheckerSetVO> transferCheckerSetToCheckerSetVO(List<OpenSourceCheckerSet> checkerSets) {
        List<OpenSourceCheckerSetVO> checkerSetVOList = checkerSets.stream().filter(it ->
                it.getCheckerSetType() == null || it.getCheckerSetType().equals(ComConstants.OpenSourceCheckerSetType
                        .FULL.getType())).map(it -> {
            OpenSourceCheckerSetVO checkerSetVO = new OpenSourceCheckerSetVO();
            BeanUtils.copyProperties(it, checkerSetVO);
            return checkerSetVO;
        }).collect(Collectors.toList());
        return checkerSetVOList;
    }
}
