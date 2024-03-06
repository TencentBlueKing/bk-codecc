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
import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.api.OpDefectRestResource;
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.BaseDataDao;
import com.tencent.bk.codecc.task.model.BaseDataEntity;
import com.tencent.bk.codecc.task.model.OpenSourceCheckerSet;
import com.tencent.bk.codecc.task.model.SpecialCheckerSetConfig;
import com.tencent.bk.codecc.task.service.BaseDataService;
import com.tencent.bk.codecc.task.vo.DefaultFilterPathVO;
import com.tencent.bk.codecc.task.vo.ReleaseDateVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineTaskLimitVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.OpenSourceCheckerSetVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.utils.PageableUtils;
import com.tencent.devops.common.codecc.util.JsonUtil;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 基础数据服务代码
 *
 * @version V1.0
 * @date 2019/5/28
 */
@Slf4j
@Service
public class BaseDataServiceImpl implements BaseDataService {

    @Autowired
    private BaseDataRepository baseDataRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private BaseDataDao baseDataDao;

    @Autowired
    private Client client;


    @Override
    public List<BaseDataVO> findBaseDataInfoByTypeAndCode(String paramType, String paramCode) {
        List<BaseDataEntity> baseDataEntityList = baseDataRepository.findAllByParamTypeAndParamCode(paramType, paramCode);
        return null == baseDataEntityList ? new ArrayList<>() : baseDataEntityList.stream()
                .map(baseDataEntity ->
                {
                    BaseDataVO baseDataVO = new BaseDataVO();
                    BeanUtils.copyProperties(baseDataEntity, baseDataVO);
                    return baseDataVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<BaseDataVO> findBaseDataInfoByTypeAndCodeList(String paramType, List<String> paramCodeList) {
        List<BaseDataEntity> baseDataEntityList = baseDataRepository.findByParamCodeInAndParamType(paramCodeList,
                paramType);
        return null == baseDataEntityList ? new ArrayList<>() : baseDataEntityList.stream()
                .map(baseDataEntity -> {
                    BaseDataVO baseDataVO = new BaseDataVO();
                    BeanUtils.copyProperties(baseDataEntity, baseDataVO);
                    return baseDataVO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据参数类型查询参数列表
     *
     * @param paramType
     * @return
     */
    @Override
    public List<BaseDataVO> findBaseDataInfoByType(String paramType) {
        List<BaseDataEntity> baseDataEntityList = baseDataRepository.findAllByParamType(paramType);
        return null == baseDataEntityList ? new ArrayList<>() : baseDataEntityList.stream()
                .map(baseDataEntity ->
                {
                    BaseDataVO baseDataVO = new BaseDataVO();
                    BeanUtils.copyProperties(baseDataEntity, baseDataVO);
                    return baseDataVO;
                })
                .collect(Collectors.toList());
    }


    @Override
    public List<BaseDataVO> findBaseDataInfoByTypeAndCodeAndValue(String paramType, String paramCode, String paramValue) {
        List<BaseDataEntity> baseDataEntityList = baseDataRepository.findAllByParamTypeAndParamCodeAndParamValue(paramType, paramCode, paramValue);
        return null == baseDataEntityList ? new ArrayList<>() : baseDataEntityList.stream()
                .map(baseDataEntity ->
                {
                    BaseDataVO baseDataVO = new BaseDataVO();
                    BeanUtils.copyProperties(baseDataEntity, baseDataVO);
                    return baseDataVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public int batchSave(String userId, List<BaseDataVO> baseDataVOList) {
        baseDataVOList.forEach(baseDataVO -> {
            BaseDataEntity entity = new BaseDataEntity();
            BeanUtils.copyProperties(baseDataVO, entity);
            entity.setCreatedDate(System.currentTimeMillis());
            entity.setUpdatedDate(System.currentTimeMillis());
            entity.setCreatedBy(userId);
            entity.setUpdatedBy(userId);
            baseDataRepository.save(entity);
        });
        return baseDataVOList.size();
    }

    @Override
    public int deleteById(String id) {
        baseDataRepository.deleteById(new ObjectId(id));
        return 0;
    }

    /**
     * 更新屏蔽用户名单
     *
     * @param baseDataVO vo
     * @return boolean
     */
    @Override
    public Boolean updateExcludeUserMember(BaseDataVO baseDataVO, String userName) {
        if (baseDataVO == null) {
            log.error("updateExcludeUserMember req body is null!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"baseDataVO"}, null);
        }

        String paramValue = baseDataVO.getParamValue();
        if (StringUtils.isNotBlank(paramValue)) {
            BaseDataEntity entity = baseDataRepository.findFirstByParamType(ComConstants.KEY_EXCLUDE_USER_LIST);
            if (entity == null) {
                entity = new BaseDataEntity();
                entity.setParamType(ComConstants.KEY_EXCLUDE_USER_LIST);
                entity.setParamCode(ComConstants.KEY_EXCLUDE_USER_LIST);
                entity.setCreatedBy(userName);
                entity.setCreatedDate(System.currentTimeMillis());
            }

            entity.setParamValue(paramValue);
            entity.setUpdatedBy(userName);
            entity.setUpdatedDate(System.currentTimeMillis());
            baseDataRepository.save(entity);
            return true;
        }
        return false;
    }


    /**
     * 获取屏蔽用户名单
     *
     * @return list
     */
    @Override
    public List<String> queryMemberListByParamType(String paramType) {
        BaseDataEntity entity = baseDataRepository.findFirstByParamType(paramType);
        if (null == entity) {
            return Lists.newArrayList();
        }
        String excludeUserStr = entity.getParamValue();
        List<String> userList;
        if (StringUtils.isBlank(excludeUserStr)) {
            userList = Lists.newArrayList();
        } else {
            userList = Lists.newArrayList(excludeUserStr.split(ComConstants.SEMICOLON));
        }
        return userList;
    }


    /**
     * 更新管理员名单
     *
     * @param baseDataVO vo
     * @param userName   user
     * @return boolean
     */
    @Override
    public Boolean updateAdminMember(BaseDataVO baseDataVO, String userName) {
        if (baseDataVO == null) {
            log.error("updateAdminMember req body is null!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"baseDataVO"}, null);
        }

        String paramValue = baseDataVO.getParamValue();
        // 参数类型 判断是更改op管理员名单还是平台管理员名单
        String paramType = baseDataVO.getParamType();
        if (StringUtils.isNotBlank(paramValue) && StringUtils.isNotBlank(paramType)) {
            BaseDataEntity entity = baseDataRepository.findFirstByParamType(paramType);
            long currentTimeMillis = System.currentTimeMillis();
            if (entity == null) {
                entity = new BaseDataEntity();
                entity.setParamType(paramType);
                entity.setParamCode(paramType);
                entity.setCreatedBy(userName);
                entity.setCreatedDate(currentTimeMillis);
            }

            entity.setParamValue(paramValue);
            entity.setUpdatedBy(userName);
            entity.setUpdatedDate(currentTimeMillis);
            baseDataRepository.save(entity);

            // 刷新到Redis
            String lastAdminMember = redisTemplate.opsForValue().get(paramType);
            log.info("lastAdminMember: {}", lastAdminMember);
            redisTemplate.opsForValue().set(paramType, paramValue);
            return true;
        }
        return false;
    }

    @Override
    public List<BaseDataVO> findBaseData() {
        List<BaseDataVO> baseDataVOList = new ArrayList<>();
        List<BaseDataEntity> baseDataEntityList =
                baseDataRepository.findByParamTypeIn(Arrays.asList("LANG",
                        ComConstants.KEY_TOOL_ORDER,
                        ComConstants.MAX_BUILD_LIST_SIZE));
        baseDataEntityList.forEach(baseDataEntity -> {
            if (baseDataEntity != null) {
                BaseDataVO baseDataVO = new BaseDataVO();
                BeanUtils.copyProperties(baseDataEntity, baseDataVO);
                List<OpenSourceCheckerSetVO> openSourceCheckerSetVOList = new ArrayList<>();
                if (baseDataEntity.getOpenSourceCheckerSets() != null) {
                    baseDataEntity.getOpenSourceCheckerSets().forEach(openSourceCheckerSet -> {
                        OpenSourceCheckerSetVO openSourceCheckerSetVO = new OpenSourceCheckerSetVO();
                        BeanUtils.copyProperties(openSourceCheckerSet, openSourceCheckerSetVO);
                        openSourceCheckerSetVOList.add(openSourceCheckerSetVO);
                    });
                }
                baseDataVO.setOpenSourceCheckerListVO(openSourceCheckerSetVOList);
                baseDataVOList.add(baseDataVO);
            }
        });
        return baseDataVOList;
    }

    /**
     * 更新开源扫描规则集信息
     *
     * @param reqVO    开源扫描规则集视图
     * @param userName 更新人
     * @return Boolean
     */
    @Override
    public Boolean updateOpenSourceCheckerSetByLang(
            com.tencent.bk.codecc.task.vo.checkerset.OpenSourceCheckerSetVO reqVO, String userName) {
        log.info("updateOpenSourceCheckerSetByLang userId:{} reqVO:{}", userName, reqVO);
        if (Objects.isNull(reqVO)) {
            log.warn("reqVO is null! abort update");
            return false;
        }

        String lang = reqVO.getLang();
        String checkerSetId = reqVO.getCheckerSetId();
        if (StringUtils.isBlank(lang) || StringUtils.isBlank(checkerSetId)) {
            log.warn("param is blank, abort update!");
            return false;
        }

        BaseDataEntity baseDataEntity =
                baseDataRepository.findFirstByParamTypeAndParamCode(ComConstants.KEY_CODE_LANG, lang);
        if (Objects.isNull(baseDataEntity)) {
            log.warn("baseDataEntity param code not found:{}", lang);
            return false;
        }

        boolean forNewAdd = true;
        List<OpenSourceCheckerSet> checkerSets;
        List<OpenSourceCheckerSet> preProdCheckerSets;

        SpecialCheckerSetConfig config;
        String paramExtend6 = baseDataEntity.getParamExtend6();
        if (StringUtils.isNotEmpty(paramExtend6)) {
            config = JsonUtil.INSTANCE.to(paramExtend6, new TypeReference<SpecialCheckerSetConfig>() {
            });
        } else {
            config = new SpecialCheckerSetConfig();
        }

        // 区分内外开源、EPC
        String manageType = reqVO.getManageType();

        // 外网开源
        if (ComConstants.CheckerSetType.COMMUNITY_OPEN_SCAN.value().equals(manageType)) {
            // 预发布版
            preProdCheckerSets = config.getPreProdCommunityOpenScan() != null ? config.getPreProdCommunityOpenScan()
                    : Lists.newArrayList();
            config.setPreProdCommunityOpenScan(preProdCheckerSets);

            // 正式版
            checkerSets = config.getProdCommunityOpenScan() != null ? config.getProdCommunityOpenScan()
                    : Lists.newArrayList();
            config.setProdCommunityOpenScan(checkerSets);

            // EPC
        } else if (ComConstants.CheckerSetType.EPC_SCAN.value().equals(manageType)) {
            List<OpenSourceCheckerSet> epcCheckerSets = baseDataEntity.getEpcCheckerSets();

            // 预发布版
            preProdCheckerSets = config.getPreProdEpcCheckerSets();
            // 如果为空,则同步正式版的规则集
            if (CollectionUtils.isEmpty(preProdCheckerSets)) {
                preProdCheckerSets = Lists.newArrayList();
                if (CollectionUtils.isNotEmpty(epcCheckerSets)) {
                    preProdCheckerSets.addAll(epcCheckerSets);
                }
            }
            config.setPreProdEpcCheckerSets(preProdCheckerSets);

            // 正式版
            checkerSets = config.getEpcCheckerSets();
            if (CollectionUtils.isEmpty(checkerSets)) {
                checkerSets = Lists.newArrayList();
                if (CollectionUtils.isNotEmpty(epcCheckerSets)) {
                    checkerSets.addAll(epcCheckerSets);
                }
            }
            // EPC正式版需要两个字段都同步
            config.setEpcCheckerSets(checkerSets);
            baseDataEntity.setEpcCheckerSets(checkerSets);
            // 默认内网开源
        } else {
            // 正式版原存储字段
            List<OpenSourceCheckerSet> openSourceCheckerSets = baseDataEntity.getOpenSourceCheckerSets();

            // 预发布版
            preProdCheckerSets = config.getPreProdOpenSourceCheckerSets();
            if (CollectionUtils.isEmpty(preProdCheckerSets)) {
                preProdCheckerSets = Lists.newArrayList();
                // 同步正式版的规则集
                if (CollectionUtils.isNotEmpty(openSourceCheckerSets)) {
                    preProdCheckerSets.addAll(openSourceCheckerSets);
                }
            }
            config.setPreProdOpenSourceCheckerSets(preProdCheckerSets);

            // 正式版
            checkerSets = config.getOpenSourceCheckerSets();
            if (Objects.isNull(checkerSets)) {
                checkerSets = Lists.newArrayList();
                // 同步正式版的规则集
                if (CollectionUtils.isNotEmpty(openSourceCheckerSets)) {
                    checkerSets.addAll(openSourceCheckerSets);
                }
            }
            // 内外开源正式版需要两个字段都同步,预发布也存一份
            config.setOpenSourceCheckerSets(checkerSets);
            baseDataEntity.setOpenSourceCheckerSets(checkerSets);
        }

        Set<String> toolList = reqVO.getToolList();
        Integer versionReq = reqVO.getVersion();
        for (int i = 0, checkerSetsSize = checkerSets.size(); i < checkerSetsSize; i++) {
            OpenSourceCheckerSet checkerSet = checkerSets.get(i);
            if (checkerSetId.equals(checkerSet.getCheckerSetId())) {
                forNewAdd = false;
                Integer currVersion = checkerSet.getVersion();
                if (versionReq != null && !versionReq.equals(currVersion)
                        && !ComConstants.ToolIntegratedStatus.PRE_PROD.name().equals(reqVO.getVersionType())) {
                    checkerSet.setLastVersion(currVersion);
                    checkerSet.setVersion(versionReq);
                }

                OpenSourceCheckerSet preProdCheckerSet = preProdCheckerSets.get(i);
                Integer version = preProdCheckerSet.getVersion();
                if (versionReq != null && !versionReq.equals(version)) {
                    preProdCheckerSet.setLastVersion(version);
                    preProdCheckerSet.setVersion(versionReq);
                }

                if (CollectionUtils.isNotEmpty(toolList)) {
                    checkerSet.setToolList(toolList);
                    preProdCheckerSet.setToolList(toolList);
                }
                break;
            }
        }

        // 新增规则集
        if (forNewAdd) {
            OpenSourceCheckerSet openSourceCheckerSet = new OpenSourceCheckerSet();
            BeanUtils.copyProperties(reqVO, openSourceCheckerSet);
            checkerSets.add(openSourceCheckerSet);
            preProdCheckerSets.add(openSourceCheckerSet);
        }

        baseDataEntity.setParamExtend6(JsonUtil.INSTANCE.toJson(config));
        baseDataRepository.save(baseDataEntity);

        return true;
    }

    /**
     * 删除开源扫描规则集配置
     *
     * @param reqVO    请求体
     * @param userName 用户名
     * @return boolean
     */
    @Override
    public Boolean deleteOpenSourceCheckerSetByLang(
            com.tencent.bk.codecc.task.vo.checkerset.OpenSourceCheckerSetVO reqVO, String userName) {
        log.info("deleteOpenSourceCheckerSetByLang userId:{} reqVO:{}", userName, reqVO);

        String lang = reqVO.getLang();
        String checkerSetId = reqVO.getCheckerSetId();
        String manageType = reqVO.getManageType();
        if (StringUtils.isEmpty(lang) || StringUtils.isEmpty(checkerSetId) || StringUtils.isEmpty(manageType)) {
            log.warn("param is blank, abort delete!");
            return false;
        }

        BaseDataEntity baseDataEntity =
                baseDataRepository.findFirstByParamTypeAndParamCode(ComConstants.KEY_CODE_LANG, lang);
        if (Objects.isNull(baseDataEntity)) {
            log.warn("baseDataEntity param code not found:{}", lang);
            return false;
        }

        SpecialCheckerSetConfig config;
        String paramExtend6 = baseDataEntity.getParamExtend6();
        if (StringUtils.isNotEmpty(paramExtend6)) {
            config = JsonUtil.INSTANCE.to(paramExtend6, new TypeReference<SpecialCheckerSetConfig>() {
            });
        } else {
            config = new SpecialCheckerSetConfig();
        }

        // 外网开源
        if (ComConstants.CheckerSetType.COMMUNITY_OPEN_SCAN.value().equals(manageType)) {
            // 预发布版
            List<OpenSourceCheckerSet> preProdCommunityOpenScan = config.getPreProdCommunityOpenScan();
            removeCheckerSetById(checkerSetId, preProdCommunityOpenScan);

            // 正式版
            List<OpenSourceCheckerSet> prodCommunityOpenScan = config.getProdCommunityOpenScan();
            removeCheckerSetById(checkerSetId, prodCommunityOpenScan);
        }

        // EPC
        if (ComConstants.CheckerSetType.EPC_SCAN.value().equals(manageType)) {
            // 预发布版
            List<OpenSourceCheckerSet> preProdEpcCheckerSets = config.getPreProdEpcCheckerSets();
            removeCheckerSetById(checkerSetId, preProdEpcCheckerSets);

            // 正式版
            List<OpenSourceCheckerSet> epcCheckerSets = config.getEpcCheckerSets();
            removeCheckerSetById(checkerSetId, epcCheckerSets);

            List<OpenSourceCheckerSet> epcCheckerSetsBase = baseDataEntity.getEpcCheckerSets();
            removeCheckerSetById(checkerSetId, epcCheckerSetsBase);
        }

        // 默认内网开源
        if (ComConstants.CheckerSetType.OPEN_SCAN.value().equals(manageType)) {
            // 预发布版
            List<OpenSourceCheckerSet> preProdOpenSourceCheckerSets = config.getPreProdOpenSourceCheckerSets();
            removeCheckerSetById(checkerSetId, preProdOpenSourceCheckerSets);

            // 正式版
            List<OpenSourceCheckerSet> openSourceCheckerSets = config.getOpenSourceCheckerSets();
            removeCheckerSetById(checkerSetId, openSourceCheckerSets);

            List<OpenSourceCheckerSet> openSourceCheckerSetsBase = baseDataEntity.getOpenSourceCheckerSets();
            removeCheckerSetById(checkerSetId, openSourceCheckerSetsBase);

        }
        baseDataEntity.setParamExtend6(JsonUtil.INSTANCE.toJson(config));
        baseDataRepository.save(baseDataEntity);
        return true;
    }

    /**
     * 从规则集列表删除指定id的规则集
     *
     * @param checkerSetId          待删除的规则集id
     * @param openSourceCheckerSets 规则集列表
     */
    private void removeCheckerSetById(String checkerSetId, List<OpenSourceCheckerSet> openSourceCheckerSets) {
        if (CollectionUtils.isNotEmpty(openSourceCheckerSets)) {
            openSourceCheckerSets.removeIf(checkerSet -> checkerSetId.equals(checkerSet.getCheckerSetId()));
        }
    }

    /**
     * 获取PreCI规则集配置列表
     *
     * @return
     */
    @Override
    public List<BaseDataVO> getPreCICheckerSetList() {
        // 根据基础数据类型查询基础数据信息
        List<BaseDataEntity> baseDataEntityList = baseDataRepository.findAllByParamType(ComConstants.PRECI_CHECKER_SET);
        log.info("BaseDataServiceImpl, getPreCICheckerSetList: [{}]", baseDataEntityList.size());

        List<BaseDataVO> preCICheckerSetList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(baseDataEntityList)) {
            for (BaseDataEntity baseDataEntity : baseDataEntityList) {
                BaseDataVO baseDataVO = new BaseDataVO();
                BeanUtils.copyProperties(baseDataEntity, baseDataVO);
                preCICheckerSetList.add(baseDataVO);
            }
        }
        return preCICheckerSetList;
    }

    /**
     * 编辑PreCI规则集配置
     *
     * @param userName 用户名
     * @param reqVO    请求体
     * @return boolean
     */
    @Override
    public Boolean editPreCICheckerSet(String userName, BaseDataVO reqVO) {

        if (StringUtils.isEmpty(reqVO.getParamCode()) && StringUtils.isNotEmpty(reqVO.getParamValue())) {
            log.error("paramCode and paramValue is null!");
            return false;
        }
        BaseDataEntity baseDataEntity = baseDataRepository
                .findFirstByParamTypeAndParamCodeAndParamValue(ComConstants.PRECI_CHECKER_SET, reqVO.getParamCode(),
                        reqVO.getParamValue());

        if (null != baseDataEntity) {
            // 编辑
            baseDataEntity.setParamValue(reqVO.getParamValue());
            baseDataEntity.setParamExtend1(reqVO.getParamExtend1());
            baseDataEntity.setUpdatedDate(System.currentTimeMillis());
            baseDataEntity.setUpdatedBy(userName);

            log.info("BaseDataServiceImpl, editPreCICheckerSet: [{}]", baseDataEntity);
            baseDataRepository.save(baseDataEntity);
        } else {
            // 新增
            BaseDataEntity baseDataEntityData = new BaseDataEntity();
            baseDataEntityData.setParamCode(reqVO.getParamCode());

            String checkerSetName =
                    client.get(OpDefectRestResource.class).queryCheckerSetNameByCheckerSetId(reqVO.getParamCode())
                            .getData();

            baseDataEntityData.setParamName(checkerSetName);
            baseDataEntityData.setParamValue(reqVO.getParamValue());
            baseDataEntityData.setParamType(ComConstants.PRECI_CHECKER_SET);
            baseDataEntityData.setCreatedBy(userName);
            long createdAndUpdateTime = System.currentTimeMillis();
            baseDataEntityData.setCreatedDate(createdAndUpdateTime);
            baseDataEntityData.setUpdatedDate(createdAndUpdateTime);
            baseDataEntityData.setUpdatedBy(userName);
            baseDataEntityData.setParamExtend1(reqVO.getParamExtend1());

            log.info("BaseDataServiceImpl, addPreCICheckerSet: [{}]", baseDataEntityData);
            baseDataRepository.save(baseDataEntityData);
        }
        return true;
    }

    /**
     * 删除PreCI规则集配置
     *
     * @param reqVO 请求体
     * @return boolean
     */
    @Override
    public Boolean deletePreCICheckerSet(BaseDataVO reqVO) {
        if (StringUtils.isEmpty(reqVO.getParamCode()) && StringUtils.isNotEmpty(reqVO.getParamValue())) {
            log.error("paramCode and paramValue is null!");
            return false;
        }
        BaseDataEntity baseDataEntity = baseDataRepository
                .findFirstByParamTypeAndParamCodeAndParamValue(ComConstants.PRECI_CHECKER_SET, reqVO.getParamCode(),
                        reqVO.getParamValue());
        log.info("BaseDataServiceImpl, deletePreCICheckerSet: [{}]", baseDataEntity);
        baseDataRepository.delete(baseDataEntity);
        return true;
    }

    /**
     * 根据参数代码获取基础数据
     *
     * @param paramCode
     * @return
     */
    @Override
    public BaseDataVO findBaseDataByCode(String paramCode) {
        BaseDataEntity baseDataEntity =
                baseDataRepository.findFirstByParamTypeAndParamCode(ComConstants.KEY_CODE_LANG, paramCode);
        BaseDataVO baseDataVO = new BaseDataVO();
        if (baseDataEntity != null) {
            BeanUtils.copyProperties(baseDataEntity, baseDataVO);
        }
        return baseDataVO;
    }

    /**
     * 获取内网开源治理发布日期
     *
     * @param manageType  管理类型 内外网、EPC
     * @param versionType 版本类型 PRE_PROD:预发布
     * @return vo
     */
    @Override
    public ReleaseDateVO getReleaseDate(String manageType, String versionType) {
        ReleaseDateVO releaseDateVO = new ReleaseDateVO();
        String paramCode = getReleaseDateParamCode(manageType, versionType);

        BaseDataEntity baseDataEntity = baseDataRepository.findFirstByParamCode(paramCode);
        if (null != baseDataEntity) {
            String paramExtend1 = baseDataEntity.getParamExtend1();
            String paramExtend2 = baseDataEntity.getParamExtend2();

            if (ComConstants.ToolIntegratedStatus.PRE_PROD.name().equals(versionType)) {
                releaseDateVO.setPreProdDate(StringUtils.isNotEmpty(paramExtend1) ? Long.parseLong(paramExtend1) : 0);
                releaseDateVO.setProdDate(StringUtils.isNotEmpty(paramExtend2) ? Long.parseLong(paramExtend2) : 0);
            } else {
                releaseDateVO.setProdDate(StringUtils.isNotEmpty(paramExtend1) ? Long.parseLong(paramExtend1) : 0);
            }
        }

        return releaseDateVO;
    }

    /**
     * 获取对应类型的param code
     */
    @NotNull
    private String getReleaseDateParamCode(String manageType, String versionType) {
        String paramCode;

        // 外网开源
        if (ComConstants.CheckerSetType.COMMUNITY_OPEN_SCAN.value().equals(manageType)) {
            if (ComConstants.ToolIntegratedStatus.PRE_PROD.name().equalsIgnoreCase(versionType)) {
                paramCode = ComConstants.PRE_PROD_TENCENT_COMMUNITY_OPENSOURCE_CHECKER_SET_TIME_GAP;
            } else {
                paramCode = ComConstants.PROD_TENCENT_COMMUNITY_OPENSOURCE_CHECKER_SET_TIME_GAP;
            }
            // EPC
        } else if (ComConstants.CheckerSetType.EPC_SCAN.value().equals(manageType)) {
            if (ComConstants.ToolIntegratedStatus.PRE_PROD.name().equalsIgnoreCase(versionType)) {
                paramCode = ComConstants.PRE_PROD_TENCENT_EPC_OPENSOURCE_CHECKER_SET_TIME_GAP;
            } else {
                paramCode = ComConstants.PROD_TENCENT_EPC_OPENSOURCE_CHECKER_SET_TIME_GAP;
            }
            // 默认内网开源
        } else {
            if (ComConstants.ToolIntegratedStatus.PRE_PROD.name().equalsIgnoreCase(versionType)) {
                paramCode = ComConstants.PRE_PROD_TENCENT_OPENSOURCE_CHECKER_SET_TIME_GAP;
            } else {
                paramCode = ComConstants.PROD_TENCENT_OPENSOURCE_CHECKER_SET_TIME_GAP;
            }
        }
        return paramCode;
    }

    /**
     * 更新内网开源治理发布日期
     *
     * @param reqVO    请求体
     * @param userName 更新人
     * @return boolean
     */
    @Override
    public Boolean updateReleaseDate(ReleaseDateVO reqVO, String userName) {
        log.info("updateReleaseDate user: {}, reqVO: {}", userName, reqVO);

        String manageType = reqVO.getManageType();
        String versionType = reqVO.getVersionType();
        if (StringUtils.isEmpty(manageType) || StringUtils.isEmpty(versionType)) {
            log.warn("param type is empty, abort update!");
            return false;
        }

        String paramCode = getReleaseDateParamCode(manageType, versionType);

        BaseDataEntity baseDataEntity = baseDataRepository.findFirstByParamCode(paramCode);
        if (null == baseDataEntity) {
            baseDataEntity = new BaseDataEntity();
            baseDataEntity.setParamCode(paramCode);
            baseDataEntity.setParamType(paramCode);
            baseDataEntity.setCreatedBy(userName);
        }

        String prodTime = null == reqVO.getProdDate() ? "0" : reqVO.getProdDate().toString();
        if (ComConstants.ToolIntegratedStatus.PRE_PROD.name().equals(versionType)) {
            baseDataEntity.setParamExtend1(null == reqVO.getPreProdDate() ? "0" : reqVO.getPreProdDate().toString());
            baseDataEntity.setParamExtend2(prodTime);
        } else {
            baseDataEntity.setParamExtend1(prodTime);
        }

        baseDataEntity.applyAuditInfoOnUpdate(userName);

        baseDataRepository.save(baseDataEntity);
        return true;
    }

    @Override
    public Set<String> updateLangPreProdConfig(List<CheckerSetVO> checkerSetVOS) {
        List<BaseDataEntity> langList = baseDataRepository.findAllByParamType(ComConstants.KEY_CODE_LANG);
        Map<String, CheckerSetVO> preProdCheckerSetMap = checkerSetVOS.stream().collect(
                Collectors.toMap(CheckerSetVO::getCheckerSetId, Function.identity(), (k, v) -> v));
        Set<String> updateCheckerSetIds = new HashSet<>();
        List<BaseDataEntity> updateCheckerSetList = new ArrayList<>();
        for (BaseDataEntity baseDataEntity : langList) {
            if (StringUtils.isBlank(baseDataEntity.getParamExtend6())) {
                log.info("lang is not set special config and continue: {}", baseDataEntity.getParamName());
                continue;
            }

            boolean isUpdate = false;

            SpecialCheckerSetConfig config = JsonUtil.INSTANCE.to(baseDataEntity.getParamExtend6(),
                    new TypeReference<SpecialCheckerSetConfig>() {
                    });
            if (CollectionUtils.isNotEmpty(config.getPreProdCommunityOpenScan())) {
                for (OpenSourceCheckerSet checkerSet : config.getPreProdCommunityOpenScan()) {
                    if (checkerSet.getVersion() != ComConstants.ToolIntegratedStatus.PRE_PROD.value()) {
                        CheckerSetVO preProdCheckerSet = preProdCheckerSetMap.get(checkerSet.getCheckerSetId());
                        if (preProdCheckerSet != null) {
                            checkerSet.setVersion(ComConstants.ToolIntegratedStatus.PRE_PROD.value());
                            checkerSet.setToolList(preProdCheckerSet.getToolList());
                            updateCheckerSetIds.add(checkerSet.getCheckerSetId());
                            isUpdate = true;
                        }
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(config.getPreProdEpcCheckerSets())) {
                for (OpenSourceCheckerSet checkerSet : config.getPreProdEpcCheckerSets()) {
                    if (checkerSet.getVersion() != ComConstants.ToolIntegratedStatus.PRE_PROD.value()) {
                        CheckerSetVO preProdCheckerSet = preProdCheckerSetMap.get(checkerSet.getCheckerSetId());
                        if (preProdCheckerSet != null) {
                            checkerSet.setVersion(ComConstants.ToolIntegratedStatus.PRE_PROD.value());
                            checkerSet.setToolList(preProdCheckerSet.getToolList());
                            updateCheckerSetIds.add(checkerSet.getCheckerSetId());
                            isUpdate = true;
                        }
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(config.getPreProdOpenSourceCheckerSets())) {
                for (OpenSourceCheckerSet checkerSet : config.getPreProdOpenSourceCheckerSets()) {
                    if (checkerSet.getVersion() != ComConstants.ToolIntegratedStatus.PRE_PROD.value()) {
                        CheckerSetVO preProdCheckerSet = preProdCheckerSetMap.get(checkerSet.getCheckerSetId());
                        if (preProdCheckerSet != null) {
                            checkerSet.setVersion(ComConstants.ToolIntegratedStatus.PRE_PROD.value());
                            checkerSet.setToolList(preProdCheckerSet.getToolList());
                            updateCheckerSetIds.add(checkerSet.getCheckerSetId());
                            isUpdate = true;
                        }
                    }
                }
            }

            if (isUpdate) {
                baseDataEntity.setParamExtend6(JsonUtil.INSTANCE.toJson(config));
                updateCheckerSetList.add(baseDataEntity);
            }
        }
        log.info("start to update pre pro checker set: {}", updateCheckerSetList);
        baseDataRepository.saveAll(updateCheckerSetList);
        return updateCheckerSetIds;
    }

    /**
     * 分页获取系统默认屏蔽路径列表
     *
     * @param paramValue 搜索参数
     * @param pageNum    页码
     * @param pageSize   每页数量
     * @param sortField  排序字段
     * @param sortType   排序类型
     * @return page
     */
    @Override
    public Page<DefaultFilterPathVO> getDefaultFilterPathList(String paramValue, Integer pageNum, Integer pageSize,
                                                              String sortField, String sortType) {
        log.info("getDefaultShieldingPath paramValue: [{}], pageNum: [{}], pageSize:[{}]", paramValue, pageNum,
                pageSize);

        String sortFieldInDb =
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, sortField == null ? "create_date" : sortField);
        Sort.Direction direction = Sort.Direction.fromString(StringUtils.isEmpty(sortType) ? "DESC" : sortType);
        Pageable pageable = PageableUtils.getPageable(pageNum, pageSize, sortFieldInDb, direction, "create_date");

        // 设置查找类型
        String paramType = ComConstants.KEY_DEFAULT_FILTER_PATH;
        // 调用dao层,分页查询屏蔽路径列表
        Page<BaseDataEntity> defaultFilterPathPage =
                baseDataDao.getDefaultFilterPathListByParamValue(paramType, paramValue, pageable);

        List<BaseDataEntity> baseDataEntityList = defaultFilterPathPage.getRecords();

        List<DefaultFilterPathVO> defaultFilterPathVOList = new ArrayList<>();
        for (BaseDataEntity baseDataEntity : baseDataEntityList) {
            DefaultFilterPathVO defaultFilterPathVO = new DefaultFilterPathVO();
            defaultFilterPathVO.setFilterPath(baseDataEntity.getParamValue());
            defaultFilterPathVO.setCreateDate(baseDataEntity.getCreatedDate());
            defaultFilterPathVO.setCreatedBy(baseDataEntity.getCreatedBy());
            defaultFilterPathVOList.add(defaultFilterPathVO);
        }
        log.info("getDefaultFilterPathList defaultFilterPathVOList size:[{}]", defaultFilterPathVOList.size());

        return new Page<>(defaultFilterPathPage.getCount(), defaultFilterPathPage.getPage(),
                defaultFilterPathPage.getPageSize(), defaultFilterPathPage.getTotalPages(), defaultFilterPathVOList);
    }

    /**
     * 删除一条系统默认屏蔽路径
     *
     * @param paramValue 屏蔽路径
     * @return boolean
     */
    @Override
    public Boolean deleteBaseDataEntityByParamValue(String paramValue) {
        log.info("deleteBaseDataEntityByParamValue paramValue:[{}]", paramValue);
        if (StringUtils.isEmpty(paramValue)) {
            log.error("paramValue is null!");
            return false;
        }
        BaseDataEntity baseDataEntity = baseDataRepository
                .findFirstByParamTypeAndParamValue(ComConstants.KEY_DEFAULT_FILTER_PATH, paramValue);
        // 查看被删除屏蔽路径记录
        log.info("deleteBaseDataEntityByParamValue baseDataEntity:[{}]", baseDataEntity);
        baseDataRepository.delete(baseDataEntity);
        return true;
    }

    /**
     * 新增一条系统默认屏蔽路径
     *
     * @param paramValue 屏蔽路径
     * @param createBy   新增路径的管理员名称
     * @return boolean
     */
    @Override
    public Boolean insertBaseDataEntityByParamValueAndCreatedBy(String paramValue, String createBy) {
        if (StringUtils.isEmpty(paramValue) || StringUtils.isEmpty(createBy)) {
            log.error("paramValue or creatBy is null!");
            return false;
        }
        BaseDataEntity baseData = baseDataRepository
                .findFirstByParamTypeAndParamValue(ComConstants.KEY_DEFAULT_FILTER_PATH, paramValue);
        if (baseData == null) {
            String paramType = ComConstants.KEY_DEFAULT_FILTER_PATH;
            BaseDataEntity baseDataEntity = new BaseDataEntity();
            baseDataEntity.setParamValue(paramValue);
            baseDataEntity.setCreatedBy(createBy);
            baseDataEntity.setCreatedDate(System.currentTimeMillis());
            baseDataEntity.setParamType(paramType);
            baseDataRepository.save(baseDataEntity);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 配置preCI用户登录列表初始化天数
     *
     * @param daily 初始化天数
     * @return boolean
     */
    @Override
    public Boolean configurePreCIInitDaily(String daily) {

        if (StringUtils.isBlank(daily)) {
            log.info("configurePreCIInitDaily daily is null!");
            return false;
        }

        // 获取base_data表中配置的参数 查询几天的数据
        List<BaseDataEntity> baseDataEntities =
                baseDataRepository.findParamValueByParamType(ComConstants.PRECI_USER_CRON_JOB_TIME_RANGE);
        BaseDataEntity baseDataEntity = CollectionUtils.isEmpty(baseDataEntities) ? null : baseDataEntities.get(0);
        if (null != baseDataEntity) {
            baseDataEntity.setParamValue(daily);
            baseDataRepository.save(baseDataEntity);
        } else {
            BaseDataEntity addAseDataEntity = new BaseDataEntity();
            addAseDataEntity.setParamType(ComConstants.PRECI_USER_CRON_JOB_TIME_RANGE);
            addAseDataEntity.setParamValue(daily);
            baseDataRepository.save(addAseDataEntity);
        }
        return true;
    }

    /**
     * 分页获取流水线任务数限制设置
     *
     * @param paramCode 流水线id
     * @param pageNum   页码
     * @param pageSize  页数
     * @return page
     */
    @Override
    public Page<PipelineTaskLimitVO> queryPipelineTaskLimitPage(String paramCode, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageableUtils.getPageable(pageNum, pageSize, "updated_date", Sort.Direction.DESC, "");

        Page<BaseDataEntity> entityPage =
                baseDataDao.findPipelineTaskLimitPage(ComConstants.KEY_PIPELINE_TASK_LIMIT, paramCode, pageable);
        List<BaseDataEntity> baseDataEntityList = entityPage.getRecords();


        if (CollectionUtils.isEmpty(baseDataEntityList)) {
            return new Page<>(0, 0, 0, Collections.emptyList());
        }

        List<PipelineTaskLimitVO> data = baseDataEntityList.stream().map(item -> {
            PipelineTaskLimitVO limitVO = new PipelineTaskLimitVO();
            BeanUtils.copyProperties(item, limitVO);

            limitVO.setPipelineTaskLimit(item.getParamValue());
            limitVO.setTarget(item.getParamCode());
            return limitVO;
        }).collect(Collectors.toList());

        return new Page<>(entityPage.getPage(), entityPage.getPageSize(), entityPage.getCount(), data);
    }

    /**
     * 更新流水线任务限制数设置
     *
     * @param reqVO 请求体
     * @return boolean
     */
    @Override
    public Boolean updatePipelineTaskLimit(PipelineTaskLimitVO reqVO) {
        log.info("updatePipelineTaskLimit reqVO: {}", JsonUtil.INSTANCE.toJson(reqVO));
        if (null == reqVO || StringUtils.isEmpty(reqVO.getTarget())) {
            log.error("param is null or empty!");
            return false;
        }
        String pipelineId = reqVO.getTarget();

        BaseDataEntity baseDataEntity = baseDataRepository
                .findFirstByParamTypeAndParamCode(ComConstants.KEY_PIPELINE_TASK_LIMIT, pipelineId);
        if (baseDataEntity == null) {
            baseDataEntity = new BaseDataEntity();
            baseDataEntity.setParamType(ComConstants.KEY_PIPELINE_TASK_LIMIT);
            baseDataEntity.setParamName(ComConstants.KEY_PIPELINE_TASK_LIMIT);
            baseDataEntity.setParamCode(pipelineId);
            baseDataEntity.applyAuditInfoOnCreate(reqVO.getUpdatedBy());
        } else {
            baseDataEntity.applyAuditInfoOnUpdate(reqVO.getUpdatedBy());
        }

        baseDataEntity.setParamValue(
                StringUtils.isNotBlank(reqVO.getPipelineTaskLimit()) ? reqVO.getPipelineTaskLimit() : "0");
        baseDataRepository.save(baseDataEntity);
        log.info("updatePipelineTaskLimit finish!");
        return true;
    }

    /**
     * 删除流水线任务限制数设置
     *
     * @param entityId 实体id
     * @param userId   用户id
     * @return boolean
     */
    @Override
    public Boolean deletePipelineTaskLimit(String entityId, String userId) {
        log.info("deletePipelineTaskLimit entityId: {}, userId: {}", entityId, userId);

        if (StringUtils.isBlank(entityId) || StringUtils.isBlank(userId)) {
            log.warn("entityId or userId is blank! update failed!");
            return false;
        }

        BaseDataEntity baseDataEntity =
                baseDataRepository.findFirstByEntityIdAndParamType(entityId, ComConstants.KEY_PIPELINE_TASK_LIMIT);
        if (baseDataEntity == null) {
            log.warn("PipelineTaskLimit entityId is not found!");
            return false;
        }
        baseDataRepository.delete(baseDataEntity);
        log.info("deletePipelineTaskLimit finish!");
        return true;
    }


    /**
     * 配置工具/语言顺序
     *
     * @param paramType 区分工具/语言
     * @param reqVO     请求体
     * @return boolean
     */
    @Override
    public Boolean editToolLangSort(String paramType, BaseDataVO reqVO) {
        // 参数判空处理
        if (reqVO != null) {
            if (StringUtil.isBlank(reqVO.getParamValue())) {
                log.error("editToolLangSort paramValue is Blank!");
                return false;
            }
        } else {
            log.error("editToolLangSort reqVO is null!");
            return false;
        }
        log.info("editToolLangSort paramType:[{}], paramValue:[{}]", paramType, reqVO.getParamValue());
        if (StringUtil.isNotBlank(paramType)) {
            // 设置mongo中的值
            baseDataDao.editToolLangSort(paramType, reqVO.getParamValue());
            // 设置redis中的值
            redisTemplate.opsForValue().set(paramType, reqVO.getParamValue());
            return true;
        } else {
            log.error("editToolLangSort paramType is Blank!");
            return false;
        }
    }

    /**
     * 根据参数类型，参数名和参数值更新数据
     *
     * @return boolean
     */
    @Override
    public Boolean updateByParamTypeAndParamNameAndParamValue(String paramType, String paramName, String paramValue,
            long updatedDate, String updateBy) {
        // 参数判空处理
        if (StringUtil.isBlank(paramName) || StringUtil.isBlank(paramName) || StringUtil.isEmpty(paramValue)) {
            log.error("update paramName or paramValue or paramType is Blank!");
            return false;
        }
        baseDataDao.updateByParamTypeAndParamNameAndParamValue(paramType, paramName,
                paramValue, updatedDate, updateBy);
        return true;
    }
}
