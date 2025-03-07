package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.constant.LicenseConstantsKt;
import com.tencent.bk.codecc.defect.constant.LicenseTag;
import com.tencent.bk.codecc.defect.dao.SCAQueryWarningParams;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca.LicenseDetailRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca.SCALicenseRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.SCALicenseDao;
import com.tencent.bk.codecc.defect.model.sca.LicenseDetailEntity;
import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity;
import com.tencent.bk.codecc.defect.service.ISCAQueryWarningService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import com.tencent.bk.codecc.defect.vo.sca.SCADefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.sca.SCADefectGroupStatisticVO;
import com.tencent.bk.codecc.defect.vo.sca.SCADefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.sca.SCALicenseDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.sca.SCALicenseDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.sca.SCALicenseDetailVO;
import com.tencent.bk.codecc.defect.vo.sca.SCALicenseVO;
import com.tencent.bk.codecc.defect.vo.sca.SCAQueryWarningPageInitRspVO;
import com.tencent.bk.codecc.task.vo.OpenScanAndPreProdCheckerSetMapVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.pojo.GlobalMessage;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BaseDataCacheService;
import com.tencent.devops.common.service.aop.AbstractI18NResponseAspect;
import com.tencent.devops.common.service.utils.GlobalMessageUtil;
import com.tencent.devops.common.service.utils.I18NUtils;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.GsonUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service("SCALicenseQueryWarningBizService")
public class SCALicenseQueryWarningBizServiceImpl implements ISCAQueryWarningService {

    @Autowired
    private SCALicenseDao scaLicenseDao;

    @Autowired
    private SCALicenseRepository scaLicenseRepository;

    @Autowired
    private LicenseDetailRepository licenseDetailRepository;

    @Autowired
    private BaseDataCacheService baseDataCacheService;

    /**
     * 处理SCA许可证列表查询
     *
     * @param scaQueryWarningParams
     * @param pageNum
     * @param pageSize
     * @param sortField
     * @param sortType
     * @return
     */
    @Override
    public SCALicenseDefectQueryRspVO processQueryWarningRequest(
            SCAQueryWarningParams scaQueryWarningParams,
            int pageNum,
            int pageSize,
            String sortField,
            Sort.Direction sortType
    ) {
        SCALicenseDefectQueryRspVO scaLicenseDefectQueryRspVO = new SCALicenseDefectQueryRspVO();
        scaLicenseDefectQueryRspVO.setLicenseList(
                new Page<>(0, pageNum, pageSize, 0, Lists.newArrayList()));
        SCADefectQueryReqVO request = scaQueryWarningParams.getScaDefectQueryReqVO();
        if (request == null) {
            return scaLicenseDefectQueryRspVO;
        }

        // 更新请求中的状态筛选条件
        Set<String> statusProcessed = ParamUtils.getStatusProcessed(request.getStatus());
        request.setStatus(statusProcessed);

        // 查询告警
        Page<SCALicenseEntity> result = scaLicenseDao.findLicensePageByCondition(
                scaQueryWarningParams,
                pageNum,
                pageSize,
                sortField,
                sortType
        );

        // 实体类转视图类
        List<SCALicenseVO> licenseVOList;
        Map<Integer, Integer> severityCountMap = new HashMap<>();
        List<SCALicenseEntity> records = result.getRecords();
        if (CollectionUtils.isNotEmpty(records)) {
            licenseVOList = records.stream().map(scaLicenseEntity -> {
                SCALicenseVO scaLicenseVO = new SCALicenseVO();
                BeanUtils.copyProperties(scaLicenseEntity, scaLicenseVO);
                int severity = scaLicenseVO.getSeverity() == ComConstants.PROMPT_IN_DB
                        ? ComConstants.PROMPT
                        : scaLicenseVO.getSeverity();
                scaLicenseVO.setSeverity(severity);

                // 更新计数
                severityCountMap.put(severity, severityCountMap.getOrDefault(severity, 0) + 1);
                return scaLicenseVO;
            }).collect(Collectors.toList());

            Page<SCALicenseVO> pageResult = new Page<>(result.getCount(), result.getPage(),
                    result.getPageSize(), result.getTotalPages(), licenseVOList);
            scaLicenseDefectQueryRspVO.setLicenseList(pageResult);
        }

        // 统计结果
        int lowCount = severityCountMap.getOrDefault(ComConstants.PROMPT, 0);
        int mediumCount = severityCountMap.getOrDefault(ComConstants.NORMAL, 0);
        int highCount = severityCountMap.getOrDefault(ComConstants.SERIOUS, 0);
        int unknownCount = severityCountMap.getOrDefault(ComConstants.UNKNOWN, 0);
        int totalCount = lowCount + mediumCount + highCount + unknownCount;

        scaLicenseDefectQueryRspVO.setLowCount(lowCount);
        scaLicenseDefectQueryRspVO.setMediumCount(mediumCount);
        scaLicenseDefectQueryRspVO.setHighCount(highCount);
        scaLicenseDefectQueryRspVO.setUnknownCount(unknownCount);
        scaLicenseDefectQueryRspVO.setTotalCount(totalCount);

        return scaLicenseDefectQueryRspVO;
    }


    /**
     * 处理SCA许可证详情查询
     *
     * @return
     */
    @Override
    public SCALicenseDefectDetailQueryRspVO processQueryWarningDetailRequest(
            SCADefectDetailQueryReqVO requestVO
    ) {
        SCALicenseDefectDetailQueryRspVO responseVO = new SCALicenseDefectDetailQueryRspVO();
        SCALicenseDetailVO licenseDetailVO = new SCALicenseDetailVO();
        Optional<SCALicenseEntity> entityOpt = scaLicenseRepository.findById(requestVO.getEntityId());
        if (!entityOpt.isPresent()) {
            log.info("license not found by entity id {}", requestVO.getEntityId());
            return responseVO;
        }
        SCALicenseEntity entity = entityOpt.get();
        BeanUtils.copyProperties(entity, licenseDetailVO);

        LicenseDetailEntity detailEntity = licenseDetailRepository.findFirstByName(entity.getName());
        if (detailEntity == null) {
            log.info("license detail not found, entity id {}, name: {}",
                    entity.getEntityId(), entity.getName());
            responseVO.setScaLicenseDetailVO(licenseDetailVO);
            return responseVO;
        }

        BeanUtils.copyProperties(detailEntity, licenseDetailVO, "entityId", "status", "severity",
                "required", "unnecessary", "permitted", "forbidden"
        );
        convertLicenseTagToDetail(licenseDetailVO, detailEntity);
        responseVO.setScaLicenseDetailVO(licenseDetailVO);
        return responseVO;
    }

    /**
     * 转换许可证标签基础数据到详情展示格式
     * @param licenseDetailVO 前端展示用VO对象
     * @param licenseDetail 数据库存储的原始许可证详情
     */
    private void convertLicenseTagToDetail(SCALicenseDetailVO licenseDetailVO, LicenseDetailEntity licenseDetail) {
        // 1. 从缓存服务获取许可证标签基础数据
        List<BaseDataVO> licenseTagBaseDataList = baseDataCacheService.getByType(LicenseConstantsKt.LICENSE_TAG_KEY);
        if (CollectionUtils.isEmpty(licenseTagBaseDataList)) {
            return;
        }

        // 2. 按参数代码分组基础数据
        Locale currentLocale = AbstractI18NResponseAspect.getLocale();
        Map<String, List<BaseDataVO>> codeToBaseDataVOs = licenseTagBaseDataList.stream()
                .collect(Collectors.groupingBy(BaseDataVO::getParamCode));

        // 3. 处理必选条款
        licenseDetailVO.setRequired(generateLocalizedTagDetails(licenseDetail.getRequired(),
                codeToBaseDataVOs.get(LicenseTag.REQUIRED.name()), currentLocale));

        // 4. 处理非必要条款
        licenseDetailVO.setUnnecessary(generateLocalizedTagDetails(licenseDetail.getUnnecessary(),
                codeToBaseDataVOs.get(LicenseTag.UNNECESSARY.name()), currentLocale));

        // 5. 处理允许条款
        licenseDetailVO.setPermitted(generateLocalizedTagDetails(licenseDetail.getPermitted(),
                codeToBaseDataVOs.get(LicenseTag.PERMITTED.name()), currentLocale));

        // 6. 处理禁止条款
        licenseDetailVO.setForbidden(generateLocalizedTagDetails(licenseDetail.getForbidden(),
                codeToBaseDataVOs.get(LicenseTag.FORBIDDEN.name()), currentLocale));
    }


    /**
     * 生成本地化的标签详细信息
     * @param licenseTagItems 许可证标签项列表
     * @param licenseTagBaseDataList 标签基础数据列表
     * @param locale 当前语言环境
     * @return 格式化后的标签描述列表
     */
    private List<String> generateLocalizedTagDetails(List<String> licenseTagItems,
                                                    List<BaseDataVO> licenseTagBaseDataList,
                                                    Locale locale) {

        if (CollectionUtils.isEmpty(licenseTagItems) || CollectionUtils.isEmpty(licenseTagBaseDataList)) {
            return Collections.emptyList();
        }
        // 按参数值分组基础数据
        Map<String, List<BaseDataVO>> baseDataByValue = licenseTagBaseDataList.stream()
                .collect(Collectors.groupingBy(BaseDataVO::getParamValue));

        List<String> formattedTags = new ArrayList<>();
        for (String tagKey : licenseTagItems) {
            List<BaseDataVO> matchedData = baseDataByValue.get(tagKey);
            if (CollectionUtils.isEmpty(matchedData)) {
                continue;
            }

            BaseDataVO baseData = matchedData.get(0);
            // 根据语言环境选择展示字段
            if (locale != null && I18NUtils.CN == locale) {
                formattedTags.add(String.format("%s: %s", baseData.getParamExtend1(), baseData.getParamExtend3()));
            } else {
                formattedTags.add(String.format("%s: %s", baseData.getParamExtend2(), baseData.getParamExtend4()));
            }
        }
        return formattedTags;
    }

    @Override
    public Object pageInit(SCAQueryWarningParams scaQueryWarningParams) {
        SCAQueryWarningPageInitRspVO response = new SCAQueryWarningPageInitRspVO();
        SCADefectQueryReqVO request = scaQueryWarningParams.getScaDefectQueryReqVO();
        // 跨任务查询时，不执行聚合统计
        if (Boolean.TRUE.equals(request.getMultiTaskQuery())) {
            return response;
        }

        // 处理严重程度、告警类型条件：置null，因为需要统计所有风险等级和告警类型的数量
        request.setSeverity(null);
        request.setDefectType(null);

        // 处理状态查询条件：默认添加查询待修复的告警
        Set<String> condStatusList = request.getStatus();
        if (CollectionUtils.isEmpty(condStatusList)) {
            condStatusList = new HashSet<>(1);
            condStatusList.add(String.valueOf(ComConstants.DefectStatus.NEW.value()));
            request.setStatus(condStatusList);
        }

        // 将处理后的条件通过scaDefectListQueryParams参数类进行传递
        scaQueryWarningParams.setScaDefectQueryReqVO(request);

        // 获取统计类型
        String statisticType = request.getStatisticType();

        if (ComConstants.StatisticType.STATUS.name().equalsIgnoreCase(statisticType)) {
            // 1. 计算各状态告警数
            statisticByStatus(scaQueryWarningParams, response);
        } else if (ComConstants.StatisticType.SEVERITY.name().equalsIgnoreCase(statisticType)) {
            // 2. 计算各严重级别告警数
            statisticBySeverity(scaQueryWarningParams, response);
        } else {
            log.error("StatisticType is invalid. {}", GsonUtils.toJson(request));
        }

        return response;
    }

    @Override
    public QueryWarningPageInitRspVO processQueryAuthorsRequest(SCAQueryWarningParams scaQueryWarningParams) {
        return new QueryWarningPageInitRspVO();
    }

    /**
     * 统计添加筛选条件后的各状态告警数量
     *
     * @param scaQueryWarningParams
     * @param response
     */
    private void statisticByStatus(
            SCAQueryWarningParams scaQueryWarningParams,
            SCAQueryWarningPageInitRspVO response
    ) {
        SCADefectQueryReqVO request = scaQueryWarningParams.getScaDefectQueryReqVO();
        Set<String> originalStatus = request.getStatus();
        List<SCADefectGroupStatisticVO> groups =
                scaLicenseDao.statisticByStatus(scaQueryWarningParams);

        groups.forEach(it -> {
            int status = it.getStatus();

            if (ComConstants.DefectStatus.NEW.value() == status) {
                response.setExistCount(response.getExistCount() + it.getDefectCount());
            } else if ((ComConstants.DefectStatus.FIXED.value() & status) > 0) {
                response.setFixCount(response.getFixCount() + it.getDefectCount());
            } else if ((ComConstants.DefectStatus.IGNORE.value() & status) > 0) {
                response.setIgnoreCount(response.getIgnoreCount() + it.getDefectCount());
            } else if ((ComConstants.DefectStatus.PATH_MASK.value() & status) > 0
                    || (ComConstants.DefectStatus.CHECKER_MASK.value() & status) > 0) {
                response.setMaskCount(response.getMaskCount() + it.getDefectCount());
            }
        });

        // 若是快照查，则修正统计；快照查已移除"已修复"状态
        if (StringUtils.isNotEmpty(request.getBuildId())) {
            // 已忽略、已屏蔽在多分支下是共享的；而待修复与已修复是互斥的
            response.setExistCount(response.getExistCount() + response.getFixCount());
            response.setFixCount(0);
        }
        request.setStatus(originalStatus);
    }

    /**
     * 统计添加筛选条件后的各风险等级告警数量
     *
     * @param scaQueryWarningParams
     * @param response
     */
    private void statisticBySeverity(
            SCAQueryWarningParams scaQueryWarningParams,
            SCAQueryWarningPageInitRspVO response
    ) {
        List<SCADefectGroupStatisticVO> groups =
                scaLicenseDao.statisticBySeverity(scaQueryWarningParams);

        groups.forEach(it -> {
            if (ComConstants.SERIOUS == it.getSeverity()) {
                response.setSeriousCount(response.getSeriousCount() + it.getDefectCount());
            } else if (ComConstants.NORMAL == it.getSeverity()) {
                response.setNormalCount(response.getNormalCount() + it.getDefectCount());
            } else if (ComConstants.PROMPT_IN_DB == it.getSeverity() || ComConstants.PROMPT == it.getSeverity()) {
                response.setPromptCount(response.getPromptCount() + it.getDefectCount());
            } else if (ComConstants.UNKNOWN == it.getSeverity()) {
                response.setUnknownCount(response.getUnknownCount() + it.getDefectCount());
            }
        });
    }
}
