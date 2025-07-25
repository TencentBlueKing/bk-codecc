/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.vo.checkerset.CheckerSetPackageVO;
import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.dao.CommonDao;
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolMetaRepository;
import com.tencent.bk.codecc.task.model.BaseDataEntity;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolMetaEntity;
import com.tencent.bk.codecc.task.service.MetaService;
import com.tencent.bk.codecc.task.vo.MetadataVO;
import com.tencent.bk.codecc.task.vo.OpenScanAndEpcToolNameMapVO;
import com.tencent.bk.codecc.task.vo.OpenScanAndPreProdCheckerSetMapVO;
import com.tencent.bk.codecc.task.vo.checkerset.OpenSourceCheckerSetVO;
import com.tencent.devops.common.api.ToolMetaBaseVO;
import com.tencent.devops.common.api.ToolMetaDetailVO;
import com.tencent.devops.common.api.annotation.I18NResponse;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.CompressionUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import com.tencent.devops.project.api.service.ServiceProjectResource;
import com.tencent.devops.project.pojo.ProjectVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

    /**
     * 当前项目是否在工具的可见范围内
     * @date 2024/8/30
     * @param orgIds    当前项目所属的组织的 id 的列表 (包括所有的祖先组织)
     * @param projectId 当前项目 id
     * @param toolMetaVO
     * @return boolean
     */
    private boolean inVisibleRange(List<String> orgIds, String projectId, ToolMetaBaseVO toolMetaVO) {
        // 兼容老逻辑. 如果这 2 个字段都为 null, 就认为这个工具还没有引入可见范围的概念, 直接认定工具在可见范围内.
        if (toolMetaVO.getVisibleOrgIds() == null && toolMetaVO.getVisibleProjects() == null) {
            return true;
        }

        if (CollectionUtils.isNotEmpty(toolMetaVO.getVisibleProjects())
                && toolMetaVO.getVisibleProjects().contains(projectId)) {
            return true;
        }
        if (CollectionUtils.isNotEmpty(toolMetaVO.getVisibleOrgIds()) && CollectionUtils.isNotEmpty(orgIds)
                && orgIds.stream().anyMatch(toolMetaVO.getVisibleOrgIds()::contains)) {
            return true;
        }

        return false;
    }

    @Override
    public List<ToolMetaBaseVO> toolList(String projectId, Boolean isDetail) {
        // 1.查询工具列表
        /*HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String userId = request.getHeader(AUTH_HEADER_DEVOPS_USER_ID);
        boolean isAdmin = bkAuthExPermissionApi.isAdminMember(userId);*/
        // 该变量已废弃，仅作mock
        boolean isAdmin = true;

        Map<String, ToolMetaBaseVO> toolMap = toolMetaCache.getToolMetaListFromCache(isDetail, isAdmin);
        if (toolMap == null || toolMap.size() == 0) {
            toolMap = getToolMetaListFromDB(isDetail, isAdmin);
        }

        List<String> orgIds = new ArrayList<>();
        if (StringUtils.isNotBlank(projectId)) {
            com.tencent.devops.project.pojo.Result<ProjectVO> result =
                    client.getDevopsService(ServiceProjectResource.class).get(projectId);
            if (result.isOk() && result.getData() != null) {
                ProjectVO projectVO = result.getData();
                if (StringUtils.isNotBlank(projectVO.getBgId())) {
                    orgIds.add(projectVO.getBgId());
                }
                if (StringUtils.isNotBlank(projectVO.getDeptId())) {
                    orgIds.add(projectVO.getDeptId());
                }
                if (StringUtils.isNotBlank(projectVO.getCenterId())) {
                    orgIds.add(projectVO.getCenterId());
                }
            }
        }

        // 2.对工具进行排序
        List<ToolMetaBaseVO> toolList = null;
        if (toolMap.size() > 0) {
            toolList = new ArrayList<>(toolMap.size());
            String orderToolIds = commonDao.getToolOrder();
            String[] toolIDArr = orderToolIds.split(",");
            for (String id : toolIDArr) {
                ToolMetaBaseVO toolMetaVO = toolMap.get(id);
                if (toolMetaVO == null) {
                    continue;
                }
                // projectId不为空，需要过滤项目是否在工具的可见范围
                if (projectId == null || inVisibleRange(orgIds, projectId, toolMetaVO)) {
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
        Map<String, List<MetadataVO>> metadataMap = Maps.newHashMapWithExpectedSize(metadataTypeArr.length);

        // t_base_data表与MetadataVO的映射关系，如循环里面的赋值关系。另外，元数据在前端的展示顺序映射到param_extend3
        List<BaseDataEntity> baseDataList = baseDataRepository.findByParamTypeInOrderByParamExtend3(metadataTypes);

        // 按照数字而不是字符串顺序排序
        baseDataList.sort(Comparator.comparingInt(o -> NumberUtils.toInt(o.getParamExtend3())));

        // toolType国际化
        String toolTypeFlag = "TOOL_TYPE";
        List<BaseDataEntity> toolTypeList = Lists.newArrayList();

        for (BaseDataEntity baseDataEntity : baseDataList) {
            if (toolTypeFlag.equals(baseDataEntity.getParamType())) {
                toolTypeList.add(baseDataEntity);
                continue;
            }

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

        if (!CollectionUtils.isEmpty(toolTypeList)) {
            List<MetadataVO> metadataVOS =
                    SpringContextUtil.Companion.getBean(MetaService.class).convertToI18N(toolTypeList);
            metadataMap.put(toolTypeFlag, metadataVOS);
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

    @I18NResponse
    @Override
    public List<MetadataVO> convertToI18N(List<BaseDataEntity> baseDataEntityList) {
        if (CollectionUtils.isEmpty(baseDataEntityList)) {
            return Lists.newArrayList();
        }

        List<MetadataVO> retList = Lists.newArrayListWithExpectedSize(baseDataEntityList.size());

        for (BaseDataEntity baseDataEntity : baseDataEntityList) {
            MetadataVO metadataVO = MetadataVO.builder()
                    .entityId(baseDataEntity.getEntityId())
                    .key(baseDataEntity.getParamCode())
                    .name(baseDataEntity.getParamName())
                    .fullName(baseDataEntity.getParamExtend1())
                    .status(baseDataEntity.getParamStatus())
                    .aliasNames(baseDataEntity.getParamExtend2())
                    .creator(baseDataEntity.getCreatedBy())
                    .createTime(baseDataEntity.getCreatedDate())
                    .langFullKey(baseDataEntity.getLangFullKey())
                    .langType(baseDataEntity.getLangType())
                    .build();

            retList.add(metadataVO);
        }

        return retList;
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
    public OpenScanAndEpcToolNameMapVO getOpenScanAndEpcToolNameMap(String projectId) {
        return new OpenScanAndEpcToolNameMapVO();
    }

    @Override
    public OpenScanAndPreProdCheckerSetMapVO getOpenScanAndPreProdCheckerSetMap(String projectId) {
        return new OpenScanAndPreProdCheckerSetMapVO();
    }

    public List<OpenSourceCheckerSetVO> transferCheckerSetToCheckerSetVO(List<CheckerSetPackageVO> checkerSets) {
        return transferCheckerSetToCheckerSetVO(checkerSets, true);
    }

    public List<OpenSourceCheckerSetVO> transferCheckerSetToCheckerSetVO(List<CheckerSetPackageVO> checkerSets,
            boolean filter) {
        return checkerSets.stream().filter(it ->
                        !filter || it.getCheckerSetType() == null
                                || it.getCheckerSetType().equals(ComConstants.OpenSourceCheckerSetType.FULL.getType()))
                .map(it -> {
                    OpenSourceCheckerSetVO checkerSetVO = new OpenSourceCheckerSetVO();
                    BeanUtils.copyProperties(it, checkerSetVO);
                    return checkerSetVO;
                }).collect(Collectors.toList());
    }
}
