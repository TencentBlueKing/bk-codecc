package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.constant.DefectMessageCode;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerSetProjectRelationshipRepository;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerSetTaskRelationshipRepository;
import com.tencent.bk.codecc.defect.dao.core.mongotemplate.CheckerSetDao;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerPropsEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetCatagoryEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetProjectRelationshipEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetTaskRelationshipEntity;
import com.tencent.bk.codecc.defect.service.ICheckerSetQueryBizService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.CheckerCommonCountVO;
import com.tencent.bk.codecc.defect.vo.CheckerCountListVO;
import com.tencent.bk.codecc.defect.vo.CheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.OtherCheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetSource;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.bk.codecc.task.api.ServiceGrayToolProjectResource;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.annotation.I18NResponse;
import com.tencent.devops.common.api.checkerset.CheckerPropVO;
import com.tencent.devops.common.api.checkerset.CheckerSetCategoryVO;
import com.tencent.devops.common.api.checkerset.CheckerSetCodeLangVO;
import com.tencent.devops.common.api.checkerset.CheckerSetParamsVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVersionVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CheckerConstants;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.CheckerSetPackageType;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.service.aop.AbstractI18NResponseAspect;
import com.tencent.devops.common.service.utils.I18NUtils;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.List2StrUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

/**
 * V3规则集服务实现类
 *
 * @version V1.0
 * @date 2020/1/2
 */
@Slf4j
@Service
public class CheckerSetQueryBizServiceImpl implements ICheckerSetQueryBizService {

    /**
     * 规则集语言参数
     */
    private static final String KEY_LANG = "LANG";

    @Autowired
    private CheckerSetRepository checkerSetRepository;
    @Autowired
    private CheckerSetDao checkerSetDao;
    @Autowired
    private Client client;
    @Autowired
    private CheckerSetProjectRelationshipRepository checkerSetProjectRelationshipRepository;
    @Autowired
    private CheckerSetTaskRelationshipRepository checkerSetTaskRelationshipRepository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 查询规则集列表
     *
     * @param projectId
     * @param queryCheckerSetReq
     * @return
     */
    @Override
    public Page<CheckerSetVO> getOtherCheckerSets(String projectId, OtherCheckerSetListQueryReq queryCheckerSetReq) {
        if (null == queryCheckerSetReq.getSortType()) {
            queryCheckerSetReq.setSortType(Sort.Direction.DESC);
        }

        if (StringUtils.isEmpty(queryCheckerSetReq.getSortField())) {
            queryCheckerSetReq.setSortField("task_usage");
        }
        int pageNum = Math.max(queryCheckerSetReq.getPageNum() - 1, 0);
        int pageSize = queryCheckerSetReq.getPageSize() <= 0 ? 10 : queryCheckerSetReq.getPageSize();
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(queryCheckerSetReq.getSortType(),
                queryCheckerSetReq.getSortField()));

        // 先查出项目已安装的规则集列表
        Set<String> projectCheckerSetIds = Sets.newHashSet();
        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntities =
                checkerSetProjectRelationshipRepository.findByProjectId(projectId);
        Map<String, Boolean> defaultCheckerSetMap;
        if (CollectionUtils.isNotEmpty(projectRelationshipEntities)) {
            defaultCheckerSetMap = projectRelationshipEntities.stream().collect(Collector.of(HashMap::new, (k, v) ->
                            k.put(v.getCheckerSetId(), v.getDefaultCheckerSet()), (k, v) -> v,
                    Collector.Characteristics.IDENTITY_FINISH
            ));
            for (CheckerSetProjectRelationshipEntity checkerSetProjectRelationshipEntity :
                    projectRelationshipEntities) {
                projectCheckerSetIds.add(checkerSetProjectRelationshipEntity.getCheckerSetId());
            }
        } else {
            defaultCheckerSetMap = new HashMap<>();
        }

        // 获取项目+工具的灰度配置
        Map<String, Integer> toolgrayMap = getProjectToolGrayConfig(projectId).entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().getStatus(), (v1, v2) -> v2));

        Set<CheckerSetCategory> categories = CollectionUtils.isEmpty(queryCheckerSetReq.getCheckerSetCategory())
                ? Collections.emptySet() : queryCheckerSetReq.getCheckerSetCategory().stream()
                .map(CheckerSetCategory::getByName).filter(Objects::nonNull).collect(Collectors.toSet());
        List<CheckerSetEntity> checkerSetEntities =
                checkerSetDao.findMoreByCondition(queryCheckerSetReq.getQuickSearch(),
                        queryCheckerSetReq.getCheckerSetLanguage(), categories, projectCheckerSetIds,
                        queryCheckerSetReq.getProjectInstalled(), toolgrayMap, pageable);

        if (CollectionUtils.isEmpty(checkerSetEntities)) {
            return new PageImpl<>(Lists.newArrayList(), pageable, 0);
        }

        Long coefficient = queryCheckerSetReq.getSortType().equals(Sort.Direction.ASC) ? 1L : -1L;
        List<CheckerSetVO> result = checkerSetEntities.stream().map(checkerSetEntity -> {
                    CheckerSetVO checkerSetVO = new CheckerSetVO();
                    BeanUtils.copyProperties(checkerSetEntity, checkerSetVO, "checkerProps");
                    checkerSetVO.setCodeLangList(List2StrUtil.fromString(checkerSetEntity.getCheckerSetLang(), ","));
                    checkerSetVO.setToolList(Sets.newHashSet());
                    if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())) {
                        for (CheckerPropsEntity checkerPropsEntity : checkerSetEntity.getCheckerProps()) {
                            checkerSetVO.getToolList().add(checkerPropsEntity.getToolName());
                        }
                    }
                    int checkerCount = checkerSetEntity.getCheckerProps() != null
                            ? checkerSetEntity.getCheckerProps().size() : 0;
                    checkerSetVO.setCheckerCount(checkerCount);
                    if (CheckerSetSource.DEFAULT.name().equals(checkerSetEntity.getCheckerSetSource())
                            || CheckerSetSource.RECOMMEND.name().equals(checkerSetEntity.getCheckerSetSource())
                            || projectCheckerSetIds.contains(checkerSetEntity.getCheckerSetId())) {
                        checkerSetVO.setProjectInstalled(true);
                    } else {
                        checkerSetVO.setProjectInstalled(false);
                    }
                    //设置默认标签
                    String checkerSetSource = checkerSetVO.getCheckerSetSource();
                    checkerSetVO.setDefaultCheckerSet((CheckerSetSource.DEFAULT.name().equals(checkerSetSource)
                            && null == defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId())
                            || (null != defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId())
                            && defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId()))));
                    return checkerSetVO;
                }).sorted(Comparator.comparingLong(o -> sortByOfficialProps(o) + coefficient * o.getCreateTime()))
                .collect(Collectors.toList());

        long total = pageNum * pageSize + result.size() + 1;

        //封装分页类
        return new PageImpl<>(result, pageable, total);
    }

    /**
     * 查询规则集列表
     *
     * @param queryCheckerSetReq
     * @return
     */
    @Override
    @I18NResponse
    public List<CheckerSetVO> getCheckerSetsOfProject(CheckerSetListQueryReq queryCheckerSetReq) {
        log.info("start to get checker set of project: {}", queryCheckerSetReq.getProjectId());

        String projectId = queryCheckerSetReq.getProjectId();
        List<CheckerSetProjectRelationshipEntity> checkerSetRelationshipRepositoryList =
                checkerSetProjectRelationshipRepository.findByProjectId(projectId);

        log.info("find project relationship of project: {}", queryCheckerSetReq.getProjectId());

        Set<String> checkerSetIds = checkerSetRelationshipRepositoryList.stream()
                .map(CheckerSetProjectRelationshipEntity::getCheckerSetId).collect(Collectors.toSet());
        Map<String, Integer> checkerSetVersionMap = Maps.newHashMap();
        Map<String, Boolean> checkerSetDefaultMap = Maps.newHashMap();
        Set<String> latestVersionCheckerSets = Sets.newHashSet();
        for (CheckerSetProjectRelationshipEntity projectRelationshipEntity : checkerSetRelationshipRepositoryList) {
            checkerSetVersionMap.put(projectRelationshipEntity.getCheckerSetId(),
                    projectRelationshipEntity.getVersion());
            checkerSetDefaultMap.put(projectRelationshipEntity.getCheckerSetId(),
                    projectRelationshipEntity.getDefaultCheckerSet());
            if (projectRelationshipEntity.getUselatestVersion() != null
                    && projectRelationshipEntity.getUselatestVersion()) {
                latestVersionCheckerSets.add(projectRelationshipEntity.getCheckerSetId());
            }
        }

        log.info("find checkerset by complex condition of project: {}", queryCheckerSetReq.getProjectId());
        List<CheckerSetEntity> pCheckerSetEntityList =
                checkerSetDao.findByComplexCheckerSetCondition(
                        queryCheckerSetReq.getKeyWord(),
                        checkerSetIds,
                        queryCheckerSetReq.getCheckerSetLanguage(),
                        queryCheckerSetReq.getCheckerSetCategory(),
                        queryCheckerSetReq.getToolName(),
                        queryCheckerSetReq.getCheckerSetSource(),
                        queryCheckerSetReq.getCreator(),
                        true,
                        true);

        if (CollectionUtils.isEmpty(pCheckerSetEntityList)) {
            return Collections.emptyList();
        }

        // 获取项目+工具的灰度配置
        Map<String, GrayToolProjectVO> toolGrayMap = getProjectToolGrayConfig(projectId);

        // 查询使用量
        log.info("find checkerset usage of project: {}", queryCheckerSetReq.getProjectId());
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList =
                checkerSetTaskRelationshipRepository.findByProjectId(projectId);
        Map<String, Long> checkerSetCountMap =
                checkerSetTaskRelationshipEntityList.stream().filter(checkerSetTaskRelationshipEntity ->
                                StringUtils.isNotBlank(checkerSetTaskRelationshipEntity.getCheckerSetId()))
                        .collect(Collectors.groupingBy(CheckerSetTaskRelationshipEntity::getCheckerSetId,
                                Collectors.counting()));

        Set<CheckerSetCategory> emptyCheckerSetCategorySet = Sets.newHashSet();
        Set<String> emptyStringSet = Sets.newHashSet();
        Map<String, String> categoryMap = Stream.of(CheckerSetCategory.values()).collect(
                Collectors.toMap(CheckerSetCategory::getName, CheckerSetCategory::getI18nResourceCode, (k1, k2) -> k1)
        );

        // 过滤出符合条件的规则，并转换成VO对象
        List<CheckerSetVO> matchChckerSetVOList = pCheckerSetEntityList.stream().filter(checkerSetEntity ->
                        judgeQualifiedCheckerSet(emptyStringSet, emptyCheckerSetCategorySet, emptyStringSet,
                                queryCheckerSetReq.getCheckerSetSource(), checkerSetEntity))
                .collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId)).entrySet().stream()
                .map(entry -> getCheckerSetVO(entry,
                        checkerSetVersionMap,
                        checkerSetDefaultMap,
                        checkerSetCountMap,
                        latestVersionCheckerSets,
                        toolGrayMap,
                        categoryMap))
                .filter(Objects::nonNull).collect(Collectors.toList());

        log.info("sort checkerset by usage or time of project: {}", queryCheckerSetReq.getProjectId());

        // 按任务使用量排序
        if (CheckerConstants.CheckerSetSortField.TASK_USAGE.name().equals(queryCheckerSetReq.getSortField())) {
            return matchChckerSetVOList.stream()
                    .sorted(Comparator.comparingLong(o -> sortByOfficialProps(o) + (checkerSetCountMap
                            .containsKey(o.getCheckerSetId()) ? -checkerSetCountMap.get(o.getCheckerSetId()) : 0L)))
                    .collect(Collectors.toList());
        } else { // 按创建时间倒序(默认)
            if (StringUtils.isEmpty(queryCheckerSetReq.getSortType())) {
                queryCheckerSetReq.setSortType(Sort.Direction.DESC.name());
            }
            Long coefficient = queryCheckerSetReq.getSortType().equals(Sort.Direction.ASC.name()) ? 1L : -1L;
            return matchChckerSetVOList.stream()
                    .sorted(Comparator.comparingLong(o -> sortByOfficialProps(o) + coefficient * o.getCreateTime()))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 获取项目+工具的灰度配置
     *
     * @param projectId
     * @return
     */
    @NotNull
    private Map<String, GrayToolProjectVO> getProjectToolGrayConfig(String projectId) {
        Result<List<GrayToolProjectVO>> listResult =
                client.get(ServiceGrayToolProjectResource.class).getGrayToolProjectDetail(projectId);

        if (listResult.isNotOk() || listResult.getData() == null) {
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }

        // 按工具维度来映射 ToolIntegratedStatus
        Map<String, GrayToolProjectVO> toolStatusMap = Maps.newHashMap();
        if (CollectionUtils.isEmpty(listResult.getData())) {
            return toolStatusMap;
        }
        for (GrayToolProjectVO vo : listResult.getData()) {
            if (StringUtils.isNotBlank(vo.getToolName())) {
                toolStatusMap.put(vo.getToolName(), vo);
            }
        }
        return toolStatusMap;
    }

    private Long sortByOfficialProps(CheckerSetVO checkerSetVO) {
        Long sortNum = (long) Integer.MAX_VALUE;
        if (null != checkerSetVO.getDefaultCheckerSet() && checkerSetVO.getDefaultCheckerSet()) {
            sortNum = sortNum + ((long) Integer.MAX_VALUE * -1000000);
        }
        if (CheckerSetSource.DEFAULT.name().equals(checkerSetVO.getCheckerSetSource())) {
            sortNum = sortNum + ((long) Integer.MAX_VALUE * -100000);
        }
        if (CheckerSetSource.RECOMMEND.name().equals(checkerSetVO.getCheckerSetSource())) {
            sortNum = sortNum + ((long) Integer.MAX_VALUE * -10000);
        }
        return sortNum;
    }

    @Override
    public Page<CheckerSetVO> getCheckerSetsOfProjectPage(CheckerSetListQueryReq queryCheckerSetReq) {
        if (null == queryCheckerSetReq.getSortType()) {
            queryCheckerSetReq.setSortType(Sort.Direction.DESC.name());
        }

        if (StringUtils.isEmpty(queryCheckerSetReq.getSortField())) {
            queryCheckerSetReq.setSortField("task_usage");
        }

        // 获取结果
        List<CheckerSetVO> result = getCheckerSetsOfProject(queryCheckerSetReq);

        log.info("finish to get checker set of project: {}", queryCheckerSetReq.getProjectId());

        //封装分页类
        int pageNum = Math.max(queryCheckerSetReq.getPageNum() - 1, 0);
        int pageSize = queryCheckerSetReq.getPageSize() <= 0 ? 10 : queryCheckerSetReq.getPageSize();
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(queryCheckerSetReq.getSortType(),
                queryCheckerSetReq.getSortField()));
        long total = pageNum * pageSize + result.size() + 1;
        return new PageImpl<>(result, pageable, total);
    }

    @Override
    public Map<String, List<CheckerSetVO>> getAvailableCheckerSetsOfProject(String projectId) {
        Map<String, List<CheckerSetVO>> resultCheckerSetMap = new LinkedHashMap<>();
        for (CheckerSetSource checkerSetSource : CheckerSetSource.values()) {
            resultCheckerSetMap.put(checkerSetSource.getNameCn(), new ArrayList<>());
        }

        List<Boolean> legacyList = new ArrayList<>();
        legacyList.add(false);
        legacyList.add(true);
        List<CheckerSetEntity> filteredCheckerSetList =
                findAvailableCheckerSetsByProjectI18NWrapper(projectId, legacyList);

        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntities =
                checkerSetProjectRelationshipRepository.findByProjectId(projectId);
        Map<String, CheckerSetProjectRelationshipEntity> checkerSetRelationshipMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(projectRelationshipEntities)) {
            for (CheckerSetProjectRelationshipEntity projectRelationshipEntity : projectRelationshipEntities) {
                checkerSetRelationshipMap.put(projectRelationshipEntity.getCheckerSetId(), projectRelationshipEntity);
            }
        }
        if (CollectionUtils.isNotEmpty(filteredCheckerSetList)) {
            for (CheckerSetEntity checkerSetEntity : filteredCheckerSetList) {
                CheckerSetProjectRelationshipEntity projectRelationshipEntity =
                        checkerSetRelationshipMap.get(checkerSetEntity.getCheckerSetId());
                if ((projectRelationshipEntity != null && null != projectRelationshipEntity.getDefaultCheckerSet()
                        && projectRelationshipEntity.getDefaultCheckerSet())
                        || (CheckerSetSource.DEFAULT.name().equals(checkerSetEntity.getCheckerSetSource())
                        && null == projectRelationshipEntity)) {
                    checkerSetEntity.setDefaultCheckerSet(true);
                } else {
                    checkerSetEntity.setDefaultCheckerSet(false);
                }
            }
        }

        if (CollectionUtils.isEmpty(filteredCheckerSetList)) {
            return resultCheckerSetMap;
        }

        //官方优选 官方推荐版本
        Map<String, Integer> officialMap = filteredCheckerSetList.stream()
                .filter(checkerSetEntity ->
                        Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name())
                                .contains(checkerSetEntity.getCheckerSetSource()))
                .collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId))
                .entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey,
                        entry -> entry.getValue().stream()
                                .max(Comparator.comparingInt(CheckerSetEntity::getVersion))
                                .orElse(new CheckerSetEntity())
                                .getVersion()));

        //进行过滤，去掉规则为空、单语言的规则集
        filteredCheckerSetList = filteredCheckerSetList.stream().filter(checkerSetEntity ->
                        CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())
                                && (StringUtils.isEmpty(checkerSetEntity.getCheckerSetLang())
                                || !checkerSetEntity.getCheckerSetLang().contains(ComConstants.STRING_SPLIT)))
                .collect(Collectors.toList());
        // 查询语言参数列表
        Result<List<BaseDataVO>> paramsResult =
                client.get(ServiceBaseDataResource.class).getParamsByType(ComConstants.KEY_CODE_LANG);
        if (paramsResult.isNotOk() || CollectionUtils.isEmpty(paramsResult.getData())) {
            log.error("param list is empty! param type: {}", ComConstants.KEY_CODE_LANG);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        List<BaseDataVO> codeLangParams = paramsResult.getData();

        //按使用量排序
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList =
                checkerSetTaskRelationshipRepository.findByProjectId(projectId);
        Map<String, Long> checkerSetCountMap = checkerSetTaskRelationshipEntityList.stream()
                .collect(Collectors.groupingBy(CheckerSetTaskRelationshipEntity::getCheckerSetId,
                        Collectors.counting()));
        filteredCheckerSetList.stream()
                .sorted(Comparator.comparingLong(o -> checkerSetCountMap.containsKey(o.getCheckerSetId())
                        ? -checkerSetCountMap.get(o.getCheckerSetId()) : 0L))
                .forEach(checkerSetEntity -> {
                    if (CheckerSetSource.DEFAULT.name().equals(checkerSetEntity.getCheckerSetSource())
                            || CheckerSetSource.RECOMMEND.name().equals(checkerSetEntity.getCheckerSetSource())) {
                        resultCheckerSetMap.compute(
                                CheckerSetSource.valueOf(checkerSetEntity.getCheckerSetSource()).getNameCn(),
                                (k, v) -> {
                                    if (null == v) {
                                        return new ArrayList<>();
                                    } else {
                                        if (!checkerSetEntity.getVersion().equals(
                                                officialMap.get(checkerSetEntity.getCheckerSetId()))) {
                                            return v;
                                        }
                                        v.add(handleCheckerSetForCateList(checkerSetEntity, codeLangParams));
                                        return v;
                                    }
                                }
                        );
                    } else {
                        resultCheckerSetMap.compute(CheckerSetSource.SELF_DEFINED.getNameCn(), (k, v) -> {
                            if (null == v) {
                                return new ArrayList<>();
                            } else {
                                v.add(handleCheckerSetForCateList(checkerSetEntity, codeLangParams));
                                return v;
                            }
                        });
                    }
                });
        return resultCheckerSetMap;
    }

    private CheckerSetVO handleCheckerSetForCateList(CheckerSetEntity checkerSetEntity,
            List<BaseDataVO> codeLangParams) {
        CheckerSetVO checkerSetVO = new CheckerSetVO();
        BeanUtils.copyProperties(checkerSetEntity, checkerSetVO);
        if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())) {
            checkerSetVO.setToolList(checkerSetEntity.getCheckerProps().stream()
                    .map(CheckerPropsEntity::getToolName).collect(Collectors.toSet()));
            checkerSetVO.setCheckerCount(checkerSetEntity.getCheckerProps().size());
        }
        List<String> codeLangs = Lists.newArrayList();
        for (BaseDataVO codeLangParam : codeLangParams) {
            int paramCodeInt = Integer.valueOf(codeLangParam.getParamCode());
            if (null != checkerSetVO.getCodeLang() && (checkerSetVO.getCodeLang() & paramCodeInt) != 0) {
                // 蓝盾流水线使用的是语言别名的第一个值作为语言的ID来匹配的
                codeLangs.add(new JSONArray(codeLangParam.getParamExtend2()).getString(0));
            }
        }
        checkerSetVO.setCodeLangList(codeLangs);

        return checkerSetVO;
    }

    /**
     * 根据项目配置，从规则集的多个版本中获取对应版本的规则集
     * 匹配优先级为：
     * 1.如果项目关联了 latest 版本的规则集，并且项目配置了对应工具的 测试/灰度 版本，则优先获取规则集的 测试/灰度 版本；
     * 2.如果项目关联了 latest 版本的规则集，但是经过1没有获取到测试/灰度版本的规则集，则获取最新版本规则集；
     * 3.经过2没有匹配到规则集，并且项目指定了规则集版本，则获取指定版本的规则集；
     * 4.项目即没有关联 latest 版本的规则集，也没有指定了规则集版本，默认获取最新版本的规则集
     *
     * @param specifiedVersionMap
     * @param entry
     * @param defaultMap
     * @param checkerSetCountMap
     * @param latestVersionCheckerSets
     * @param toolGrayMap
     * @param categoryMap
     * @return
     */
    private CheckerSetVO getCheckerSetVO(
            Entry<String, List<CheckerSetEntity>> entry,
            Map<String, Integer> specifiedVersionMap,
            Map<String, Boolean> defaultMap,
            Map<String, Long> checkerSetCountMap,
            Set<String> latestVersionCheckerSets,
            Map<String, GrayToolProjectVO> toolGrayMap,
            Map<String, String> categoryMap) {
        String checkerSetId = entry.getKey();
        List<CheckerSetEntity> checkerSetEntities = entry.getValue();

        CheckerSetEntity selectedCheckerSet = selectMatchVersionCheckerSet(checkerSetId, checkerSetEntities,
                latestVersionCheckerSets, specifiedVersionMap, toolGrayMap);
        if (selectedCheckerSet == null) {
            return null;
        }

        CheckerSetVO checkerSetVO = new CheckerSetVO();
        checkerSetVO.setToolList(Sets.newHashSet());
        BeanUtils.copyProperties(selectedCheckerSet, checkerSetVO);

        // 加入工具列表
        if (CollectionUtils.isNotEmpty(selectedCheckerSet.getCheckerProps())) {
            for (CheckerPropsEntity checkerPropsEntity : selectedCheckerSet.getCheckerProps()) {
                checkerSetVO.getToolList().add(checkerPropsEntity.getToolName());
            }
        }

        List<CheckerSetVersionVO> versionList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(checkerSetEntities)) {
            // 加入latest，未发布的规则集，无需latest
            if (checkerSetVO.getVersion() == null || checkerSetVO.getVersion() > 0) {
                CheckerSetVersionVO latestCheckerSetVersionVO = new CheckerSetVersionVO();
                latestCheckerSetVersionVO.setVersion(Integer.MAX_VALUE);
                latestCheckerSetVersionVO.setDisplayName("latest");
                versionList.add(latestCheckerSetVersionVO);
            }

            Locale locale = AbstractI18NResponseAspect.getLocale();

            List<Integer> notProdVersions = Lists.newArrayList(ToolIntegratedStatus.G.value(),
                    ToolIntegratedStatus.T.value());
            // 组装规则集可用的version清单
            for (CheckerSetEntity checkerSetEntity : checkerSetEntities) {
                // 过滤掉：项目工具配置是正式版本，但规则集不是正式版本
                GrayToolProjectVO grayTool = toolGrayMap.get(checkerSetEntity.getToolName());
                int version = grayTool == null ? ToolIntegratedStatus.P.value() : grayTool.getStatus();
                if (version >= ToolIntegratedStatus.P.value()
                        && notProdVersions.contains(checkerSetEntity.getVersion())) {
                    continue;
                }
                // 过滤掉：如果规则集版本不是生产阶段，且工具灰度配置阶段与当前规则集版本不同
                if (notProdVersions.contains(checkerSetEntity.getVersion())
                        && !Objects.equals(version, checkerSetEntity.getVersion())) {
                    continue;
                }
                CheckerSetVersionVO checkerSetVersionVO = getCheckerSetVersionVO(checkerSetEntity, locale);
                versionList.add(checkerSetVersionVO);
            }
        }
        versionList.sort(((o1, o2) -> o2.getVersion().compareTo(o1.getVersion())));
        checkerSetVO.setVersionList(versionList);
        checkerSetVO.setTaskUsage(checkerSetCountMap.get(checkerSetVO.getCheckerSetId()) == null ? 0 :
                checkerSetCountMap.get(checkerSetVO.getCheckerSetId()).intValue());

        // 加入语言显示名称
        checkerSetVO.setCodeLangList(List2StrUtil.fromString(selectedCheckerSet.getCheckerSetLang(), ","));

        // 如果选择了latest，或者没有指定规则集，且规则集是正式规则集，那么选中的规则集是latest，则传入整数最大值对应版本列表中的latest
        if (latestVersionCheckerSets.contains(selectedCheckerSet.getCheckerSetId())
                || null == specifiedVersionMap.get(checkerSetVO.getCheckerSetId())) {
            if (checkerSetVO.getVersion() > 0) {
                checkerSetVO.setVersion(Integer.MAX_VALUE);
            }
        }

        int checkerCount = selectedCheckerSet.getCheckerProps() != null
                ? selectedCheckerSet.getCheckerProps().size() : 0;
        checkerSetVO.setCheckerCount(checkerCount);
        if (null == checkerSetVO.getCreateTime()) {
            checkerSetVO.setCreateTime(0L);
        }
        //加入是否默认
        checkerSetVO.setDefaultCheckerSet((CheckerSetSource.DEFAULT.name().equals(checkerSetVO.getCheckerSetSource())
                && null == defaultMap.get(checkerSetVO.getCheckerSetId()))
                || (null != defaultMap.get(checkerSetVO.getCheckerSetId())
                && defaultMap.get(checkerSetVO.getCheckerSetId())));

        if (CollectionUtils.isNotEmpty(selectedCheckerSet.getCatagories())) {
            checkerSetVO.setCatagories(Lists.newArrayList());
            for (CheckerSetCatagoryEntity category : selectedCheckerSet.getCatagories()) {
                String resourceCode = categoryMap.get(category.getCnName());
                if (!ObjectUtils.isEmpty(resourceCode)) {
                    String message = I18NUtils.getMessage(resourceCode);
                    checkerSetVO.getCatagories().add(new CheckerSetCategoryVO(category.getEnName(), message));
                }
            }
        }

        return checkerSetVO;
    }

    @NotNull
    private CheckerSetVersionVO getCheckerSetVersionVO(CheckerSetEntity checkerSetEntity, Locale locale) {
        CheckerSetVersionVO checkerSetVersionVO = new CheckerSetVersionVO();
        checkerSetVersionVO.setVersion(checkerSetEntity.getVersion());
        if (checkerSetEntity.getVersion() == ToolIntegratedStatus.G.value()) {
            checkerSetVersionVO.setDisplayName(I18NUtils.getMessage("CHECKER_SET_GRAY_VERSION", locale));
        } else if (checkerSetEntity.getVersion() == ToolIntegratedStatus.T.value()) {
            checkerSetVersionVO.setDisplayName(I18NUtils.getMessage("CHECKER_SET_TEST_VERSION", locale));
        } else {
            checkerSetVersionVO.setDisplayName("V" + checkerSetEntity.getVersion());
        }
        return checkerSetVersionVO;
    }

    /**
     * 从一个规则集会的多个版本中选出最合适当前项目/任务的版本
     * 匹配优先级为：
     * * 1.如果项目关联了 latest 版本的规则集，并且项目配置了对应工具的 测试/灰度 版本，则优先获取规则集的 测试/灰度 版本；
     * * 2.如果项目关联了 latest 版本的规则集，但是经过1没有获取到测试/灰度版本的规则集，则获取最新版本规则集；
     * * 3.经过2没有匹配到规则集，并且项目指定了规则集版本，则获取指定版本的规则集；
     * * 4.项目即没有关联 latest 版本的规则集，也没有指定了规则集版本，默认获取最新版本的规则集
     *
     * @param checkerSetId
     * @param checkerSetEntities
     * @param latestVersionCheckerSets
     * @param specifiedVersionMap
     * @param toolGrayMap
     * @return
     */
    @Nullable
    private CheckerSetEntity selectMatchVersionCheckerSet(
            String checkerSetId,
            List<CheckerSetEntity> checkerSetEntities,
            Set<String> latestVersionCheckerSets,
            Map<String, Integer> specifiedVersionMap,
            Map<String, GrayToolProjectVO> toolGrayMap) {

        // 1.如果项目关联了 latest 版本的规则集，并且项目配置了对应工具的 测试/灰度 版本，则优先获取规则集的 测试/灰度 版本；
        CheckerSetEntity selectedCheckerSet = getGrayCheckerSetByGrayToolConfig(checkerSetId, checkerSetEntities,
                latestVersionCheckerSets, specifiedVersionMap, toolGrayMap);

        CheckerSetEntity maxVersionCheckerSet = checkerSetEntities.stream()
                .max(Comparator.comparingInt(CheckerSetEntity::getVersion)).orElse(null);

        // 2.如果项目关联了 latest 版本的规则集，但是经过1没有获取到测试/灰度版本的规则集，则获取最新版本规则集；
        if (selectedCheckerSet == null && latestVersionCheckerSets.contains(checkerSetId)) {
            selectedCheckerSet = maxVersionCheckerSet;
        }

        // 3.经过2没有匹配到规则集，并且项目指定了规则集版本，则获取指定版本的规则集；
        if (selectedCheckerSet == null && null != specifiedVersionMap.get(checkerSetId)) {
            selectedCheckerSet = checkerSetEntities.stream().filter(it ->
                    specifiedVersionMap.get(checkerSetId).equals(it.getVersion())).findFirst().orElse(null);
        }

        /*
         4.项目即没有关联 latest 版本的规则集，也没有指定了规则集版本，默认获取最新版本的规则集（这种情况一般是因为规则集是官方推荐或默认
         规则集，不需要安装就可以使用，所以也是选中最新规则集）
         */
        if (selectedCheckerSet == null) {
            selectedCheckerSet = maxVersionCheckerSet;
        }

        // 匹配不到规则集，则直接返回
        if (selectedCheckerSet == null) {
            return null;
        }

        // 匹配到规则集，但是项目是正式项目，而规则集是非正式规则集，则直接返回null
        List<Integer> notProdVersions = Lists.newArrayList(ToolIntegratedStatus.G.value(),
                ToolIntegratedStatus.T.value());
        GrayToolProjectVO grayToolProjectVO = toolGrayMap.get(selectedCheckerSet.getToolName());
        if ((grayToolProjectVO == null || grayToolProjectVO.getStatus() >= ToolIntegratedStatus.P.value())
                && notProdVersions.contains(selectedCheckerSet.getVersion())) {
            return null;
        }

        return selectedCheckerSet;
    }

    /**
     * 根据灰度配置尝试获取对应状态的规则集，注意：该方法只返回非正式状态的规则集，没有非正式的规则集则返回null
     *
     * @param checkerSetId
     * @param checkerSetList
     * @param latestVersionCheckerSets
     * @param specifiedVersionMap
     * @param toolGrayMap
     * @return
     */
    @Nullable
    private CheckerSetEntity getGrayCheckerSetByGrayToolConfig(
            String checkerSetId,
            List<CheckerSetEntity> checkerSetList,
            Set<String> latestVersionCheckerSets,
            Map<String, Integer> specifiedVersionMap,
            Map<String, GrayToolProjectVO> toolGrayMap) {
        CheckerSetEntity selectedCheckerSet = null;
        if (MapUtils.isNotEmpty(toolGrayMap)) {
            // 过滤出非正式的规则集，并且按版本分组组成Map
            Map<Integer, CheckerSetEntity> versionMap = checkerSetList.stream()
                    .filter(it -> it.getVersion() < ToolIntegratedStatus.P.value())
                    .collect(Collectors.toMap(CheckerSetEntity::getVersion, Function.identity(), (v1, v2) -> v2));

            /*
             如果项目关联 latest 版本的规则集
             或者 没有指定规则集版本（即没有安装规则集，这种一般是官方推荐或默认规则集，不需要安装就可以使用，也是选最新规则集），
             或者 是开源项目（即openSourceProject=true，因为开源项目是使用固定版本的规则集的，但是也要支持使用灰度/测试规则集）
             并且项目配置了对应工具的 测试/灰度 版本，则根据工具的 测试/灰度 找 灰度/测试版本规则集
             */
            for (Entry<String, GrayToolProjectVO> toolGrayEntry : toolGrayMap.entrySet()) {
                String toolName = toolGrayEntry.getKey();
                GrayToolProjectVO grayToolProjectVO = toolGrayEntry.getValue();
                if (latestVersionCheckerSets.contains(checkerSetId)
                        || null == specifiedVersionMap.get(checkerSetId)
                        || grayToolProjectVO.isOpenSourceProject()) {
                    CheckerSetEntity checkerSetEntity = versionMap.get(grayToolProjectVO.getStatus());

                    // 灰度配置与规则集的toolName字段匹配则赋值
                    if (null != checkerSetEntity && toolName.equals(checkerSetEntity.getToolName())) {
                        selectedCheckerSet = checkerSetEntity;
                        break;
                    }
                }
            }
        }
        return selectedCheckerSet;
    }

    /**
     * 查询规则集列表
     * 对于服务创建的任务，有可能存在规则集迁移自动生成的多语言规则，此处查询逻辑如下：
     * 1、展示适合项目语言的新规则集
     * 2、展示适合项目语言的单语言的老规则集
     * 3、如果有多语言的老规则集，且已经被迁移脚本开启了，则也需要进行展示。用户关闭后则不再展示。
     * 4、多语言的老规则集只能关闭，不能再打开，需做下限制
     *
     * @param queryCheckerSetReq
     * @return
     */
    @Override
    public List<CheckerSetVO> getCheckerSetsOfTask(CheckerSetListQueryReq queryCheckerSetReq) {
        log.info("start to get checker set of project, task: {}, {}",
                queryCheckerSetReq.getProjectId(), queryCheckerSetReq.getTaskId());

        // 获取任务详情
        Long taskId = queryCheckerSetReq.getTaskId();
        TaskDetailVO taskDetail =
                client.get(ServiceTaskRestResource.class).getTaskInfoWithoutToolsByTaskId(taskId).getData();
        Long codeLang;
        if (taskDetail == null) {
            log.error("task info empty! task id: {}", taskId);
            codeLang = 0L;
        } else {
            codeLang = taskDetail.getCodeLang();
        }

        String projectId = queryCheckerSetReq.getProjectId();
        // 查出项目维度的id集合
        List<CheckerSetProjectRelationshipEntity> projectRelationships =
                checkerSetProjectRelationshipRepository.findByProjectId(projectId);
        // 查出任务维度的id集合
        Map<String, CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityMap =
                checkerSetTaskRelationshipRepository.findByTaskId(taskId).stream()
                        .collect(Collectors.toMap(
                                CheckerSetTaskRelationshipEntity::getCheckerSetId, Function.identity(), (k, v) -> v));
        // 非自主配置的，用任务维度规则集版本覆盖项目维度的
        if (taskDetail != null && taskDetail.getCheckerSetType() != null
                && taskDetail.getCheckerSetType() != CheckerSetPackageType.NORMAL) {
            for (CheckerSetProjectRelationshipEntity projectRelationshipEntity : projectRelationships) {
                CheckerSetTaskRelationshipEntity taskRelationship =
                        checkerSetTaskRelationshipEntityMap.get(projectRelationshipEntity.getCheckerSetId());
                if (taskRelationship != null && taskRelationship.getVersion() != null
                        && !Objects.equals(taskRelationship.getVersion(), projectRelationshipEntity.getVersion())) {
                    projectRelationshipEntity.setVersion(taskRelationship.getVersion());
                    projectRelationshipEntity.setUselatestVersion(false);
                }
            }
        }

        //查出项目纬度的id集合
        log.info("start to get checker by taskId set of project, task: {}, {}",
                queryCheckerSetReq.getProjectId(), queryCheckerSetReq.getTaskId());
        Set<String> projCheckerSetIds = projectRelationships.stream()
                .map(CheckerSetProjectRelationshipEntity::getCheckerSetId).collect(Collectors.toSet());

        //查出项目纬度下的规则集
        List<CheckerSetEntity> checkerSetEntityList = checkerSetDao.findByComplexCheckerSetCondition(
                queryCheckerSetReq.getKeyWord(),
                projCheckerSetIds,
                queryCheckerSetReq.getCheckerSetLanguage(),
                queryCheckerSetReq.getCheckerSetCategory(),
                queryCheckerSetReq.getToolName(),
                queryCheckerSetReq.getCheckerSetSource(),
                queryCheckerSetReq.getCreator(),
                true,
                true);

        // 先根据规则数是否为空、是否是可以展示的老规则集、规则的语言是否匹配做一遍过滤
        Set<String> taskCheckerSetIds = checkerSetTaskRelationshipEntityMap.keySet();
        checkerSetEntityList = checkerSetEntityList.stream().filter(it -> {
            //规则数为空的不显示
            if (CollectionUtils.isEmpty(it.getCheckerProps())) {
                return false;
            }

            //如果是老规则集，且没有被该任务使用，且是多语言规则集，不显示
            if (it.getLegacy() != null
                    && it.getLegacy()
                    && !taskCheckerSetIds.contains(it.getCheckerSetId())
                    && StringUtils.isNotEmpty(it.getCheckerSetLang())
                    && it.getCheckerSetLang().contains(ComConstants.STRING_SPLIT)) {
                return false;
            }

            // 规则集语言于任务语言不匹配，不显示
            if (it.getCodeLang() != null && (codeLang & it.getCodeLang()) == 0L) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());

        // 再调用项目规则过滤器过滤出符合项目的规则集
        checkerSetEntityList = filterCheckerSets(projectId, checkerSetEntityList, projectRelationships);

        List<CheckerSetVO> taskCheckerSets = Lists.newArrayList();
        List<CheckerSetVO> otherCheckerSets = Lists.newArrayList();
        Map<String, String> categoryMap = Stream.of(CheckerSetCategory.values()).collect(
                Collectors.toMap(CheckerSetCategory::getName, CheckerSetCategory::getI18nResourceCode, (k1, k2) -> k1)
        );
        Map<String, Boolean> defaultCheckerSetMap = projectRelationships.stream()
                .collect(Collector.of(HashMap::new, (k, v) -> k.put(v.getCheckerSetId(), v.getDefaultCheckerSet()),
                        (k, v) -> v, Collector.Characteristics.IDENTITY_FINISH));

        checkerSetEntityList.forEach(it -> {
            CheckerSetVO checkerSetVO = applyTaskCheckerSetVO(it, defaultCheckerSetMap, categoryMap);
            if (taskCheckerSetIds.contains(checkerSetVO.getCheckerSetId())) {
                checkerSetVO.setTaskUsing(true);
                taskCheckerSets.add(checkerSetVO);
            } else {
                checkerSetVO.setTaskUsing(false);
                otherCheckerSets.add(checkerSetVO);
            }
        });

        List<CheckerSetVO> result = Lists.newArrayList();

        // 任务使用的规则在前，未使用的规则在后，然后再按创建时间倒序
        if (CollectionUtils.isNotEmpty(taskCheckerSets)) {
            taskCheckerSets.sort(Comparator.comparingLong(o -> sortByOfficialProps(o) - o.getCreateTime()));
            result.addAll(taskCheckerSets);
        }
        if (CollectionUtils.isNotEmpty(otherCheckerSets)) {
            otherCheckerSets.sort(Comparator.comparingLong(o -> sortByOfficialProps(o) - o.getCreateTime()));
            result.addAll(otherCheckerSets);
        }
        return result;
    }

    /**
     * 将CheckerSetEntity转换成任务规则集页面展示的VO，并填充页面所需的信息
     *
     * @param selectedCheckerSet
     * @param defaultCheckerSetMap
     * @param categoryMap
     */
    private CheckerSetVO applyTaskCheckerSetVO(CheckerSetEntity selectedCheckerSet,
            Map<String, Boolean> defaultCheckerSetMap, Map<String, String> categoryMap) {
        CheckerSetVO checkerSetVO = new CheckerSetVO();
        BeanUtils.copyProperties(selectedCheckerSet, checkerSetVO);
        checkerSetVO.setToolList(Sets.newHashSet());

        // 加工具列表
        for (CheckerPropsEntity checkerPropsEntity : selectedCheckerSet.getCheckerProps()) {
            checkerSetVO.getToolList().add(checkerPropsEntity.getToolName());
        }

        // 加语言显示名称
        checkerSetVO.setCodeLangList(List2StrUtil.fromString(selectedCheckerSet.getCheckerSetLang(), ","));

        //设置默认标签
        checkerSetVO.setDefaultCheckerSet((CheckerSetSource.DEFAULT.name()
                .equals(checkerSetVO.getCheckerSetSource())
                && null == defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId())
                || (null != defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId())
                && defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId()))));

        if (checkerSetVO.getVersion() == ToolIntegratedStatus.T.value()
                || checkerSetVO.getVersion() == ToolIntegratedStatus.G.value()
                || checkerSetVO.getVersion() == ToolIntegratedStatus.PRE_PROD.value()) {
            CheckerSetVersionVO tCheckerSetVersionVO =
                    new CheckerSetVersionVO(ToolIntegratedStatus.T.value(), "测试");
            CheckerSetVersionVO gCheckerSetVersionVO =
                    new CheckerSetVersionVO(ToolIntegratedStatus.G.value(), "灰度");
            CheckerSetVersionVO preProdCheckerSetVersionVO =
                    new CheckerSetVersionVO(ToolIntegratedStatus.PRE_PROD.value(), "预发布");
            List<CheckerSetVersionVO> checkerSetVersionVOList = checkerSetVO.getVersionList() != null
                    ? checkerSetVO.getVersionList() : new ArrayList<>();
            checkerSetVersionVOList.add(tCheckerSetVersionVO);
            checkerSetVersionVOList.add(gCheckerSetVersionVO);
            checkerSetVersionVOList.add(preProdCheckerSetVersionVO);
            checkerSetVO.setVersionList(checkerSetVersionVOList);
        }

        // 类型i18n
        if (CollectionUtils.isNotEmpty(selectedCheckerSet.getCatagories())) {
            checkerSetVO.setCatagories(Lists.newArrayList());
            for (CheckerSetCatagoryEntity category : selectedCheckerSet.getCatagories()) {
                // 历史原因，DB存在的必有中文名，所以用作KEY定位
                String resourceCode = categoryMap.get(category.getCnName());

                if (!ObjectUtils.isEmpty(resourceCode)) {
                    String message = I18NUtils.getMessage(resourceCode);
                    checkerSetVO.getCatagories().add(new CheckerSetCategoryVO(message, message));
                }
            }
        }
        return checkerSetVO;
    }

    @Override
    public List<CheckerSetVO> getTaskCheckerSets(
            String projectId,
            long taskId,
            String toolName,
            String dimension,
            boolean needProps
    ) {
        List<String> toolNameSet = ParamUtils.getTools(toolName, dimension, taskId, "", true);

        return getTaskCheckerSetsCore(projectId, Lists.newArrayList(taskId), toolNameSet, true);
    }

    @Override
    public List<CheckerSetVO> getTaskCheckerSets(String projectId, List<Long> taskIdList, List<String> toolNameList
    ) {
        return getTaskCheckerSetsCore(projectId, taskIdList, toolNameList, false);
    }

    @Override
    public List<CheckerSetVO> getTaskCheckerSets(String projectId, long taskId, List<String> toolNameList) {
        return getTaskCheckerSetsCore(projectId, Lists.newArrayList(taskId), toolNameList, true);
    }

    @Override
    public List<CheckerSetVO> getTaskCheckerSetsCore(
            String projectId,
            List<Long> taskIdList,
            List<String> toolNameList,
            boolean needProps
    ) {
        log.info("getTaskCheckerSetsCore, project id: {}, task id: {}, tool name: {}",
                projectId, taskIdList, toolNameList);

        //查出任务维度的id集合
        List<CheckerSetTaskRelationshipEntity> taskRelationships =
                checkerSetTaskRelationshipRepository.findByTaskIdIn(taskIdList);

        Set<String> taskCheckerSetIds = taskRelationships.stream()
                .map(CheckerSetTaskRelationshipEntity::getCheckerSetId).collect(Collectors.toSet());

        //查出任务纬度下的包含指定工具的规则集
        List<CheckerSetEntity> checkerSetEntityList = checkerSetDao.findByComplexCheckerSetCondition(null,
                taskCheckerSetIds, null, null, new HashSet<>(toolNameList), null,
                null, false, true);

        //查出项目纬度的id集合
        List<CheckerSetProjectRelationshipEntity> projectRelationships =
                checkerSetProjectRelationshipRepository.findByProjectId(projectId);
        if (CollectionUtils.isEmpty(projectRelationships)) {
            return new ArrayList<>();
        }

        // 过滤出符合项目的规则集
        List<CheckerSetEntity> matchCheckerSetList = filterCheckerSets(projectId, checkerSetEntityList,
                projectRelationships);

        return matchCheckerSetList.stream().map(it -> mapToVO(it, needProps))
                .sorted(Comparator.comparing(CheckerSetVO::getCodeLang)).collect(Collectors.toList());
    }

    private CheckerSetVO mapToVO(CheckerSetEntity checkerSetEntity, boolean needProps) {
        CheckerSetVO checkerSetVO = new CheckerSetVO();
        if (needProps) {
            BeanUtils.copyProperties(checkerSetEntity, checkerSetVO);
            List<CheckerPropVO> checkerPropsVOS = checkerSetEntity.getCheckerProps()
                    .stream()
                    .map(checkerPropsEntity -> {
                        CheckerPropVO checkerPropVO = new CheckerPropVO();
                        BeanUtils.copyProperties(checkerPropsEntity, checkerPropVO);
                        return checkerPropVO;
                    }).collect(Collectors.toList());
            checkerSetVO.setCheckerProps(checkerPropsVOS);
        } else {
            BeanUtils.copyProperties(checkerSetEntity, checkerSetVO, "checkerProps");
        }
        checkerSetVO.setTaskUsing(true);
        return checkerSetVO;
    }

    @Override
    public Page<CheckerSetVO> getCheckerSetsOfTaskPage(CheckerSetListQueryReq queryCheckerSetReq) {
        if (null == queryCheckerSetReq.getSortType()) {
            queryCheckerSetReq.setSortType(Sort.Direction.DESC.name());
        }

        if (StringUtils.isEmpty(queryCheckerSetReq.getSortField())) {
            queryCheckerSetReq.setSortField("task_usage");
        }

        // 获取结果
        List<CheckerSetVO> result = getCheckerSetsOfTask(queryCheckerSetReq);

        log.info("finish to get checker set of project, task: {}, {}",
                queryCheckerSetReq.getProjectId(), queryCheckerSetReq.getTaskId());

        //封装分页类
        int pageNum = Math.max(queryCheckerSetReq.getPageNum() - 1, 0);
        int pageSize = queryCheckerSetReq.getPageSize() <= 0 ? 10 : queryCheckerSetReq.getPageSize();
        Pageable pageable = PageRequest.of(pageNum, pageSize,
                Sort.by(queryCheckerSetReq.getSortType(), queryCheckerSetReq.getSortField()));
        long total = pageNum * pageSize + result.size() + 1;
        return new PageImpl<>(result, pageable, total);
    }

    /**
     * 查询规则集参数
     *
     * @param projectId
     * @return
     */
    @Override
    public CheckerSetParamsVO getParams(String projectId) {
        // 查询规则集类型列表
        CheckerSetParamsVO checkerSetParams = new CheckerSetParamsVO();
        checkerSetParams.setCatatories(Lists.newArrayList());
        for (CheckerSetCategory checkerSetCategory : CheckerSetCategory.values()) {
            CheckerSetCategoryVO categoryVO = new CheckerSetCategoryVO();
            String message = I18NUtils.getMessage(checkerSetCategory.getI18nResourceCode());
            categoryVO.setCnName(message);
            categoryVO.setEnName(message);
            checkerSetParams.getCatatories().add(categoryVO);
        }

        // 查询规则集语言列表
        Result<List<BaseDataVO>> langsParamsResult =
                client.get(ServiceBaseDataResource.class).getParamsByType(KEY_LANG);
        if (langsParamsResult.isNotOk() || CollectionUtils.isEmpty(langsParamsResult.getData())) {
            log.error("checker set langs is empty!");
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        checkerSetParams.setCodeLangs(Lists.newArrayList());
        for (BaseDataVO baseDataVO : langsParamsResult.getData()) {
            CheckerSetCodeLangVO checkerSetCodeLangVO = new CheckerSetCodeLangVO();
            checkerSetCodeLangVO.setCodeLang(Integer.valueOf(baseDataVO.getParamCode()));
            checkerSetCodeLangVO.setDisplayName(baseDataVO.getParamName());
            checkerSetParams.getCodeLangs().add(checkerSetCodeLangVO);
        }

        // 查询项目下的规则集列表
        CheckerSetListQueryReq queryCheckerSetReq = new CheckerSetListQueryReq();
        queryCheckerSetReq.setProjectId(projectId);
        queryCheckerSetReq.setSortField(CheckerConstants.CheckerSetSortField.TASK_USAGE.value());
        queryCheckerSetReq.setSortType(Sort.Direction.DESC.name());
        List<CheckerSetVO> checkerSetVOS = getCheckerSetsOfProject(queryCheckerSetReq);
        checkerSetParams.setCheckerSets(checkerSetVOS);

        return checkerSetParams;
    }

    /**
     * 规则集ID
     *
     * @param checkerSetId
     */
    @Override
    public CheckerSetVO getCheckerSetDetail(String checkerSetId, int version) {
        CheckerSetEntity selectedCheckerSetEntity = null;
        if (version == Integer.MAX_VALUE) {
            List<CheckerSetEntity> checkerSetEntities =
                    checkerSetRepository.findByCheckerSetIdIn(Sets.newHashSet(checkerSetId));
            if (CollectionUtils.isNotEmpty(checkerSetEntities)) {
                int latestVersion = CheckerConstants.DEFAULT_VERSION;
                selectedCheckerSetEntity = checkerSetEntities.get(0);
                for (CheckerSetEntity checkerSetEntity : checkerSetEntities) {
                    if (checkerSetEntity.getVersion() > latestVersion) {
                        selectedCheckerSetEntity = checkerSetEntity;
                        latestVersion = selectedCheckerSetEntity.getVersion();
                    }
                }
            }
        } else {
            selectedCheckerSetEntity = checkerSetRepository.findFirstByCheckerSetIdAndVersion(checkerSetId, version);
        }
        CheckerSetVO checkerSetVO = new CheckerSetVO();
        checkerSetVO.setToolList(Sets.newHashSet());

        if (selectedCheckerSetEntity != null) {
            BeanUtils.copyProperties(selectedCheckerSetEntity, checkerSetVO);
            checkerSetVO.setCodeLangList(List2StrUtil.fromString(selectedCheckerSetEntity.getCheckerSetLang(), ","));

            // 加入工具列表
            if (CollectionUtils.isNotEmpty(selectedCheckerSetEntity.getCheckerProps())) {
                for (CheckerPropsEntity checkerPropsEntity : selectedCheckerSetEntity.getCheckerProps()) {
                    checkerSetVO.getToolList().add(checkerPropsEntity.getToolName());
                }
            }

            // 类型
            if (CollectionUtils.isNotEmpty(selectedCheckerSetEntity.getCatagories())) {
                Map<String, String> categoryMap = Stream.of(CheckerSetCategory.values()).collect(
                        Collectors.toMap(
                                CheckerSetCategory::getName,
                                CheckerSetCategory::getI18nResourceCode,
                                (k1, k2) -> k1
                        )
                );
                checkerSetVO.setCatagories(Lists.newArrayList());
                for (CheckerSetCatagoryEntity category : selectedCheckerSetEntity.getCatagories()) {
                    String resourceCode = categoryMap.get(category.getCnName());
                    if (!ObjectUtils.isEmpty(resourceCode)) {
                        String message = I18NUtils.getMessage(resourceCode);
                        checkerSetVO.getCatagories().add(new CheckerSetCategoryVO(message, message));
                    }
                }
            }
        }

        return checkerSetVO;
    }

    @Override
    public List<CheckerSetVO> queryCheckerSets(Set<String> checkerSetList, String projectId) {
        List<CheckerSetEntity> checkerSets = checkerSetDao.findByComplexCheckerSetCondition(null,
                checkerSetList, null, null, null, null, null,
                true, true);

        if (CollectionUtils.isEmpty(checkerSets)) {
            log.error("project {} has not install checker set: {}", projectId, checkerSetList);
            return null;
        }

        // findByComplexCheckerSetCondition()中会把官方推荐或者默认的规则集也查出来，这里需要过滤掉checkerSetId不在传入的列表里面的
        checkerSets = checkerSets.stream()
                .filter(it -> checkerSetList.contains(it.getCheckerSetId())).collect(Collectors.toList());

        List<CheckerSetProjectRelationshipEntity> relationshipList =
                checkerSetProjectRelationshipRepository.findByCheckerSetIdInAndProjectId(checkerSetList, projectId);

        // 先过滤出符合项目的规则集，再转换成VO对象
        return filterCheckerSets(projectId, checkerSets, relationshipList)
                .stream().map(it -> {
                    CheckerSetVO checkerSetVO = new CheckerSetVO();
                    BeanUtils.copyProperties(it, checkerSetVO);
                    if (CollectionUtils.isNotEmpty(it.getCheckerProps())) {
                        checkerSetVO.setToolList(it.getCheckerProps().stream()
                                .map(CheckerPropsEntity::getToolName).collect(Collectors.toSet()));
                    }
                    return checkerSetVO;
                }).collect(Collectors.toList());
    }

    @Override
    public List<CheckerSetVO> getCheckerSetsByTaskId(Long taskId) {
        //查出任务维度的id集合
        List<CheckerSetTaskRelationshipEntity> taskRelationships =
                checkerSetTaskRelationshipRepository.findByTaskId(taskId);
        if (CollectionUtils.isEmpty(taskRelationships)) {
            return Lists.newArrayList();
        }
        Map<String, CheckerSetTaskRelationshipEntity> taskRelationshipMap = taskRelationships.stream()
                .collect(Collectors.toMap(CheckerSetTaskRelationshipEntity::getCheckerSetId,
                        Function.identity(), (k, v) -> v));
        Set<String> checkerSetIds = taskRelationshipMap.keySet();
        String projectId = taskRelationships.get(0).getProjectId();

        List<CheckerSetEntity> checkerSetEntityList = checkerSetRepository.findByCheckerSetIdIn(checkerSetIds);
        if (CollectionUtils.isEmpty(checkerSetEntityList)) {
            return Lists.newArrayList();
        }

        List<CheckerSetProjectRelationshipEntity> projectRelationships =
                checkerSetProjectRelationshipRepository.findByCheckerSetIdInAndProjectId(checkerSetIds, projectId);

        Map<String, CheckerSetProjectRelationshipEntity> projectRelationshipMap = projectRelationships.stream()
                .collect(Collectors.toMap(CheckerSetProjectRelationshipEntity::getCheckerSetId,
                        Function.identity(), (k, v) -> v));

        // 获取任务详情
        TaskDetailVO taskDetail = client.get(ServiceTaskRestResource.class)
                .getTaskInfoWithoutToolsByTaskId(taskId).getData();
        // 非自主配置的，用任务维度规则集版本覆盖项目维度的
        if (taskDetail != null && taskDetail.getCheckerSetType() != null
                && taskDetail.getCheckerSetType() != CheckerSetPackageType.NORMAL) {
            for (CheckerSetProjectRelationshipEntity projectRelationshipEntity : projectRelationships) {
                CheckerSetTaskRelationshipEntity taskRelationship =
                        taskRelationshipMap.get(projectRelationshipEntity.getCheckerSetId());
                if (taskRelationship != null && taskRelationship.getVersion() != null
                        && !Objects.equals(taskRelationship.getVersion(), projectRelationshipEntity.getVersion())) {
                    projectRelationshipEntity.setVersion(taskRelationship.getVersion());
                    projectRelationshipEntity.setUselatestVersion(false);
                }
            }
        }

        // 过滤出符合项目的规则集
        checkerSetEntityList = filterCheckerSets(projectId, checkerSetEntityList, projectRelationships);

        // 计算规则集的使用量
        Map<String, Long> checkerSetCountMap = taskRelationships.stream()
                .collect(Collectors.groupingBy(CheckerSetTaskRelationshipEntity::getCheckerSetId,
                        Collectors.counting()));

        // 按使用量排序
        return checkerSetEntityList.stream()
                .sorted(Comparator.comparingLong(o -> checkerSetCountMap.containsKey(o.getCheckerSetId())
                        ? -checkerSetCountMap.get(o.getCheckerSetId()) : 0L))
                .map(checkerSetEntity -> {
                    CheckerSetVO checkerSetVO = new CheckerSetVO();
                    BeanUtils.copyProperties(checkerSetEntity, checkerSetVO);
                    Integer useCount = checkerSetCountMap.get(checkerSetVO.getCheckerSetId()) == null ? 0 :
                            Integer.parseInt(checkerSetCountMap.get(checkerSetVO.getCheckerSetId()).toString());
                    checkerSetVO.setTaskUsage(useCount);
                    if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())) {
                        Set<String> toolList = new HashSet<>();
                        checkerSetVO.setCheckerProps(checkerSetEntity.getCheckerProps().stream()
                                .map(checkerPropsEntity -> {
                                    toolList.add(checkerPropsEntity.getToolName());
                                    CheckerPropVO checkerPropVO = new CheckerPropVO();
                                    BeanUtils.copyProperties(checkerPropsEntity, checkerPropVO);
                                    return checkerPropVO;
                                }).collect(Collectors.toList()));
                        checkerSetVO.setToolList(toolList);
                    }

                    CheckerSetProjectRelationshipEntity projectRelationshipEntity =
                            projectRelationshipMap.get(checkerSetEntity.getCheckerSetId());
                    if ((projectRelationshipEntity != null && null != projectRelationshipEntity.getDefaultCheckerSet()
                            && projectRelationshipEntity.getDefaultCheckerSet()) || (CheckerSetSource.DEFAULT.name()
                            .equals(checkerSetEntity.getCheckerSetSource()) && null == projectRelationshipEntity)) {
                        checkerSetVO.setDefaultCheckerSet(true);
                    } else {
                        checkerSetVO.setDefaultCheckerSet(false);
                    }
                    return checkerSetVO;
                }).collect(Collectors.toList());
    }

    @Override
    public List<CheckerCommonCountVO> queryCheckerSetCountList(CheckerSetListQueryReq checkerSetListQueryReq) {
        String projectId = checkerSetListQueryReq.getProjectId();
        if (StringUtils.isEmpty(projectId)) {
            log.error("project id is empty!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"project id"}, null);
        }

        //1. 语言数量map
        Map<String, Integer> langMap = new HashMap<>();
        List<String> langOrder =
                Arrays.asList(redisTemplate.opsForValue().get(RedisKeyConstants.KEY_LANG_ORDER).split(","));
        for (String codeLang : langOrder) {
            langMap.put(codeLang, 0);
        }
        //2.规则类别数量map
        CheckerSetCategory[] checkerSetCategoryList = CheckerSetCategory.values();
        Map<String, Integer> checkerSetCateMap = new HashMap<>();
        for (CheckerSetCategory checkerSetCategory : checkerSetCategoryList) {
            checkerSetCateMap.put(checkerSetCategory.name(), 0);
        }
        //3.工具类别数量map
        Map<String, Integer> toolMap = new HashMap<>();
        List<String> toolOrder =
                Arrays.asList(redisTemplate.opsForValue().get(RedisKeyConstants.KEY_TOOL_ORDER).split(","));
        for (String tool : toolOrder) {
            toolMap.put(tool, 0);
        }
        //4.来源数量筛选
        CheckerSetSource[] checkerSetSources = CheckerSetSource.values();
        Map<String, Integer> sourceMap = new HashMap<>();
        for (CheckerSetSource checkerSetSource : checkerSetSources) {
            sourceMap.put(checkerSetSource.name(), 0);
        }
        //5.总数
        List<CheckerSetEntity> totalList = new ArrayList<>();

        List<CheckerSetProjectRelationshipEntity> projectRelationshipList =
                checkerSetProjectRelationshipRepository.findByProjectId(projectId);

        Set<String> checkerSetIds = projectRelationshipList.stream()
                .map(CheckerSetProjectRelationshipEntity::getCheckerSetId).collect(Collectors.toSet());

        List<CheckerSetEntity> checkerSetEntityList =
                checkerSetDao.findByComplexCheckerSetCondition(checkerSetListQueryReq.getKeyWord(), checkerSetIds,
                        null, null, null, null, null, true, true);

        if (CollectionUtils.isNotEmpty(checkerSetEntityList)) {

            // 先过滤出符合项目的规则集
            checkerSetEntityList = filterCheckerSets(projectId, checkerSetEntityList, projectRelationshipList);

            checkerSetEntityList.forEach(checkerSetEntity -> {
                //1. 计算语言数量
                if (judgeQualifiedCheckerSet(null, checkerSetListQueryReq.getCheckerSetCategory(),
                        checkerSetListQueryReq.getToolName(), checkerSetListQueryReq.getCheckerSetSource(),
                        checkerSetEntity) && StringUtils.isNotEmpty(checkerSetEntity.getCheckerSetLang())) {
                    //要分新插件和老插件
                    if (null != checkerSetEntity.getLegacy() && checkerSetEntity.getLegacy()) {
                        if (CollectionUtils.isNotEmpty(
                                Arrays.asList(checkerSetEntity.getCheckerSetLang().split(",")))) {
                            for (String lang : checkerSetEntity.getCheckerSetLang().split(",")) {
                                langMap.compute(lang, (k, v) -> {
                                    if (null == v) {
                                        return 1;
                                    } else {
                                        v++;
                                        return v;
                                    }
                                });
                            }
                        }
                    } else {
                        langMap.compute(checkerSetEntity.getCheckerSetLang(), (k, v) -> {
                            if (null == v) {
                                return 1;
                            } else {
                                v++;
                                return v;
                            }
                        });
                    }
                }
                //2. 规则类别数量计算
                if (judgeQualifiedCheckerSet(checkerSetListQueryReq.getCheckerSetLanguage(), Sets.newHashSet(),
                        checkerSetListQueryReq.getToolName(),
                        checkerSetListQueryReq.getCheckerSetSource(), checkerSetEntity)
                        && CollectionUtils.isNotEmpty(checkerSetEntity.getCatagories())) {
                    checkerSetEntity.getCatagories().forEach(category -> {
                        if (!checkerSetCateMap.containsKey(category.getEnName())) {
                            return;
                        }

                        checkerSetCateMap.compute(category.getEnName(), (k, v) -> {
                            if (null == v) {
                                return 1;
                            } else {
                                v++;
                                return v;
                            }
                        });
                    });
                }
                //3. 工具数量计算
                if (judgeQualifiedCheckerSet(checkerSetListQueryReq.getCheckerSetLanguage(),
                        checkerSetListQueryReq.getCheckerSetCategory(), null,
                        checkerSetListQueryReq.getCheckerSetSource(), checkerSetEntity)
                        && CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())) {
                    checkerSetEntity.getCheckerProps().stream().map(CheckerPropsEntity::getToolName).distinct()
                            .forEach(tool -> {
                                if (StringUtils.isBlank(tool)) {
                                    return;
                                }
                                toolMap.compute(tool, (k, v) -> {
                                    if (null == v) {
                                        return 1;
                                    } else {
                                        v++;
                                        return v;
                                    }
                                });
                            });
                }
                //4. 来源数量计算
                if (judgeQualifiedCheckerSet(checkerSetListQueryReq.getCheckerSetLanguage(),
                        checkerSetListQueryReq.getCheckerSetCategory(),
                        checkerSetListQueryReq.getToolName(), Sets.newHashSet(), checkerSetEntity)) {
                    sourceMap.compute(StringUtils.isBlank(checkerSetEntity.getCheckerSetSource())
                            ? "SELF_DEFINED" : checkerSetEntity.getCheckerSetSource(), (k, v) -> {
                        if (null == v) {
                            return 1;
                        } else {
                            v++;
                            return v;
                        }
                    });
                }

                //5. 总数计算
                if (judgeQualifiedCheckerSet(checkerSetListQueryReq.getCheckerSetLanguage(),
                        checkerSetListQueryReq.getCheckerSetCategory(),
                        checkerSetListQueryReq.getToolName(), checkerSetListQueryReq.getCheckerSetSource(),
                        checkerSetEntity)) {
                    totalList.add(checkerSetEntity);
                }

            });
        }

        //按照语言顺序
        List<CheckerCountListVO> checkerSetLangCountVOList = langMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(entry.getKey(), null, entry.getValue())
        ).sorted(Comparator.comparingInt(o -> langOrder.contains(o.getKey())
                ? langOrder.indexOf(o.getKey()) : Integer.MAX_VALUE)).collect(Collectors.toList());
        //按照类别枚举排序
        List<CheckerSetCategory> categoryOrder = Arrays.asList(CheckerSetCategory.values());
        boolean isEN = "en".equalsIgnoreCase(AbstractI18NResponseAspect.getLocale().toString());
        Map<String, String> enCategoryMap = Stream.of(CheckerSetCategory.values()).collect(
                Collectors.toMap(CheckerSetCategory::name,
                        x -> I18NUtils.getMessage(x.getI18nResourceCode(), I18NUtils.EN), (k1, k2) -> k1)
        );
        List<CheckerCountListVO> checkerSetCateCountVOList = checkerSetCateMap.entrySet().stream().map(entry -> {
                    CheckerSetCategory category = CheckerSetCategory.valueOf(entry.getKey());
                    return new CheckerCountListVO(
                            category.name(),
                            isEN ? enCategoryMap.get(category.name()) : category.getName(),
                            entry.getValue()
                    );
                })
                .sorted(Comparator.comparingInt(o -> categoryOrder.indexOf(CheckerSetCategory.valueOf(o.getKey()))))
                .collect(Collectors.toList());
        //按照工具的排序
        List<CheckerCountListVO> checkerSetToolCountVOList = toolMap.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new CheckerCountListVO(entry.getKey(), null, entry.getValue()))
                .sorted(Comparator.comparingInt(o -> toolOrder.contains(o.getKey())
                        ? toolOrder.indexOf(o.getKey()) : Integer.MAX_VALUE)).collect(Collectors.toList());

        List<CheckerSetSource> sourceOrder = Arrays.asList(CheckerSetSource.values());
        List<CheckerCountListVO> checkerSetSourceCountVOList = sourceMap.entrySet().stream().map(entry -> {
                    CheckerSetSource checkerSetSource = CheckerSetSource.valueOf(entry.getKey());
                    return new CheckerCountListVO(
                            checkerSetSource.name(),
                            isEN ? checkerSetSource.getNameEn() : checkerSetSource.getNameCn(),
                            entry.getValue()
                    );
                })
                .sorted(Comparator.comparingInt(o -> sourceOrder.indexOf(CheckerSetSource.valueOf(o.getKey()))))
                .collect(Collectors.toList());

        List<CheckerCountListVO> checkerSetTotalCountVOList = Collections.singletonList(new CheckerCountListVO("total",
                null, totalList.size()));

        List<CheckerCommonCountVO> checkerCommonCountVOList = new ArrayList<>();
        checkerCommonCountVOList.add(new CheckerCommonCountVO("checkerSetLanguage", checkerSetLangCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("checkerSetCategory", checkerSetCateCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("toolName", checkerSetToolCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("checkerSetSource", checkerSetSourceCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("total", checkerSetTotalCountVOList));
        return checkerCommonCountVOList;
    }

    /**
     * 过滤出符合项目的规则集
     *
     * @param projectId
     * @param checkerSetEntityList
     * @param projectRelationshipList
     * @return
     */
    @NotNull
    private List<CheckerSetEntity> filterCheckerSets(String projectId, List<CheckerSetEntity> checkerSetEntityList,
            List<CheckerSetProjectRelationshipEntity> projectRelationshipList) {
        Map<String, Integer> specifiedVersionMap = Maps.newHashMap();
        Set<String> latestVersionCheckerSets = Sets.newHashSet();
        projectRelationshipList.forEach(it -> {
            if (it.getUselatestVersion() != null && it.getUselatestVersion()) {
                latestVersionCheckerSets.add(it.getCheckerSetId());
            }
            specifiedVersionMap.put(it.getCheckerSetId(), it.getVersion());
        });

        // 获取项目+工具的灰度配置
        Map<String, GrayToolProjectVO> toolGrayMap = getProjectToolGrayConfig(projectId);

        return checkerSetEntityList.stream()
                // 先对查询到的任务使用的规则集列表按规则集ID分组，然后选出每组中版本号匹配的规则集
                .collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId))
                .entrySet().stream().map(entry ->
                        // 具体过滤条件可以参考下面的selectMatchVersionCheckerSet()方法
                        selectMatchVersionCheckerSet(entry.getKey(), entry.getValue(),
                                latestVersionCheckerSets, specifiedVersionMap, toolGrayMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @I18NResponse
    @Override
    public List<CheckerSetEntity> findAvailableCheckerSetsByProjectI18NWrapper(
            String projectId,
            List<Boolean> legacyList
    ) {
        return findAvailableCheckerSetsByProject(projectId, legacyList);
    }

    @Override
    public List<CheckerSetEntity> findAvailableCheckerSetsByProject(String projectId, List<Boolean> legacy) {
        List<CheckerSetProjectRelationshipEntity> projectRelationshipList =
                checkerSetProjectRelationshipRepository.findByProjectId(projectId);
        Set<String> checkerSetIds = projectRelationshipList.stream()
                .map(CheckerSetProjectRelationshipEntity::getCheckerSetId).collect(Collectors.toSet());

        List<CheckerSetEntity> checkerSetEntityList = checkerSetDao.findByComplexCheckerSetCondition(null,
                checkerSetIds, null, null, null, null, null, true, true);
        checkerSetEntityList = checkerSetEntityList.stream()
                .filter(it -> {
                    if (CollectionUtils.isEmpty(it.getCheckerProps())) {
                        return false;
                    }
                    /*
                     根据前端传入参数legacy过滤规则集：
                     传入为true表示只查旧版规则集，传入为false或者为空，表示不查旧版规则集，传入true+false，
                     表示新规则集和遗留规则集都查询），伪代码逻辑如下：
                     legacy == false时checkerSetEntity.getLegacy() == null 或者 checkerSetEntity.getLegacy() == legacy
                     */
                    return (CollectionUtils.isNotEmpty(legacy) && legacy.contains(false) && it.getLegacy() == null)
                            || legacy.contains(it.getLegacy());
                }).collect(Collectors.toList());

        // 过滤出符合项目的规则集
        return filterCheckerSets(projectId, checkerSetEntityList, projectRelationshipList);
    }

    @Override
    public TaskBaseVO getCheckerAndCheckerSetCount(Long taskId, String projectId) {
        TaskBaseVO taskBaseVO = new TaskBaseVO();
        taskBaseVO.setTaskId(taskId);
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList =
                checkerSetTaskRelationshipRepository.findByTaskId(taskId);
        if (CollectionUtils.isNotEmpty(checkerSetTaskRelationshipEntityList)) {
            Result<TaskDetailVO> taskDetailVOResult =
                    client.get(ServiceTaskRestResource.class).getTaskInfoWithoutToolsByTaskId(taskId);
            Long codeLang;
            if (taskDetailVOResult.isNotOk() || taskDetailVOResult.getData() == null) {
                log.error("task info empty! task id: {}", taskId);
                codeLang = 0L;
            } else {
                codeLang = taskDetailVOResult.getData().getCodeLang();
            }
            Set<String> taskCheckerSetList = checkerSetTaskRelationshipEntityList.stream()
                    .map(CheckerSetTaskRelationshipEntity::getCheckerSetId).collect(Collectors.toSet());
            List<CheckerSetEntity> checkerSetEntityList = findAvailableCheckerSetsByProject(projectId,
                    Arrays.asList(true, false));
            if (CollectionUtils.isNotEmpty(checkerSetEntityList)) {
                List<CheckerSetEntity> taskCheckerSetEntityList = checkerSetEntityList.stream()
                        .filter(checkerSetEntity -> taskCheckerSetList.contains(checkerSetEntity.getCheckerSetId())
                                && (codeLang & checkerSetEntity.getCodeLang()) > 0L)
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(taskCheckerSetEntityList)) {
                    taskBaseVO.setCheckerSetName(taskCheckerSetEntityList.stream()
                            .map(CheckerSetEntity::getCheckerSetName)
                            .distinct().reduce((o1, o2) -> String.format("%s,%s", o1, o2)).get());
                    taskBaseVO.setCheckerCount(taskCheckerSetEntityList.stream()
                            .filter(checkerSetEntity -> CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps()))
                            .map(CheckerSetEntity::getCheckerProps)
                            .flatMap(Collection::stream).map(CheckerPropsEntity::getCheckerKey).distinct().count());
                    return taskBaseVO;
                }
            }
        }
        taskBaseVO.setCheckerCount(0L);
        taskBaseVO.setCheckerSetName("");
        return taskBaseVO;
    }

    private Boolean judgeQualifiedCheckerSet(Set<String> checkerSetLanguage,
            Set<CheckerSetCategory> checkerSetCategorySet,
            Set<String> toolName, Set<CheckerSetSource> checkerSetSource,
            CheckerSetEntity checkerSetEntity) {
        //语言筛选要分新版本插件和老版本插件
        if (CollectionUtils.isNotEmpty(checkerSetLanguage)) {
            if (null != checkerSetEntity.getLegacy() && checkerSetEntity.getLegacy()) {
                if (checkerSetLanguage.stream().noneMatch(language ->
                        checkerSetEntity.getCheckerSetLang().contains(language))) {
                    return false;
                }
            } else {
                if (!checkerSetLanguage.contains(checkerSetEntity.getCheckerSetLang())) {
                    return false;
                }
            }
        }
        if (CollectionUtils.isNotEmpty(checkerSetCategorySet)
                && checkerSetCategorySet.stream().noneMatch(checkerSetCategory ->
                checkerSetEntity.getCatagories().stream()
                        .anyMatch(category -> checkerSetCategory.name().equalsIgnoreCase(category.getEnName())))) {
            return false;

        }
        if (CollectionUtils.isNotEmpty(toolName) && (CollectionUtils.isEmpty(checkerSetEntity.getCheckerProps())
                || toolName.stream().noneMatch(tool -> checkerSetEntity.getCheckerProps().stream().anyMatch(
                checkerPropsEntity -> tool.equalsIgnoreCase(checkerPropsEntity.getToolName()))))) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(checkerSetSource) && !checkerSetSource.contains(CheckerSetSource.valueOf(
                StringUtils.isBlank(checkerSetEntity.getCheckerSetSource()) ? "SELF_DEFINED" :
                        checkerSetEntity.getCheckerSetSource()))) {
            return false;
        }
        return true;
    }

    @Override
    public List<CheckerSetVO> queryCheckerSetsForOpenScan(Set<CheckerSetVO> checkerSetList) {
        if (CollectionUtils.isEmpty(checkerSetList)) {
            return new ArrayList<>();
        }
        Map<String, Integer> checkerSetVersionMap = checkerSetList.stream()
                .filter(checkerSetVO -> null != checkerSetVO.getVersion())
                .collect(Collectors.toMap(CheckerSetVO::getCheckerSetId, CheckerSetVO::getVersion, (k, v) -> v));
        Set<String> checkerSetIdList =
                checkerSetList.stream().map(CheckerSetVO::getCheckerSetId).collect(Collectors.toSet());
        List<CheckerSetEntity> checkerSets = checkerSetDao.findByComplexCheckerSetCondition(null,
                checkerSetIdList, null, null, null, null, null, false, true);
        //用于装最新版本的map
        Map<String, Integer> latestVersionMap = new HashMap<>();
        //用于装当前版本的map
        Map<String, Integer> currentVersionMap = new HashMap<>();
        for (CheckerSetEntity checkerSetEntity : checkerSets) {
            /* 1. 如果入参list中有固定版本号
             *    固定版本号为正常数字：表示固定用该版本号，用currentVersionMap
             *    固定版本号为整数最大值：表示用最新的版本，用latestVersionMap
             * 2. 如果入参list中无版本数字
             *    表示用最新版本，用latestVersionMap
             */
            if (checkerSetVersionMap.containsKey(checkerSetEntity.getCheckerSetId())
                    && Integer.MAX_VALUE != checkerSetVersionMap.get(checkerSetEntity.getCheckerSetId())) {
                currentVersionMap.put(checkerSetEntity.getCheckerSetId(),
                        checkerSetVersionMap.get(checkerSetEntity.getCheckerSetId()));
            } else if (latestVersionMap.get(checkerSetEntity.getCheckerSetId()) == null
                    || checkerSetEntity.getVersion() > latestVersionMap.get(checkerSetEntity.getCheckerSetId())) {
                latestVersionMap.put(checkerSetEntity.getCheckerSetId(), checkerSetEntity.getVersion());
            }
        }
        if (CollectionUtils.isNotEmpty(checkerSets)) {
            return checkerSets.stream()
                    .filter(checkerSetEntity ->
                            //最新版本号
                            (latestVersionMap.containsKey(checkerSetEntity.getCheckerSetId())
                                    && null != latestVersionMap.get(checkerSetEntity.getCheckerSetId())
                                    && (latestVersionMap.get(checkerSetEntity.getCheckerSetId())
                                    .equals(checkerSetEntity.getVersion())))
                                    //或者当前版本号
                                    || (currentVersionMap.containsKey(checkerSetEntity.getCheckerSetId())
                                    && null != currentVersionMap.get(checkerSetEntity.getCheckerSetId())
                                    && (currentVersionMap.get(checkerSetEntity.getCheckerSetId())
                                    .equals(checkerSetEntity.getVersion()))))
                    .map(checkerSetEntity -> {
                        CheckerSetVO checkerSetVO = new CheckerSetVO();
                        BeanUtils.copyProperties(checkerSetEntity, checkerSetVO);
                        if (latestVersionMap.containsKey(checkerSetEntity.getCheckerSetId())) {
                            checkerSetVO.setVersion(Integer.MAX_VALUE);
                        }
                        if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())) {
                            checkerSetVO.setToolList(checkerSetEntity.getCheckerProps().stream()
                                    .map(CheckerPropsEntity::getToolName).collect(Collectors.toSet()));
                        }
                        return checkerSetVO;
                    }).collect(Collectors.toList());
        }

        return null;
    }

    /**
     * 获取规则集管理初始化参数选项
     *
     * @return
     */
    @Override
    public CheckerSetParamsVO getCheckerSetParams() {
        // 查询规则集类型列表
        CheckerSetParamsVO checkerSetParams = new CheckerSetParamsVO();
        // 类别
        checkerSetParams.setCatatories(Lists.newArrayList());
        for (CheckerSetCategory checkerSetCategory : CheckerSetCategory.values()) {
            CheckerSetCategoryVO categoryVO = new CheckerSetCategoryVO();
            categoryVO.setCnName(checkerSetCategory.getName());
            categoryVO.setEnName(checkerSetCategory.name());
            checkerSetParams.getCatatories().add(categoryVO);
        }

        // 来源
        checkerSetParams.setCheckerSetSource(Lists.newArrayList());
        for (CheckerSetSource checkerSetSource : CheckerSetSource.values()) {
            CheckerSetCategoryVO categoryVO = new CheckerSetCategoryVO();
            categoryVO.setCnName(checkerSetSource.getNameCn());
            categoryVO.setEnName(checkerSetSource.name());
            checkerSetParams.getCheckerSetSource().add(categoryVO);
        }

        //适用语言
        Result<List<BaseDataVO>> langsParamsResult =
                client.get(ServiceBaseDataResource.class).getParamsByType(KEY_LANG);
        if (langsParamsResult.isNotOk() || CollectionUtils.isEmpty(langsParamsResult.getData())) {
            log.error("checker set langs is empty!");
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        checkerSetParams.setCodeLangs(Lists.newArrayList());
        for (BaseDataVO baseDataVO : langsParamsResult.getData()) {
            CheckerSetCodeLangVO checkerSetCodeLangVO = new CheckerSetCodeLangVO();
            checkerSetCodeLangVO.setDisplayName(baseDataVO.getParamName());
            checkerSetParams.getCodeLangs().add(checkerSetCodeLangVO);
        }

        return checkerSetParams;
    }

    @Override
    public List<CheckerSetVO> queryCheckerDetailForPreCI() {
        Result<List<BaseDataVO>> result = client.get(ServiceBaseDataResource.class)
                .getParamsByType(ComConstants.PRECI_CHECKER_SET);

        if (result.isNotOk() || CollectionUtils.isEmpty(result.getData())) {
            log.error("all preci checker set base data is null");
            throw new CodeCCException(DefectMessageCode.BASE_DATA_NOT_FOUND);
        }

        List<BaseDataVO> baseDataList = result.getData();
        if (CollectionUtils.isEmpty(baseDataList)) {
            log.error("queryCheckerDetailForPreCI get baseData list is empty");
            return new ArrayList<>();
        }
        return queryCheckerFromBaseData(baseDataList);
    }

    @Override
    public List<CheckerSetVO> queryCheckerDetailForContent(List<String> checkerSetIdList) {
        Result<List<BaseDataVO>> result = client.get(ServiceBaseDataResource.class)
                .getInfoByTypeAndCodeList(ComConstants.PRECI_CHECKER_SET, checkerSetIdList);

        if (result.isNotOk() || CollectionUtils.isEmpty(result.getData())) {
            String ms = String.format("select checker set %s data is null", checkerSetIdList);
            log.error(ms);
            throw new CodeCCException(ms);
        }

        List<BaseDataVO> baseDataList = result.getData();
        if (CollectionUtils.isEmpty(baseDataList)) {
            log.error("queryCheckerDetailForContent get baseData list is empty");
            return new ArrayList<>();
        }
        return queryCheckerFromBaseData(baseDataList);
    }

    /**
     * 根据checkerSetId获取规则集名称
     *
     * @param checkerSetId
     * @return string
     */
    @Override
    public String queryCheckerSetNameByCheckerSetId(String checkerSetId) {
        log.info("queryCheckerSetNameByCheckerSetId, checkerSetId: [{}]", checkerSetId);
        CheckerSetEntity checkerSetEntity = checkerSetDao.queryCheckerSetNameByCheckerSetId(checkerSetId);
        if (null != checkerSetEntity) {
            return checkerSetEntity.getCheckerSetName();
        } else {
            return null;
        }
    }

    private List<CheckerSetVO> queryCheckerFromBaseData(List<BaseDataVO> baseDataList) {

        // param_value -> 版本, param_code -> 规则id
        // param_value为latest的, 获取当前规则集的最新版本
        String latestTag = "latest";
        List<CheckerSetVO> checkerSetVOList = new ArrayList<>();
        Set<String> latestCheckerIdSet = baseDataList.stream().filter(x -> latestTag.equals(x.getParamValue()))
                .map(BaseDataVO::getParamCode).collect(Collectors.toSet());
        Map<String, Integer> checkerIdMaxVersionMap = checkerSetDao.queryCheckerMaxVersion(latestCheckerIdSet);

        if (checkerIdMaxVersionMap == null) {
            checkerIdMaxVersionMap = Maps.newHashMap();
        }

        for (BaseDataVO baseDataVO : baseDataList) {
            String checkerId = baseDataVO.getParamCode();
            String versionStr = baseDataVO.getParamValue();

            if (!latestTag.equals(versionStr)) {
                checkerIdMaxVersionMap.put(checkerId, Integer.valueOf(versionStr));
            }
        }

        // 处理默认规则
        Map<String, BaseDataVO> defaultCheckerMap = baseDataList.stream().filter(x -> "1".equals(x.getParamExtend1()))
                .collect(Collectors.toMap(BaseDataVO::getParamCode, Function.identity(), (k1, k2) -> k2));

        List<CheckerSetEntity> checkerList = checkerSetDao.queryCheckerDetailForPreCI(checkerIdMaxVersionMap);

        checkerList.forEach(it -> {
            CheckerSetVO checkerSetVo = new CheckerSetVO();
            BeanUtils.copyProperties(it, checkerSetVo);

            if (CollectionUtils.isNotEmpty(it.getCheckerProps())) {
                checkerSetVo.setToolList(it.getCheckerProps().stream()
                        .map(CheckerPropsEntity::getToolName).collect(Collectors.toSet()));
            }

            // 处理默认规则集
            BaseDataVO defaultChecker = defaultCheckerMap.get(it.getCheckerSetId());
            checkerSetVo.setPreCIDefault(defaultChecker != null);

            checkerSetVOList.add(checkerSetVo);
        });

        return checkerSetVOList;
    }
}
