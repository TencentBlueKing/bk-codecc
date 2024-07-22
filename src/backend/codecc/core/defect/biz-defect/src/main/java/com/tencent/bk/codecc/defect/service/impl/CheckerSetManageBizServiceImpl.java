package com.tencent.bk.codecc.defect.service.impl;

import static com.tencent.devops.common.constant.ComConstants.ONCE_CHECKER_SET_KEY;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_TASK_CHECKER_CONFIG;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_IGNORE_CHECKER;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerSetProjectRelationshipRepository;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerSetTaskRelationshipRepository;
import com.tencent.bk.codecc.defect.dao.core.mongotemplate.CheckerDetailDao;
import com.tencent.bk.codecc.defect.dao.core.mongotemplate.CheckerSetDao;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerPropsEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetCatagoryEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetProjectRelationshipEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetTaskRelationshipEntity;
import com.tencent.bk.codecc.defect.pojo.CheckerSetCategoryModel;
import com.tencent.bk.codecc.defect.service.ICheckerSetManageBizService;
import com.tencent.bk.codecc.defect.service.ICheckerSetQueryBizService;
import com.tencent.bk.codecc.defect.service.ToolBuildInfoService;
import com.tencent.bk.codecc.defect.vo.CheckerListQueryReq;
import com.tencent.bk.codecc.defect.vo.ConfigCheckersPkgReqVO;
import com.tencent.bk.codecc.defect.vo.UpdateAllCheckerReq;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetSource;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.bk.codecc.task.api.ServiceGrayToolProjectResource;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.api.ServiceToolRestResource;
import com.tencent.bk.codecc.task.vo.BatchRegisterVO;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.checkerset.CheckerPropVO;
import com.tencent.devops.common.api.checkerset.CheckerSetManagementReqVO;
import com.tencent.devops.common.api.checkerset.CheckerSetRelationshipVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.checkerset.CreateCheckerSetReqVO;
import com.tencent.devops.common.api.checkerset.V3UpdateCheckerSetReqExtVO;
import com.tencent.devops.common.api.checkerset.V3UpdateCheckerSetReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.constant.CheckerConstants;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.service.aop.AbstractI18NResponseAspect;
import com.tencent.devops.common.service.utils.I18NUtils;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.List2StrUtil;
import com.tencent.devops.common.util.ThreadPoolUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.CloneUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

/**
 * V3规则集服务实现类
 *
 * @version V1.0
 * @date 2020/1/2
 */
@Slf4j
@Service
public class CheckerSetManageBizServiceImpl implements ICheckerSetManageBizService {

    @Autowired
    private CheckerSetRepository checkerSetRepository;
    @Autowired
    private CheckerSetDao checkerSetDao;
    @Autowired
    private Client client;
    @Autowired
    private ToolBuildInfoService toolBuildInfoService;
    @Autowired
    private CheckerSetProjectRelationshipRepository checkerSetProjectRelationshipRepository;
    @Autowired
    private CheckerSetTaskRelationshipRepository checkerSetTaskRelationshipRepository;
    @Autowired
    private CheckerDetailDao checkerDetailDao;
    @Autowired
    private AuthExPermissionApi authExPermissionApi;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ICheckerSetQueryBizService checkerSetQueryBizService;
    @Autowired
    private ToolMetaCacheService toolMetaCache;


    /**
     * 创建规则集
     *
     * @param user
     * @param projectId
     * @param createCheckerSetReqVO
     * @return
     */
    @Override
    public void createCheckerSet(String user, String projectId, CreateCheckerSetReqVO createCheckerSetReqVO) {
        if (StringUtils.isEmpty(createCheckerSetReqVO.getCheckerSetId())
                || StringUtils.isEmpty(createCheckerSetReqVO.getCheckerSetName())) {
            String errMsgStr = "规则集ID、规则集名称";
            log.error("{}不能为空", errMsgStr);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{errMsgStr}, null);
        }

        // 校验规则集ID是否已存在
        checkIdDuplicate(createCheckerSetReqVO.getCheckerSetId());

        // 校验规则集名称在项目中是否已存在
        checkNameExistInProject(createCheckerSetReqVO.getCheckerSetName(), projectId);

        // 获取规则集基础信息
        long currentTime = System.currentTimeMillis();
        CheckerSetEntity checkerSetEntity = new CheckerSetEntity();
        BeanUtils.copyProperties(createCheckerSetReqVO, checkerSetEntity);
        checkerSetEntity.setCreateTime(currentTime);
        checkerSetEntity.setCreator(user);
        checkerSetEntity.setLastUpdateTime(currentTime);
        checkerSetEntity.setOfficial(CheckerConstants.CheckerSetOfficial.NOT_OFFICIAL.code());
        if (checkerSetEntity.getVersion() == null) {
            checkerSetEntity.setVersion(CheckerConstants.DEFAULT_VERSION);
        }
        checkerSetEntity.setEnable(CheckerConstants.CheckerSetEnable.ENABLE.code());
        checkerSetEntity.setProjectId(projectId);
        checkerSetEntity.setScope(CheckerConstants.CheckerSetScope.PRIVATE.code());

        checkerSetEntity.setDefaultCheckerSet(false);
        checkerSetEntity.setOfficial(CheckerConstants.CheckerSetOfficial.NOT_OFFICIAL.code());

        // 查询语言参数列表
        Result<List<BaseDataVO>> paramsResult =
                client.get(ServiceBaseDataResource.class).getParamsByType(ComConstants.KEY_CODE_LANG);
        if (paramsResult.isNotOk() || CollectionUtils.isEmpty(paramsResult.getData())) {
            log.error("param list is empty! param type: {}", ComConstants.KEY_CODE_LANG);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        List<BaseDataVO> codeLangParams = paramsResult.getData();
        checkerSetEntity.setCheckerSetLang(List2StrUtil.toString(getCodelangs(createCheckerSetReqVO.getCodeLang(),
                codeLangParams), ","));

        // 加入规则集类型中英文名称
        List<CheckerSetCatagoryEntity> catagoryEntities = getCatagoryEntities(createCheckerSetReqVO.getCatagories());
        checkerSetEntity.setCatagories(catagoryEntities);

        // 如果选择了基于某个规则集或者复制与某个规则集，则需要更新规则集中的规则
        if (StringUtils.isNotEmpty(createCheckerSetReqVO.getBaseCheckerSetId())) {
            CheckerSetEntity baseCheckerSet;
            if (createCheckerSetReqVO.getBaseCheckerSetVersion() == null
                    || createCheckerSetReqVO.getBaseCheckerSetVersion() == Integer.MAX_VALUE) {
                List<CheckerSetEntity> baseCheckerSets =
                        checkerSetRepository.findByCheckerSetId(createCheckerSetReqVO.getBaseCheckerSetId());
                baseCheckerSets.sort(((o1, o2) -> o2.getVersion().compareTo(o1.getVersion())));
                baseCheckerSet = baseCheckerSets.get(0);
            } else {
                baseCheckerSet = checkerSetRepository.findFirstByCheckerSetIdAndVersion(
                        createCheckerSetReqVO.getBaseCheckerSetId(),
                        createCheckerSetReqVO.getBaseCheckerSetVersion());
            }
            if (baseCheckerSet == null) {
                String errMsg = "找不到规则集，ID：" + createCheckerSetReqVO.getBaseCheckerSetId();
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
            // 如果基于非生产阶段的规则集来创建，应该根据规则版本来判断新规则集的版本，同时赋值toolName字段
            if (baseCheckerSet.getVersion() < ToolIntegratedStatus.P.value()) {
                if (CollectionUtils.isNotEmpty(baseCheckerSet.getCheckerProps())) {
                    List<CheckerPropVO> checkerProps = baseCheckerSet.getCheckerProps().stream().map(it -> {
                        CheckerPropVO checkerPropVO = new CheckerPropVO();
                        BeanUtils.copyProperties(it, checkerPropVO);
                        return checkerPropVO;
                    }).collect(Collectors.toList());
                    List<CheckerDetailEntity> checkerDetailEntities =
                            checkerDetailDao.findByToolNameAndCheckerKey(checkerProps);
                    Pair<Integer, String> versionAndTool = getCheckerSetVersion(checkerDetailEntities);
                    if (versionAndTool != null) {
                        checkerSetEntity.setVersion(versionAndTool.getFirst());
                        checkerSetEntity.setToolName(baseCheckerSet.getToolName());
                    }
                }
            }
            checkerSetEntity.setCheckerProps(baseCheckerSet.getCheckerProps());
            checkerSetEntity.setInitCheckers(true);
        } else {
            checkerSetEntity.setInitCheckers(false);
        }

        // 入库
        checkerSetRepository.save(checkerSetEntity);

        // 保存规则集与项目的关系
        CheckerSetProjectRelationshipEntity relationshipEntity = new CheckerSetProjectRelationshipEntity();
        relationshipEntity.setCheckerSetId(checkerSetEntity.getCheckerSetId());
        relationshipEntity.setVersion(checkerSetEntity.getVersion());
        relationshipEntity.setProjectId(projectId);
        relationshipEntity.setCreatedBy(user);
        relationshipEntity.setCreatedDate(System.currentTimeMillis());
        relationshipEntity.setUselatestVersion(true);
        relationshipEntity.setDefaultCheckerSet(false);
        checkerSetProjectRelationshipRepository.save(relationshipEntity);
    }

    @Override
    public Boolean updateCheckersOfSetForAll(String user, UpdateAllCheckerReq updateAllCheckerReq) {
        CheckerListQueryReq checkerListQueryReq = updateAllCheckerReq.getCheckerListQueryReq();
        List<CheckerPropVO> checkerPropVOS;
        if (CollectionUtils.isEmpty(updateAllCheckerReq.getCheckerProps())) {
            checkerPropVOS = new ArrayList<>();
        } else {
            checkerPropVOS = updateAllCheckerReq.getCheckerProps();
        }
        List<CheckerDetailEntity> checkerDetailEntityList =
                checkerDetailDao.findByComplexCheckerCondition(checkerListQueryReq.getKeyWord(),
                        checkerListQueryReq.getCheckerLanguage(),
                        checkerListQueryReq.getCheckerCategory(),
                        checkerListQueryReq.getToolName(),
                        checkerListQueryReq.getTag(),
                        checkerListQueryReq.getSeverity(),
                        checkerListQueryReq.getEditable(),
                        checkerListQueryReq.getCheckerRecommend(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        if (CollectionUtils.isNotEmpty(checkerDetailEntityList)) {
            checkerDetailEntityList.forEach(checkerDetailEntity -> {
                if (checkerPropVOS.stream().noneMatch(checkerPropVO ->
                        checkerPropVO.getCheckerKey().equals(checkerDetailEntity.getCheckerKey())
                                && checkerPropVO.getToolName().equals(checkerDetailEntity.getToolName()))) {
                    CheckerPropVO checkerPropVO = new CheckerPropVO();
                    checkerPropVO.setToolName(checkerDetailEntity.getToolName());
                    checkerPropVO.setCheckerKey(checkerDetailEntity.getCheckerKey());
                    checkerPropVO.setCheckerName(checkerDetailEntity.getCheckerName());
                    checkerPropVOS.add(checkerPropVO);
                }
            });
        }
        updateCheckersOfSet(checkerListQueryReq.getCheckerSetId(), user, checkerPropVOS, null);
        return true;

    }

    /**
     * 更新规则集中的规则
     *
     * @param checkerSetId
     * @param checkerProps
     * @param versionAndTool Pair: version, tool
     */
    @Override
    public void updateCheckersOfSet(String checkerSetId, String user,
            List<CheckerPropVO> checkerProps, Pair<Integer, String> versionAndTool) {
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
        if (CollectionUtils.isNotEmpty(checkerSetEntities)) {
            List<CheckerPropsEntity> checkerPropsEntities = Lists.newArrayList();

            List<CheckerDetailEntity> checkerDetailList = null;
            if (CollectionUtils.isNotEmpty(checkerProps)) {
                checkerDetailList = checkerDetailDao.findByToolNameAndCheckerKey(checkerProps);
                Map<String, CheckerDetailEntity> checkerDetailMap = checkerDetailList.stream()
                        .collect(Collectors.toMap(x -> x.getCheckerKey() + x.getToolName(), y -> y, (k1, k2) -> k2));

                for (CheckerPropVO checkerPropVO : checkerProps) {
                    CheckerPropsEntity checkerPropsEntity = new CheckerPropsEntity();
                    BeanUtils.copyProperties(checkerPropVO, checkerPropsEntity);

                    // 若前端传入的props不为空，则进行简单校验
                    if (StringUtils.isNotEmpty(checkerPropVO.getProps())) {
                        String key = checkerPropVO.getCheckerKey() + checkerPropVO.getToolName();
                        CheckerDetailEntity checkerDetailEntity = checkerDetailMap.get(key);

                        if (checkerDetailEntity != null) {
                            // 前端传入props有值，但规则实际上无值，以DB为准
                            if (StringUtils.isEmpty(checkerDetailEntity.getProps())) {
                                checkerPropsEntity.setProps(checkerDetailEntity.getProps());
                            }

                            // 不可编辑参数的规则，以数据库的props为准
                            if (Boolean.FALSE.equals(checkerDetailEntity.getEditable())) {
                                checkerPropsEntity.setProps(checkerDetailEntity.getProps());
                            }
                        }
                    }

                    checkerPropsEntities.add(checkerPropsEntity);
                }
            }

            // 获取规则集的版本，T-测试，G-灰度，P-发布
            Integer checkerSetVersion = null;
            String checkerSetVersionForTool = null;
            if (versionAndTool != null) {
                checkerSetVersion = versionAndTool.getFirst();
                checkerSetVersionForTool = versionAndTool.getSecond();

            } else if (checkerDetailList != null) {
                Pair<Integer, String> checkerVersionAndTool = getCheckerSetVersion(checkerDetailList);
                if (checkerVersionAndTool != null) {
                    checkerSetVersion = checkerVersionAndTool.getFirst();
                    checkerSetVersionForTool = checkerVersionAndTool.getSecond();
                }
            }

            CheckerSetEntity checkerSetEntity = checkerSetEntities.get(0);

            // 还未初始化过的规则集需要设置初始化为true，并且不用生成新的规则集版本
            if (checkerSetEntities.size() == 1
                    && checkerSetEntity.getInitCheckers() != null && !checkerSetEntity.getInitCheckers()) {
                checkerSetEntity.setInitCheckers(true);
                if (checkerSetVersion != null) {
                    checkerSetEntity.setVersion(checkerSetVersion);
                    checkerSetEntity.setToolName(checkerSetVersionForTool);
                }
            } else {
                /*
                 * 已经初始化的规则集：
                 * 1 新规则集是测试、灰度规则集，则
                 * 1.1 已经存在测试/灰度的规则集，则不增加新数据，直接找到旧的测试/灰度规则集，更新相应字段即可
                 * 1.2 不存在测试/灰度规则集，则找到最新的一个版本的规则集，在最新版本上更新相应字段，然后生成一条新的测试/灰度规则集
                 * 2 新规则集是普通规则集，生成一条新版本的规则集（version + 1）
                 */
                checkerSetEntity = checkerSetEntities.stream()
                        .max(Comparator.comparing(CheckerSetEntity::getVersion)).get();
                if (checkerSetVersion != null) {
                    Integer finalCheckerSetVersion = checkerSetVersion;
                    CheckerSetEntity grayCheckerSet = checkerSetEntities.stream()
                            .filter(it -> finalCheckerSetVersion.equals(it.getVersion())).findFirst().orElse(null);
                    if (grayCheckerSet != null) {
                        checkerSetEntity = grayCheckerSet;
                    } else {
                        checkerSetEntity.setEntityId(null);
                    }
                    checkerSetEntity.setVersion(checkerSetVersion);
                    checkerSetEntity.setToolName(checkerSetVersionForTool);
                } else {
                    checkerSetEntity.setEntityId(null);
                    checkerSetEntity.setVersion(checkerSetEntity.getVersion() + 1);
                }
            }

            // 更新前，先保留规则集的老版本规则列表
            List<CheckerPropsEntity> oldCheckerProps = checkerSetEntity.getCheckerProps();
            checkerSetEntity.setCheckerProps(checkerPropsEntities);
            checkerSetEntity.setUpdatedBy(user);
            checkerSetEntity.setLastUpdateTime(System.currentTimeMillis());

            // 新规则集数据入库
            checkerSetRepository.save(checkerSetEntity);

            // 查询已关联此规则集，且选择了latest版本自动更新的项目数据
            List<CheckerSetProjectRelationshipEntity> projectRelationships =
                    checkerSetProjectRelationshipRepository.findByCheckerSetIdAndUselatestVersion(checkerSetId, true);
            if (CollectionUtils.isNotEmpty(projectRelationships)) {

                Integer newCheckerSetVersion = checkerSetEntity.getVersion();

                // 如果新规则集版本不是测试或灰度的规则集，则需要更新项目规则集版本
                if (newCheckerSetVersion != ToolIntegratedStatus.T.value()
                        && newCheckerSetVersion != ToolIntegratedStatus.G.value()) {
                    projectRelationships.forEach(it -> it.setVersion(newCheckerSetVersion));
                    checkerSetProjectRelationshipRepository.saveAll(projectRelationships);

                    handleNormalProject(checkerSetEntity, projectRelationships, user);
                } else {
                    // 如果是测试或灰度规则集，且项目是测试或灰度项目，则设置测试或灰度的项目为强制全量，且更新工具
                    CheckerSetEntity fromCheckerSet = new CheckerSetEntity();
                    fromCheckerSet.setCheckerProps(oldCheckerProps);
                    updateTaskAfterChangeCheckerSet(checkerSetEntity, fromCheckerSet, projectRelationships, user);
                }
            }
        }
    }

    /**
     * 刷新本次规则更新涉及的任务的信息。包括强制全量标志，工具，告警状态等
     *
     * @param checkerSetEntity
     * @param fromCheckerSet
     * @param projectRelationships
     * @param user
     */
    @Override
    public void updateTaskAfterChangeCheckerSet(CheckerSetEntity checkerSetEntity, CheckerSetEntity fromCheckerSet,
            List<CheckerSetProjectRelationshipEntity> projectRelationships,
            String user) {
        String checkerSetId = checkerSetEntity.getCheckerSetId();
        Integer version = checkerSetEntity.getVersion();
        String toolName = checkerSetEntity.getToolName();

        // 获取灰度项目清单(灰度配置新增工具维度，故以projectId + toolName做key)
        Map<String, Integer> grayToolProjectMap = getGrayToolProjectMap(projectRelationships);

        Set<String> needRefreshProjects = projectRelationships.stream()
                .filter(it -> {
                    Integer grayStatus = grayToolProjectMap.get(it.getProjectId() + toolName);
                    if (version < ToolIntegratedStatus.P.value() && version.equals(grayStatus)) {
                        // 测试/灰度的规则集，只需要刷测试/灰度的项目
                        return true;
                    } else if (version > ToolIntegratedStatus.P.value()
                            && (grayStatus == null || grayStatus == ToolIntegratedStatus.P.value())) {
                        // 正式的规则集，只需要刷正式的项目
                        return true;
                    }
                    return false;
                })
                .map(it -> it.getProjectId()).collect(Collectors.toSet());

        if (CollectionUtils.isNotEmpty(needRefreshProjects)) {
            List<CheckerSetTaskRelationshipEntity> taskRelationshipEntities =
                    checkerSetTaskRelationshipRepository.findByCheckerSetIdAndProjectIdIn(checkerSetId,
                            needRefreshProjects);

            setTaskForceFullScan(taskRelationshipEntities, checkerSetEntity, fromCheckerSet, user, true);
        }
    }

    @Override
    public void updateTaskAfterChangeCheckerSet(
            CheckerSetEntity checkerSetEntity, CheckerSetEntity fromCheckerSet, int version, String user) {
        String checkerSetId = checkerSetEntity.getCheckerSetId();

        List<CheckerSetTaskRelationshipEntity> taskRelationshipEntities =
                checkerSetTaskRelationshipRepository.findByCheckerSetIdAndVersion(checkerSetId, version);

        setTaskForceFullScan(taskRelationshipEntities, checkerSetEntity, fromCheckerSet, user, false);
    }

    /**
     * 设置强制全量扫描
     *
     * @param taskRelationshipEntities
     * @param checkerSetEntity
     * @param fromCheckerSet
     * @param user
     * @param loadAllCheckerSetToUpdateTools 是否加载所有规则集以更新工具的"停/启"状态
     */
    private void setTaskForceFullScan(
            List<CheckerSetTaskRelationshipEntity> taskRelationshipEntities,
            CheckerSetEntity checkerSetEntity, CheckerSetEntity fromCheckerSet,
            String user, boolean loadAllCheckerSetToUpdateTools
    ) {
        // 对各任务设置强制全量扫描标志，并修改告警状态
        ThreadPoolUtil.addRunnableTask(() -> {
            List<CheckerSetEntity> fromCheckerSets = Lists.newArrayList(fromCheckerSet);
            List<CheckerSetEntity> toCheckerSets = Lists.newArrayList(checkerSetEntity);
            taskRelationshipEntities.forEach(it -> {
                Long taskId = it.getTaskId();
                String projectId = it.getProjectId();

                log.info("set task to force full scan for checker set change: {}", taskId);

                setForceFullScanAndUpdateDefectAndToolStatus(taskId, fromCheckerSets, toCheckerSets);
                Set<String> updatedToolSet = Sets.newHashSet(ComConstants.Tool.SCC.name());
                List<String> checkerSetIdList = Lists.newArrayList();

                // 是否需要加载全部规则集以刷新工具状态
                if (loadAllCheckerSetToUpdateTools) {
                    List<CheckerSetVO> allCheckerSetOfTask = checkerSetQueryBizService.getTaskCheckerSetsCore(projectId,
                            Lists.newArrayList(taskId), Lists.newArrayList(), true);

                    for (CheckerSetVO checkerSetVO : allCheckerSetOfTask) {
                        checkerSetIdList.add(checkerSetVO.getCheckerSetId());

                        if (checkerSetVO.getCheckerProps() == null) {
                            continue;
                        }

                        for (CheckerPropVO checkerProp : checkerSetVO.getCheckerProps()) {
                            if (StringUtils.isNotEmpty(checkerProp.getToolName())) {
                                updatedToolSet.add(checkerProp.getToolName());
                            }
                        }
                    }
                } else {
                    checkerSetIdList.add(toCheckerSets.get(0).getCheckerSetId());

                    for (CheckerPropsEntity checkerPropsEntity : toCheckerSets.get(0).getCheckerProps()) {
                        if (StringUtils.isNotBlank(checkerPropsEntity.getToolName())) {
                            updatedToolSet.add(checkerPropsEntity.getToolName());
                        }
                    }
                }

                log.info("update tool for checker set change, task id: {}, project id: {}, checker set: {}, "
                                + "update tool set: {}",
                        taskId, projectId, checkerSetIdList, updatedToolSet);

                updateTools(user, taskId, updatedToolSet);
            });
        });
    }

    /**
     * 处理本次规则更新涉及的普通项目
     *
     * @param checkerSetEntity
     * @param projectRelationships
     * @param user
     */
    private void handleNormalProject(CheckerSetEntity checkerSetEntity,
            List<CheckerSetProjectRelationshipEntity> projectRelationships, String user) {
        String checkerSetId = checkerSetEntity.getCheckerSetId();
        String toolName = checkerSetEntity.getToolName();

        // 获取灰度项目清单(灰度配置新增工具维度，故以projectId + toolName做key)
        Map<String, Integer> grayToolProjectMap = getGrayToolProjectMap(projectRelationships);

        // 找出普通项目之前关联的规则集版本映射
        Map<String, Integer> oldCheckerSetVersionMap = Maps.newHashMap();
        projectRelationships.forEach(it -> {
            Integer grayToolProjectStatus = grayToolProjectMap.get(it.getProjectId() + toolName);
            if (grayToolProjectStatus == null || grayToolProjectStatus == 0) {
                oldCheckerSetVersionMap.put(it.getProjectId(), it.getVersion());
            }
        });
        if (MapUtils.isNotEmpty(oldCheckerSetVersionMap)) {
            // 刷新告警状态并设置强制全量扫描标志(只有使用了latest规则集的任务才需要刷新)
            Map<Long, Map<String, Integer>> currentTaskCheckerSetMap = Maps.newHashMap();
            List<CheckerSetTaskRelationshipEntity> taskRelationshipEntities =
                    checkerSetTaskRelationshipRepository.findByCheckerSetIdAndProjectIdIn(checkerSetId,
                            oldCheckerSetVersionMap.keySet());
            taskRelationshipEntities.stream()
                    .filter(it -> oldCheckerSetVersionMap.get(it.getProjectId()) != null)
                    .forEach(it -> {
                        currentTaskCheckerSetMap.computeIfAbsent(it.getTaskId(), k -> Maps.newHashMap());
                        currentTaskCheckerSetMap.get(it.getTaskId()).put(checkerSetId,
                                oldCheckerSetVersionMap.get(it.getProjectId()));
                    });

            Map<Long, Map<String, Integer>> updatedTaskCheckerSetMap = Maps.newHashMap();
            updatedTaskCheckerSetMap.putAll(currentTaskCheckerSetMap);

            for (Map.Entry<Long, Map<String, Integer>> entry : updatedTaskCheckerSetMap.entrySet()) {
                if (entry.getValue().get(checkerSetId) != null) {
                    entry.getValue().put(checkerSetId, checkerSetEntity.getVersion());
                }
            }

            // 对各任务设置强制全量扫描标志，并修改告警状态
            ThreadPoolUtil.addRunnableTask(() -> setForceFullScanAndUpdateDefectAndToolStatus(
                    currentTaskCheckerSetMap, updatedTaskCheckerSetMap, user));
        }
    }

    /**
     * 获取灰度项目清单
     *
     * @param relationships
     * @return
     */
    @NotNull
    private Map<String, Integer> getGrayToolProjectMap(List<CheckerSetProjectRelationshipEntity> relationships) {
        Set<String> projectSet = relationships.stream().map(it -> it.getProjectId()).collect(Collectors.toSet());
        Result<List<GrayToolProjectVO>> result = client.get(ServiceGrayToolProjectResource.class)
                .getGrayToolProjectByProjectIds(projectSet);
        if (result.isNotOk()) {
            log.error("getGrayToolProjectByProjectIds fail.");
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }

        Map<String, Integer> grayToolProjectVOMap = new HashMap<>();
        List<GrayToolProjectVO> grayToolProjectVOList = result.getData();
        if (CollectionUtils.isNotEmpty(grayToolProjectVOList)) {
            grayToolProjectVOList.forEach(
                    it -> grayToolProjectVOMap.put(it.getProjectId() + it.getToolName(), it.getStatus()));
        }
        return grayToolProjectVOMap;
    }

    /**
     * 获取规则集的版本，T-测试，G-灰度，P-发布
     * 1.如果规则集包含 T-测试 版本的规则，那么优先设置规则集为测试规则集
     * 2.如果规则集包含 G-灰度 版本的规则，那么优先设置规则集为灰度规则集
     * 3.否则规则集为正式规则集
     * 注意：
     * 规则集的version只能跟随单个工具的灰度/测试状态，如果规则checkerProps中含有多个工具的灰度/测试规则，则提示报错：
     * “规则集不允许包含多个灰度工具的版本的规则”
     *
     * @param checkerList 规矩详情清单
     * @return Pair: version, tool
     */
    @Nullable
    private Pair<Integer, String> getCheckerSetVersion(@NotNull List<CheckerDetailEntity> checkerList) {
        Pair<Integer, String> pair = null;

        List<Integer> notProdCheckerVersions =
                Lists.newArrayList(ToolIntegratedStatus.T.value(), ToolIntegratedStatus.G.value());

        // 保存非生产的规则版本
        Set<Integer> checkerVersions = Sets.newHashSet();
        Map<String, List<String>> toolGrayCheckersMap = Maps.newHashMap();
        for (CheckerDetailEntity checkerDetail : checkerList) {
            int checkerVersion = checkerDetail.getCheckerVersion();

            // 如果是非生产的规则版本
            if (notProdCheckerVersions.contains(checkerVersion)) {
                List<String> grayCheckers =
                        toolGrayCheckersMap.computeIfAbsent(checkerDetail.getToolName(), v -> Lists.newArrayList());
                grayCheckers.add(checkerDetail.getCheckerKey());
                checkerVersions.add(checkerVersion);
            }
        }
        int grayToolCount = toolGrayCheckersMap.size();
        if (grayToolCount > 1) {
            /* 如果grayToolCount大于1，说明存在多个灰度/测试状态的工具规则，则抛异常
               此处只能有一种工具，不允许同时勾选多个工具的灰度(测试)规则
               规则集的version只能跟随单个工具的灰度配置，测试优先于灰度状态
             */
            Locale locale = AbstractI18NResponseAspect.getLocale();
            String messageTitle = I18NUtils.getMessage("CHECKER_SET_NOT_ALLOW_MULTI_GRAY_TOOL_RULE", locale);
            if (StringUtils.isNotEmpty(messageTitle)) {
                messageTitle = messageTitle.replace("{n}", String.valueOf(grayToolCount));
            }
            StringBuilder strBuilder = new StringBuilder(messageTitle);

            toolGrayCheckersMap.forEach(
                    (tool, grayCheckers) -> {
                        String checkerStr = String.join("、", grayCheckers);
                        // 仅保留200字符长度，超过则截断
                        if (checkerStr.length() > ComConstants.SHOW_CHECKER_KEY_LENGTH) {
                            checkerStr = checkerStr.substring(0, ComConstants.SHOW_CHECKER_KEY_LENGTH) + "...";
                        }
                        String messageInfo = I18NUtils.getMessage("CHECKER_SET_PROPS_KEY_FORMAT", locale);
                        if (StringUtils.isNotEmpty(messageInfo)) {
                            messageInfo = messageInfo.replace("{tool}", toolMetaCache.getToolDisplayName(tool))
                                    .replace("{checker}", checkerStr);
                        }
                        strBuilder.append("\n").append(messageInfo);
                    });
            throw new CodeCCException(CommonMessageCode.NOT_ALLOW_MULTI_GRAY_TOOL_RULE, strBuilder.toString());
        }

        if (checkerVersions.contains(ToolIntegratedStatus.T.value())) {
            pair = Pair.of(ComConstants.ToolIntegratedStatus.T.value(), toolGrayCheckersMap.keySet().iterator().next());
        } else if (checkerVersions.contains(ToolIntegratedStatus.G.value())) {
            pair = Pair.of(ComConstants.ToolIntegratedStatus.G.value(), toolGrayCheckersMap.keySet().iterator().next());
        }
        return pair;
    }

    private List<String> getCodelangs(long codeLang, List<BaseDataVO> codeLangParams) {
        List<String> codeLangs = Lists.newArrayList();
        for (BaseDataVO codeLangParam : codeLangParams) {
            int paramCodeInt = Integer.valueOf(codeLangParam.getParamCode());
            if ((codeLang & paramCodeInt) != 0) {
                codeLangs.add(codeLangParam.getParamName());
            }
        }
        return codeLangs;
    }

    /**
     * 修改规则集基础信息
     *
     * @param checkerSetId
     * @param updateCheckerSetReq
     */
    @Override
    public void updateCheckerSetBaseInfo(String checkerSetId, String projectId,
            V3UpdateCheckerSetReqVO updateCheckerSetReq) {
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
        if (CollectionUtils.isNotEmpty(checkerSetEntities)) {
            List<CheckerSetCatagoryEntity> catagoryEntities = getCatagoryEntities(updateCheckerSetReq.getCatagories());
            for (CheckerSetEntity checkerSetEntity : checkerSetEntities) {
                if (!projectId.equals(checkerSetEntity.getProjectId())) {
                    String errMsg = "不能修改其他项目的规则集！";
                    log.error(errMsg);
                    throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
                }
                checkerSetEntity.setCheckerSetName(updateCheckerSetReq.getCheckerSetName());
                checkerSetEntity.setDescription(updateCheckerSetReq.getDescription());
                checkerSetEntity.setCatagories(catagoryEntities);
            }
            checkerSetRepository.saveAll(checkerSetEntities);
        }
    }

    /**
     * 规则集关联到项目或任务
     *
     * @param checkerSetId
     * @param checkerSetRelationshipVO
     */
    @Override
    public void setRelationships(String checkerSetId, String user, CheckerSetRelationshipVO checkerSetRelationshipVO) {
        String projectId = checkerSetRelationshipVO.getProjectId();
        Long taskId = checkerSetRelationshipVO.getTaskId();

        CheckerSetProjectRelationshipEntity projectRelationshipEntity = null;
        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntities =
                checkerSetProjectRelationshipRepository.findByProjectId(projectId);
        Map<String, Integer> checkerSetVersionMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(projectRelationshipEntities)) {
            for (CheckerSetProjectRelationshipEntity relationshipEntity : projectRelationshipEntities) {
                if (relationshipEntity.getCheckerSetId().equals(checkerSetId)) {
                    projectRelationshipEntity = relationshipEntity;
                }
                checkerSetVersionMap.put(relationshipEntity.getCheckerSetId(), relationshipEntity.getVersion());
            }
        }

        log.info("project relation ship entity is: {}, {}, {}", projectRelationshipEntity, projectId, taskId);

        if (CheckerConstants.CheckerSetRelationshipType.PROJECT.name().equals(checkerSetRelationshipVO.getType())) {
            if (projectRelationshipEntity != null) {
                log.error("关联已存在！: {}, {}, {}", checkerSetId, projectId, taskId);
                return;
            }
            CheckerSetProjectRelationshipEntity newProjectRelationshipEntity =
                    new CheckerSetProjectRelationshipEntity();
            newProjectRelationshipEntity.setCheckerSetId(checkerSetId);
            newProjectRelationshipEntity.setProjectId(projectId);
            newProjectRelationshipEntity.setUselatestVersion(true);
            newProjectRelationshipEntity.setDefaultCheckerSet(false);
            if (checkerSetRelationshipVO.getVersion() == null) {
                Map<String, Integer> latestVersionMap = getLatestVersionMap(Sets.newHashSet(checkerSetId));
                newProjectRelationshipEntity.setVersion(latestVersionMap.get(checkerSetId));
            } else {
                newProjectRelationshipEntity.setVersion(checkerSetRelationshipVO.getVersion());
            }
            checkerSetProjectRelationshipRepository.save(newProjectRelationshipEntity);
            log.info("set new task relation ship successfully: {}, {}, {}", checkerSetId, projectId, taskId);
        } else if (CheckerConstants.CheckerSetRelationshipType.TASK.name().equals(checkerSetRelationshipVO.getType())) {
            if (projectRelationshipEntity == null) {
                List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
                if (CollectionUtils.isNotEmpty(checkerSetEntities)) {
                    CheckerSetEntity latestVersionCheckerSet = checkerSetEntities.get(0);
                    for (CheckerSetEntity checkerSetEntity : checkerSetEntities) {
                        if (checkerSetEntity.getVersion() > latestVersionCheckerSet.getVersion()) {
                            latestVersionCheckerSet = checkerSetEntity;
                        }
                    }
                    if (Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name())
                            .contains(latestVersionCheckerSet.getCheckerSetSource())) {
                        projectRelationshipEntity = new CheckerSetProjectRelationshipEntity();
                        projectRelationshipEntity.setCheckerSetId(checkerSetId);
                        projectRelationshipEntity.setProjectId(projectId);
                        projectRelationshipEntity.setUselatestVersion(true);
                        //默认是默认规则集
                        if (CheckerSetSource.DEFAULT.name().equals(latestVersionCheckerSet.getCheckerSetSource())) {
                            projectRelationshipEntity.setDefaultCheckerSet(true);
                        } else {
                            projectRelationshipEntity.setDefaultCheckerSet(false);
                        }
                        projectRelationshipEntity.setVersion(latestVersionCheckerSet.getVersion());
                        checkerSetVersionMap.put(checkerSetId, latestVersionCheckerSet.getVersion());
                        checkerSetProjectRelationshipRepository.save(projectRelationshipEntity);
                    }
                }
                if (projectRelationshipEntity == null) {
                    String errMsg = "规则集没有安装到项目！";
                    log.error(errMsg);
                    throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
                }
            }

            CheckerSetTaskRelationshipEntity taskRelationshipEntity = null;
            Map<Long, Map<String, Integer>> currentTaskCheckerSetMap = Maps.newHashMap();
            List<CheckerSetTaskRelationshipEntity> taskRelationshipEntities =
                    checkerSetTaskRelationshipRepository.findByTaskId(taskId);
            if (CollectionUtils.isNotEmpty(taskRelationshipEntities)) {
                for (CheckerSetTaskRelationshipEntity relationshipEntity : taskRelationshipEntities) {
                    if (relationshipEntity.getCheckerSetId().equals(checkerSetId)) {
                        taskRelationshipEntity = relationshipEntity;
                    }
                    currentTaskCheckerSetMap.computeIfAbsent(relationshipEntity.getTaskId(), k -> Maps.newHashMap());
                    currentTaskCheckerSetMap.get(relationshipEntity.getTaskId())
                            .put(relationshipEntity.getCheckerSetId(),
                                    checkerSetVersionMap.get(relationshipEntity.getCheckerSetId()));
                }
            }
            if (taskRelationshipEntity != null) {
                log.error("关联已存在！: {}, {}, {}", checkerSetId, projectId, taskId);
                return;
            }
            CheckerSetTaskRelationshipEntity newTaskRelationshipEntity = new CheckerSetTaskRelationshipEntity();
            newTaskRelationshipEntity.setCheckerSetId(checkerSetId);
            newTaskRelationshipEntity.setProjectId(projectId);
            newTaskRelationshipEntity.setTaskId(taskId);
            checkerSetTaskRelationshipRepository.save(newTaskRelationshipEntity);
            log.info("set new task relation ship successfully: {}, {}, {}", checkerSetId, projectId, taskId);

            // 任务关联规则集需要设置全量扫描
            CheckerSetEntity checkerSetEntity = checkerSetRepository.findFirstByCheckerSetIdAndVersion(checkerSetId,
                    projectRelationshipEntity.getVersion());
            if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())) {
                Set<String> toolSet = Sets.newHashSet();
                for (CheckerPropsEntity checkerPropsEntity : checkerSetEntity.getCheckerProps()) {
                    toolSet.add(checkerPropsEntity.getToolName());
                }
                toolBuildInfoService.setForceFullScan(taskId, Lists.newArrayList(toolSet));
            }

            // 设置强制全量扫描标志并刷新告警状态
            Map<Long, Map<String, Integer>> updatedTaskCheckerSetMap;
            try {
                updatedTaskCheckerSetMap = Maps.newHashMap();
                updatedTaskCheckerSetMap.put(taskId, CloneUtils.cloneObject(currentTaskCheckerSetMap.get(taskId)));
                if (null != updatedTaskCheckerSetMap.get(taskId)) {
                    updatedTaskCheckerSetMap.get(taskId).put(checkerSetId, checkerSetVersionMap.get(checkerSetId));
                }

                // 对各任务设置强制全量扫描标志，并修改告警状态
                Map<Long, Map<String, Integer>> finalUpdatedTaskCheckerSetMap = updatedTaskCheckerSetMap;
                ThreadPoolUtil.addRunnableTask(() -> {
                    setForceFullScanAndUpdateDefectAndToolStatus(currentTaskCheckerSetMap,
                            finalUpdatedTaskCheckerSetMap,
                            user);
                });
            } catch (CloneNotSupportedException e) {
                log.error("copy currentTaskCheckerSetMap fail!");
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }
        } else {
            String errMsg = "关联类型非法！";
            log.error(errMsg);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
        }
    }

    @Override
    public Pair<Boolean, String> setRelationshipsOnce(String user, String projectId, long taskId, String toolName) {
        CheckerSetRelationshipVO projectCheckerSetRelationshipVO = new CheckerSetRelationshipVO();
        projectCheckerSetRelationshipVO.setType("PROJECT");
        projectCheckerSetRelationshipVO.setProjectId(projectId);
        projectCheckerSetRelationshipVO.setTaskId(taskId);

        CheckerSetRelationshipVO taskCheckerSetRelationshipVO = new CheckerSetRelationshipVO();
        taskCheckerSetRelationshipVO.setType("TASK");
        taskCheckerSetRelationshipVO.setProjectId(projectId);
        taskCheckerSetRelationshipVO.setTaskId(taskId);

        TaskDetailVO taskInfo = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId).getData();
        List<BaseDataVO> baseDataVOList =
                client.get(ServiceBaseDataResource.class).getParamsByType(ONCE_CHECKER_SET_KEY).getData();

        List<BaseDataVO> toolBaseData = null;
        if (CollectionUtils.isNotEmpty(baseDataVOList)) {
            toolBaseData = baseDataVOList.stream()
                    .filter(it -> it.getParamName().equals(toolName)
                            && (Integer.valueOf(it.getParamCode()) & taskInfo.getCodeLang()) != 0)
                    .collect(Collectors.toList());
        }

        if (CollectionUtils.isEmpty(toolBaseData)) {
            return Pair.of(false, "该任务语言暂无合适规则集");
        }

        toolBaseData.forEach(baseDataVO -> {
            String checkerSetId = baseDataVO.getParamExtend1();
            log.info("start to open checker for task: {}, {}", taskId, checkerSetId);

            // 先安装
            setRelationships(checkerSetId, user, projectCheckerSetRelationshipVO);

            // 再关联
            setRelationships(checkerSetId, user, taskCheckerSetRelationshipVO);
        });

        return Pair.of(true, "");
    }

    /**
     * 任务批量关联规则集
     *
     * @param projectId
     * @param taskId
     * @param checkerSetList
     * @param user
     * @return
     */
    @Override
    public Boolean batchRelateTaskAndCheckerSet(String projectId,
            Long taskId,
            List<CheckerSetVO> checkerSetList,
            String user,
            Boolean isOpenSource) {
        log.info("start to batch relate task and checker set: {}, {}, {}", projectId, taskId, checkerSetList);

        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntityList =
                checkerSetProjectRelationshipRepository.findByProjectId(projectId);
        Map<String, CheckerSetProjectRelationshipEntity> projInstallCheckerSetMap = projectRelationshipEntityList
                .stream().collect(Collectors.toMap(it -> it.getCheckerSetId(), Function.identity(), (k, v) -> k));

        List<CheckerSetTaskRelationshipEntity> existTaskRelationshipEntityList =
                checkerSetTaskRelationshipRepository.findByTaskId(taskId);
        Map<String, CheckerSetTaskRelationshipEntity> existTaskRelatedCheckerMap = null;
        if (CollectionUtils.isNotEmpty(existTaskRelationshipEntityList)) {
            existTaskRelatedCheckerMap = existTaskRelationshipEntityList.stream()
                    .collect(Collectors.toMap(
                            CheckerSetTaskRelationshipEntity::getCheckerSetId, Function.identity(), (k, v) -> v));
        }
        Map<String, CheckerSetVO> originCheckerSetMap =
                checkerSetList.stream().collect(Collectors.toMap(CheckerSetVO::getCheckerSetId,
                        Function.identity(), (k, v) -> v));
        Set<String> checkerSetIds = originCheckerSetMap.keySet();

        List<CheckerSetEntity> checkerSetEntityList = checkerSetRepository.findByCheckerSetIdIn(checkerSetIds);

        // 找到每个规则集中版本号最大的一个规则集
        Map<String, CheckerSetEntity> maxCheckerSetEntityMap = checkerSetEntityList.stream()
                .collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId))
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().max(
                                Comparator.comparingInt(CheckerSetEntity::getVersion)).orElse(new CheckerSetEntity())));

        // 项目还没安装的规则集是非法规则集
        List<String> invalidCheckerSet = new ArrayList<>();

        // 关联规则集需要设置全量扫描的任务工具
        Set<String> toolSet = new HashSet<>();
        List<CheckerSetTaskRelationshipEntity> taskRelationshipEntityList = new ArrayList<>();
        //如果是官方推荐和官方优选的话，还需要关联项目表
        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntities = new ArrayList<>();
        long currTime = System.currentTimeMillis();
        for (CheckerSetVO checkerSetVO : checkerSetList) {
            String checkerSetId = checkerSetVO.getCheckerSetId();
            CheckerSetEntity maxVersionCheckerSet = maxCheckerSetEntityMap.get(checkerSetId);
            if (maxVersionCheckerSet == null) {
                log.error("projectId:{}, taskId:{}, checkerSetId:{} max checker set version is null",
                        projectId, taskId, checkerSetId);
                continue;
            }
            if (!projInstallCheckerSetMap.containsKey(checkerSetId)) {
                //如果是官方的话 需要关联
                boolean matchCheckerSetSource = StringUtils.isNotBlank(maxVersionCheckerSet.getCheckerSetSource())
                        && Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name())
                        .contains(maxVersionCheckerSet.getCheckerSetSource());
                if ((null != isOpenSource && isOpenSource)
                        || matchCheckerSetSource
                        || (maxVersionCheckerSet.getLegacy() != null && maxVersionCheckerSet.getLegacy()
                        && CheckerConstants.CheckerSetOfficial.OFFICIAL.code() == maxVersionCheckerSet.getOfficial())) {
                    log.info("start to create project relationship: isOpenSource:{}, matchCheckerSetSource:{}, "
                                    + "maxVersionCheckerSet.getLegacy():{}, maxVersionCheckerSet.getOfficial(): {}",
                            isOpenSource, matchCheckerSetSource,
                            maxVersionCheckerSet.getLegacy(), maxVersionCheckerSet.getOfficial());
                    //关联项目关联表
                    CheckerSetProjectRelationshipEntity checkerSetProjectRelationshipEntity =
                            new CheckerSetProjectRelationshipEntity();
                    checkerSetProjectRelationshipEntity.setProjectId(projectId);
                    checkerSetProjectRelationshipEntity.setCheckerSetId(checkerSetVO.getCheckerSetId());
                    //如果是开源扫描，并且版本不为空，则设置版本
                    Integer curVersion = 0;
                    if ((null != isOpenSource && isOpenSource)
                            && null != checkerSetVO.getVersion()
                            && Integer.MAX_VALUE != checkerSetVO.getVersion()) {
                        curVersion = checkerSetVO.getVersion();
                        checkerSetProjectRelationshipEntity.setVersion(checkerSetVO.getVersion());
                        checkerSetProjectRelationshipEntity.setUselatestVersion(false);
                    } else { // 否则项目首次关联规则集版本都默认使用最新规则集版本
                        curVersion = maxVersionCheckerSet.getVersion();
                        checkerSetProjectRelationshipEntity.setVersion(maxVersionCheckerSet.getVersion());
                        checkerSetProjectRelationshipEntity.setUselatestVersion(true);
                    }
                    if (CheckerSetSource.DEFAULT.name().equals(checkerSetVO.getCheckerSetSource())) {
                        checkerSetProjectRelationshipEntity.setDefaultCheckerSet(true);
                    } else {
                        checkerSetProjectRelationshipEntity.setDefaultCheckerSet(false);
                    }
                    projectRelationshipEntities.add(checkerSetProjectRelationshipEntity);

                    //在关联任务的关联表
                    CheckerSetTaskRelationshipEntity newRelationshipEntity = new CheckerSetTaskRelationshipEntity();
                    newRelationshipEntity.setCheckerSetId(checkerSetId);
                    newRelationshipEntity.setProjectId(projectId);
                    newRelationshipEntity.setTaskId(taskId);
                    newRelationshipEntity.setCreatedBy(user);
                    newRelationshipEntity.setCreatedDate(currTime);

                    // 因为如果是使用预发布规则集，那么应该优先取传进来的version，传进来为空则取最大的规则集版本
                    Integer version = checkerSetVO.getVersion() != null ? checkerSetVO.getVersion() : curVersion;
                    newRelationshipEntity.setVersion(version);
                    taskRelationshipEntityList.add(newRelationshipEntity);
                    if (CollectionUtils.isNotEmpty(checkerSetVO.getToolList())) {
                        toolSet.addAll(checkerSetVO.getToolList());
                    }
                } else {
                    invalidCheckerSet.add(checkerSetId);
                }
            } else {
                //如果是开源的，并且版本号与原来不一致，则需要更新版本号
                if ((null != isOpenSource && isOpenSource) && null != checkerSetVO.getVersion()) {
                    CheckerSetProjectRelationshipEntity checkerSetProjectRelationshipEntity =
                            projInstallCheckerSetMap.get(checkerSetId);

                    if (checkerSetProjectRelationshipEntity != null
                            && !checkerSetVO.getVersion().equals(checkerSetProjectRelationshipEntity.getVersion())) {
                        if (Integer.MAX_VALUE != checkerSetVO.getVersion()) {
                            checkerSetProjectRelationshipEntity.setVersion(checkerSetVO.getVersion());
                            checkerSetProjectRelationshipEntity.setUselatestVersion(false);
                        } else {
                            checkerSetProjectRelationshipEntity.setVersion(maxVersionCheckerSet.getVersion());
                            checkerSetProjectRelationshipEntity.setUselatestVersion(true);
                        }

                        projectRelationshipEntities.add(checkerSetProjectRelationshipEntity);
                    }
                }

                // 还没有被任务关联的规则集则创建关联
                if (MapUtils.isEmpty(existTaskRelatedCheckerMap)
                        || !existTaskRelatedCheckerMap.containsKey(checkerSetId)) {
                    CheckerSetTaskRelationshipEntity newRelationshipEntity = new CheckerSetTaskRelationshipEntity();
                    newRelationshipEntity.setCheckerSetId(checkerSetId);
                    newRelationshipEntity.setProjectId(projectId);
                    newRelationshipEntity.setTaskId(taskId);
                    newRelationshipEntity.setCreatedBy(user);
                    newRelationshipEntity.setCreatedDate(currTime);
                    newRelationshipEntity.setVersion(checkerSetVO.getVersion());
                    taskRelationshipEntityList.add(newRelationshipEntity);
                    if (CollectionUtils.isNotEmpty(checkerSetVO.getToolList())) {
                        toolSet.addAll(checkerSetVO.getToolList());
                    }
                }

                // 更新现有的
                if (MapUtils.isNotEmpty(existTaskRelatedCheckerMap)) {
                    CheckerSetTaskRelationshipEntity relationshipEntity = existTaskRelatedCheckerMap.get(checkerSetId);
                    if (relationshipEntity != null) {
                        // 版本不一样则强制全量
                        if (relationshipEntity.getVersion() != null
                                && checkerSetVO.getVersion() != null
                                && !relationshipEntity.getVersion().equals(checkerSetVO.getVersion())) {
                            toolSet.addAll(checkerSetVO.getToolList());
                        }

                        // 每次都更新版本号
                        relationshipEntity.setVersion(checkerSetVO.getVersion());
                        taskRelationshipEntityList.add(relationshipEntity);
                    }
                }
            }

            // 不在本次关联列表中的都要解除关联
            if (MapUtils.isNotEmpty(existTaskRelatedCheckerMap)) {
                existTaskRelatedCheckerMap.remove(checkerSetId);
            }
        }

        if (CollectionUtils.isNotEmpty(invalidCheckerSet)) {
            StringBuffer errMsg = new StringBuffer();
            errMsg.append("项目未安装规则集: ")
                    .append(taskId).append(", ").append(JsonUtil.INSTANCE.toJson(invalidCheckerSet));
            log.error(errMsg.toString());
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg.toString()}, null);
        }

        checkerSetTaskRelationshipRepository.saveAll(taskRelationshipEntityList);

        // 如果是项目使用的是最新的规则集，则每次关联规则集时，都更新下规则集的最新版本
        projectRelationshipEntityList.forEach(it -> {
            if (it.getUselatestVersion() != null && it.getUselatestVersion()) {
                CheckerSetEntity maxCheckerSet = maxCheckerSetEntityMap.get(it.getCheckerSetId());
                if (maxCheckerSet != null && !it.getVersion().equals(maxCheckerSet.getVersion())) {
                    it.setVersion(maxCheckerSet.getVersion());
                    projectRelationshipEntities.add(it);
                }
            }
        });

        //保存新增或者需要更新的项目规则集关系
        if (CollectionUtils.isNotEmpty(projectRelationshipEntities)) {
            checkerSetProjectRelationshipRepository.saveAll(projectRelationshipEntities);
        }

        // 解除规则集关联
        if (MapUtils.isNotEmpty(existTaskRelatedCheckerMap)) {
            Collection<CheckerSetTaskRelationshipEntity> needDeleteTaskRelationshens =
                    existTaskRelatedCheckerMap.values();
            checkerSetTaskRelationshipRepository.deleteAll(needDeleteTaskRelationshens);

            // 解除关联的规则集涉及的工具也需要强制全量扫描
            Set<String> needDeleteCheckerSeIds = needDeleteTaskRelationshens.stream()
                    .map(it -> it.getCheckerSetId()).collect(Collectors.toSet());
            List<CheckerSetEntity> needDeleteCheckerSets =
                    checkerSetRepository.findByCheckerSetIdIn(needDeleteCheckerSeIds);
            Map<String, List<CheckerSetEntity>> checkerSetMap =
                    needDeleteCheckerSets.stream().collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId));
            checkerSetMap.forEach((checkerSetId, checkerSetEntities) -> {
                CheckerSetEntity selectCheckerSet = null;
                if (projInstallCheckerSetMap.get(checkerSetId).getUselatestVersion()) {
                    selectCheckerSet = checkerSetEntities.stream()
                            .max(Comparator.comparing(CheckerSetEntity::getVersion)).orElse(new CheckerSetEntity());
                } else {
                    for (CheckerSetEntity checkerSetEntity : checkerSetEntities) {
                        if (checkerSetEntity.getVersion()
                                .equals(projInstallCheckerSetMap.get(checkerSetId).getVersion())) {
                            selectCheckerSet = checkerSetEntity;
                        }
                    }
                }
                if (selectCheckerSet != null && CollectionUtils.isNotEmpty(selectCheckerSet.getCheckerProps())) {
                    Set<String> tools = selectCheckerSet.getCheckerProps().stream()
                            .map(CheckerPropsEntity::getToolName).collect(Collectors.toSet());
                    toolSet.addAll(tools);
                }
            });
        }

        // 关联规则集需要设置全量扫描
        toolBuildInfoService.setForceFullScan(taskId, Lists.newArrayList(toolSet));

        return true;
    }

    @Override
    public void management(String user, String checkerSetId, CheckerSetManagementReqVO checkerSetManagementReqVO) {

        // 校验规则集是否存在
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
        if (CollectionUtils.isEmpty(checkerSetEntities)) {
            String errMsg = I18NUtils.getMessage("CHECKER_SET_NOT_EXIST");
            log.error(errMsg);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
        }

        CheckerSetEntity firstCheckerSetEntity = checkerSetEntities.get(0);
        if (CheckerSetSource.DEFAULT.name().equals(firstCheckerSetEntity.getCheckerSetSource())
                || CheckerSetSource.RECOMMEND.name().equals(firstCheckerSetEntity.getCheckerSetSource())) {
            if (checkerSetManagementReqVO.getUninstallCheckerSet() != null
                    && checkerSetManagementReqVO.getUninstallCheckerSet()) {
                String errMsg = I18NUtils.getMessage("CHECKER_SET_RECOMMEND_NOT_ALLOW_DELETE");
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
        }

        // 校验设置为公开的规则集名称是否与公共规则集重复
        if (checkerSetManagementReqVO.getScope() != null
                && checkerSetManagementReqVO.getScope() == CheckerConstants.CheckerSetScope.PUBLIC.code()) {
            checkNameExistInPublic(firstCheckerSetEntity.getCheckerSetName());
        }

        // 校验用户是否有权限
        boolean havePermission;
        if (checkerSetManagementReqVO.getDiscardFromTask() == null) {
            log.info("management checkerSet version auth user {} | project {}",
                    user, checkerSetManagementReqVO.getProjectId());
            havePermission = authExPermissionApi.authProjectManager(checkerSetManagementReqVO.getProjectId(), user);
        } else {
            Set<String> tasks = authExPermissionApi.queryTaskListForUser(user, checkerSetManagementReqVO.getProjectId(),
                    Sets.newHashSet(CodeCCAuthAction.TASK_MANAGE.getActionName()));
            havePermission = tasks.contains(String.valueOf(checkerSetManagementReqVO.getDiscardFromTask()));
            log.info("management checkSet auth user {} | task {} | set {}",
                    user, checkerSetManagementReqVO.getDiscardFromTask(), tasks);
        }
        if (!havePermission && !firstCheckerSetEntity.getCreator().equals(user)) {
            String errMsg = I18NUtils.getMessage("CHECKER_SET_USER_UNAUTHORIZED_NOT_ALLOW_DELETE");
            log.error(errMsg);
            throw new CodeCCException("", errMsg);
        }

        // 查询任务关联规则集记录
        List<CheckerSetTaskRelationshipEntity> taskRelationshipEntities =
                checkerSetTaskRelationshipRepository.findByProjectId(checkerSetManagementReqVO.getProjectId());
        //过滤限制当前规则集id的规则集
        List<CheckerSetTaskRelationshipEntity> selectCheckerSetEntities = new ArrayList<>();
        Map<Long, CheckerSetTaskRelationshipEntity> taskRelationshipEntityMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(taskRelationshipEntities)) {
            for (CheckerSetTaskRelationshipEntity taskRelationshipEntity : taskRelationshipEntities) {
                if (checkerSetId.equalsIgnoreCase(taskRelationshipEntity.getCheckerSetId())) {
                    taskRelationshipEntityMap.put(taskRelationshipEntity.getTaskId(), taskRelationshipEntity);
                    selectCheckerSetEntities.add(taskRelationshipEntity);
                }
            }
        }

        /*
         * 1、不允许删除非本项目的规则集，
         * 2、不允许卸载本项目的规则集
         * 3、已在任务中使用的规则集不允许删除或卸载
         */
        if (checkerSetManagementReqVO.getDeleteCheckerSet() != null
                && checkerSetManagementReqVO.getDeleteCheckerSet()) {
            if (!checkerSetEntities.get(0).getProjectId().equals(checkerSetManagementReqVO.getProjectId())) {
                String errMsg = I18NUtils.getMessage("CHECKER_SET_NOT_BELONG_PROJECT_NOT_ALLOW_DELETE");
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg});
            }
            if (CollectionUtils.isNotEmpty(selectCheckerSetEntities)) {
                String errMsg = I18NUtils.getMessage("CHECKER_SET_IN_USE_NOT_ALLOW_DELETE");
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg});
            }
        }
        if (checkerSetManagementReqVO.getUninstallCheckerSet() != null
                && checkerSetManagementReqVO.getUninstallCheckerSet()) {
            if (checkerSetEntities.get(0).getProjectId().equals(checkerSetManagementReqVO.getProjectId())) {
                String errMsg = I18NUtils.getMessage("CHECKER_SET_CURRENT_PROJECT_NOT_ALLOW_DELETE");
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg});
            }
            if (CollectionUtils.isNotEmpty(selectCheckerSetEntities)) {
                String errMsg = I18NUtils.getMessage("CHECKER_SET_IN_USE_NOT_ALLOW_DELETE");
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg});
            }
        }

        // 查询当前项目关联规则集的列表
        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntities =
                checkerSetProjectRelationshipRepository.findByProjectId(checkerSetManagementReqVO.getProjectId());
        Map<String, CheckerSetProjectRelationshipEntity> projectRelationshipEntityMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(projectRelationshipEntities)) {
            for (CheckerSetProjectRelationshipEntity projectRelationshipEntity : projectRelationshipEntities) {
                projectRelationshipEntityMap.put(projectRelationshipEntity.getCheckerSetId(),
                        projectRelationshipEntity);
            }
        }

        // 获取各任务当前使用的规则集列表
        Map<Long, Map<String, Integer>> currentTaskUseCheckerSetsMap = Maps.newHashMap();
        Map<Long, Map<String, Integer>> updatedTaskUseCheckerSetsMap = Maps.newHashMap();
        for (Map.Entry<Long, CheckerSetTaskRelationshipEntity> entry : taskRelationshipEntityMap.entrySet()) {
            CheckerSetProjectRelationshipEntity projectRelationshipEntity =
                    projectRelationshipEntityMap.get(entry.getValue().getCheckerSetId());
            currentTaskUseCheckerSetsMap.computeIfAbsent(entry.getKey(), k -> Maps.newHashMap());
            currentTaskUseCheckerSetsMap.get(entry.getKey()).put(projectRelationshipEntity.getCheckerSetId(),
                    projectRelationshipEntity.getVersion());
            taskRelationshipEntities.stream()
                    .filter(taskRelationshipEntity -> taskRelationshipEntity.getTaskId()
                            .equals(entry.getKey()))
                    .forEach(taskRelationshipEntity ->
                            currentTaskUseCheckerSetsMap.get(
                                    entry.getKey()).put(taskRelationshipEntity.getCheckerSetId(),
                                    projectRelationshipEntityMap.get(
                                            taskRelationshipEntity.getCheckerSetId()).getVersion()));

            updatedTaskUseCheckerSetsMap.computeIfAbsent(entry.getKey(), k -> Maps.newHashMap());
            updatedTaskUseCheckerSetsMap.get(entry.getKey()).put(projectRelationshipEntity.getCheckerSetId(),
                    projectRelationshipEntity.getVersion());
            taskRelationshipEntities.stream()
                    .filter(taskRelationshipEntity -> taskRelationshipEntity
                            .getTaskId()
                            .equals(entry.getKey()))
                    .forEach(taskRelationshipEntity ->
                            updatedTaskUseCheckerSetsMap.get(
                                    entry.getKey()).put(taskRelationshipEntity.getCheckerSetId(),
                                    projectRelationshipEntityMap.get(
                                            taskRelationshipEntity.getCheckerSetId()).getVersion()));
        }

        //兼容官方推荐官方优选
        CheckerSetProjectRelationshipEntity sourceProjectRelationEntity = null;

        // 设置项目维度的默认规则集
        if (checkerSetManagementReqVO.getDefaultCheckerSet() != null) {
            CheckerSetProjectRelationshipEntity projectRelationshipEntity =
                    projectRelationshipEntityMap.get(checkerSetId);
            if (null != projectRelationshipEntity) {
                projectRelationshipEntity.setDefaultCheckerSet(checkerSetManagementReqVO.getDefaultCheckerSet());
                checkerSetProjectRelationshipRepository.save(projectRelationshipEntity);
            } else {
                //兼容官方推荐官方优选
                CheckerSetEntity checkerSetEntity = checkerSetEntities.get(0);
                if (Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name())
                        .contains(checkerSetEntity.getCheckerSetSource())) {
                    sourceProjectRelationEntity = new CheckerSetProjectRelationshipEntity();
                    sourceProjectRelationEntity.setCheckerSetId(checkerSetId);
                    sourceProjectRelationEntity.setProjectId(checkerSetManagementReqVO.getProjectId());
                    sourceProjectRelationEntity.setDefaultCheckerSet(checkerSetManagementReqVO.getDefaultCheckerSet());
                }
            }
        }

        // 规则集的可见范围、是否设为默认都要更新到所有版本
        for (CheckerSetEntity checkerSetEntity : checkerSetEntities) {
            if (checkerSetManagementReqVO.getScope() != null) {
                checkerSetEntity.setScope(checkerSetManagementReqVO.getScope());
            }

            // 从本项目删除后，规则集需要设置为私有，这样其他没安装的项目就找不到了
            if (checkerSetManagementReqVO.getDeleteCheckerSet() != null
                    && checkerSetManagementReqVO.getDeleteCheckerSet()) {
                checkerSetEntity.setScope(CheckerConstants.CheckerSetScope.PRIVATE.code());
            }
        }
        checkerSetRepository.saveAll(checkerSetEntities);

        // 从本项目中卸载规则集，或者删除本项目的规则集，都要删除关联数据
        CheckerSetProjectRelationshipEntity relationshipEntity = projectRelationshipEntityMap.get(checkerSetId);
        if ((checkerSetManagementReqVO.getDeleteCheckerSet() != null && checkerSetManagementReqVO.getDeleteCheckerSet())
                || (checkerSetManagementReqVO.getUninstallCheckerSet() != null
                && checkerSetManagementReqVO.getUninstallCheckerSet())) {
            if (relationshipEntity != null) {
                checkerSetProjectRelationshipRepository.delete(relationshipEntity);
            }

            // 修改更新后的任务规则集列表
            for (Map.Entry<Long, Map<String, Integer>> entry : updatedTaskUseCheckerSetsMap.entrySet()) {
                if (entry.getValue().containsKey(checkerSetId)) {
                    entry.getValue().remove(checkerSetId);
                }
            }
        }

        // 切换项目关联的规则集版本
        if (checkerSetManagementReqVO.getVersionSwitchTo() != null) {
            if (relationshipEntity != null) {
                if (checkerSetManagementReqVO.getVersionSwitchTo() == Integer.MAX_VALUE) {
                    Map<String, Integer> latestVersionMap = getLatestVersionMap(Sets.newHashSet(checkerSetId));
                    relationshipEntity.setVersion(latestVersionMap.get(checkerSetId));
                    relationshipEntity.setUselatestVersion(true);
                } else {
                    relationshipEntity.setVersion(checkerSetManagementReqVO.getVersionSwitchTo());
                    relationshipEntity.setUselatestVersion(false);
                }
            } else {
                CheckerSetEntity checkerSetEntity = checkerSetEntities.get(0);
                if (Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name())
                        .contains(checkerSetEntity.getCheckerSetSource())) {
                    if (null == sourceProjectRelationEntity) {
                        sourceProjectRelationEntity = new CheckerSetProjectRelationshipEntity();
                    }
                    sourceProjectRelationEntity.setCheckerSetId(checkerSetId);
                    sourceProjectRelationEntity.setProjectId(checkerSetManagementReqVO.getProjectId());
                    if (checkerSetManagementReqVO.getVersionSwitchTo() == Integer.MAX_VALUE) {
                        Map<String, Integer> latestVersionMap =
                                getLatestVersionMap(Sets.newHashSet(checkerSetEntity.getCheckerSetId()));
                        sourceProjectRelationEntity.setVersion(latestVersionMap.get(checkerSetId));
                        sourceProjectRelationEntity.setUselatestVersion(true);
                    } else {
                        sourceProjectRelationEntity.setVersion(checkerSetManagementReqVO.getVersionSwitchTo());
                        sourceProjectRelationEntity.setUselatestVersion(false);
                    }
                    if (null == sourceProjectRelationEntity.getDefaultCheckerSet()) {
                        if (CheckerSetSource.DEFAULT.name().equals(checkerSetEntity.getCheckerSetSource())) {
                            sourceProjectRelationEntity.setDefaultCheckerSet(true);
                        } else {
                            sourceProjectRelationEntity.setDefaultCheckerSet(false);
                        }
                    }
                }
            }
            if (relationshipEntity != null) {
                checkerSetProjectRelationshipRepository.save(relationshipEntity);

                // 修改更新后的任务规则集列表
                for (Map.Entry<Long, Map<String, Integer>> entry : updatedTaskUseCheckerSetsMap.entrySet()) {
                    if (entry.getValue().containsKey(checkerSetId)) {
                        entry.getValue().put(checkerSetId, relationshipEntity.getVersion());
                    }
                }
            }


        }

        if (null != sourceProjectRelationEntity) {
            checkerSetProjectRelationshipRepository.save(sourceProjectRelationEntity);
        }

        // 任务不再使用该规则集
        Long taskId = checkerSetManagementReqVO.getDiscardFromTask();
        if (taskId != null) {
            CheckerSetTaskRelationshipEntity taskRelationshipEntity = taskRelationshipEntityMap.get(taskId);
            if (taskRelationshipEntity != null) {
                List<CheckerSetTaskRelationshipEntity> currentTaskRelationships =
                        checkerSetTaskRelationshipRepository.findByTaskId(taskId);
                if (currentTaskRelationships.size() == 1) {
                    String errMsg = I18NUtils.getMessage("CHECKER_SET_AT_LEAST_ONE_FOR_TASK_NOT_ALLOW_DELETE");
                    log.error(errMsg);
                    throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg});
                }
                checkerSetTaskRelationshipRepository.delete(taskRelationshipEntity);
            }

            // 修改更新后的任务规则集列表
            Map<String, Integer> taskCheckerSetVersionMap = updatedTaskUseCheckerSetsMap.get(taskId);
            taskCheckerSetVersionMap.remove(checkerSetId);
        }

        // 对各任务设置强制全量扫描标志，并修改告警状态
        ThreadPoolUtil.addRunnableTask(() -> {
            setForceFullScanAndUpdateDefectAndToolStatus(currentTaskUseCheckerSetsMap, updatedTaskUseCheckerSetsMap,
                    user);
        });
    }

    @Override
    public Boolean updateCheckerSetAndTaskRelation(Long taskId, Long codeLang, String user) {
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList =
                checkerSetTaskRelationshipRepository.findByTaskId(taskId);
        List<CheckerSetEntity> projectCheckerSetList;
        Map<String, Integer> oldCheckerSetVersionMap = Maps.newHashMap();
        Map<String, Integer> newCheckerSetVersionMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(checkerSetTaskRelationshipEntityList)) {
            String projectId = checkerSetTaskRelationshipEntityList.get(0).getProjectId();
            List<CheckerSetProjectRelationshipEntity> checkerSetProjectRelationshipEntityList =
                    checkerSetProjectRelationshipRepository.findByProjectId(projectId);
            Set<String> checkerSetIds;

            if (CollectionUtils.isEmpty(checkerSetProjectRelationshipEntityList)) {
                projectCheckerSetList = new ArrayList<>();
                checkerSetIds = new HashSet<>();
            } else {
                checkerSetIds = checkerSetProjectRelationshipEntityList.stream()
                        .map(CheckerSetProjectRelationshipEntity::getCheckerSetId).collect(Collectors.toSet());

                Map<String, Integer> checkerSetVersionMap = checkerSetProjectRelationshipEntityList.stream()
                        .collect(HashMap::new, (m, v) -> m.put(v.getCheckerSetId(), v.getVersion()), HashMap::putAll);

                List<CheckerSetEntity> checkerSetEntityList = checkerSetDao.findByComplexCheckerSetCondition(null,
                        checkerSetIds, null, null, null, null, null, true, true);

                projectCheckerSetList = checkerSetEntityList.stream().filter(it ->
                        it.getVersion().equals(checkerSetVersionMap.get(it.getCheckerSetId()))
                                || CheckerSetSource.DEFAULT.name().equals(it.getCheckerSetSource())
                                || CheckerSetSource.RECOMMEND.name().equals(it.getCheckerSetSource())
                ).collect(Collectors.toList());
                oldCheckerSetVersionMap.putAll(checkerSetTaskRelationshipEntityList.stream()
                        .collect(Collectors.toMap(CheckerSetTaskRelationshipEntity::getCheckerSetId,
                                checkerSetTaskRelationEntity -> checkerSetVersionMap
                                        .get(checkerSetTaskRelationEntity.getCheckerSetId()), (k, v) -> v)));
                newCheckerSetVersionMap.putAll(checkerSetTaskRelationshipEntityList.stream()
                        .collect(Collectors.toMap(CheckerSetTaskRelationshipEntity::getCheckerSetId,
                                checkerSetTaskRelationEntity -> checkerSetVersionMap
                                        .get(checkerSetTaskRelationEntity.getCheckerSetId()), (k, v) -> v)));
            }

            //官方优选 官方推荐版本
            Map<String, Integer> officialMap = projectCheckerSetList.stream().filter(it ->
                            CheckerSetSource.DEFAULT.name().equals(it.getCheckerSetSource())
                                    || CheckerSetSource.RECOMMEND.name().equals(it.getCheckerSetSource()))
                    .collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId))
                    .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> entry.getValue().stream()
                                    .max(Comparator.comparingInt(CheckerSetEntity::getVersion))
                                    .orElse(new CheckerSetEntity()).getVersion()));

            List<CheckerSetEntity> taskCheckerSetList = projectCheckerSetList.stream().filter(checkerSetEntity ->
                    checkerSetTaskRelationshipEntityList.stream().anyMatch(it ->
                            it.getCheckerSetId().equals(checkerSetEntity.getCheckerSetId()))
            ).collect(Collectors.toList());

            //1. 解绑规则集
            Set<String> needToUnbindList = taskCheckerSetList.stream()
                    .filter(checkerSetEntity -> (codeLang & checkerSetEntity.getCodeLang()) == 0)
                    .map(CheckerSetEntity::getCheckerSetId).collect(Collectors.toSet());
            List<CheckerSetTaskRelationshipEntity> needToUnbindRelationEntityList =
                    checkerSetTaskRelationshipEntityList.stream()
                            .filter(it -> needToUnbindList.contains(it.getCheckerSetId())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(needToUnbindRelationEntityList)) {
                checkerSetTaskRelationshipRepository.deleteAll(needToUnbindRelationEntityList);
                for (CheckerSetTaskRelationshipEntity unbindRelationEntity : needToUnbindRelationEntityList) {
                    newCheckerSetVersionMap.remove(unbindRelationEntity.getCheckerSetId());
                }
            }

            //2. 新增语言自动绑定默认规则集
            Set<String> needToBindCheckerSet = new HashSet<>();
            Map<String, CheckerSetProjectRelationshipEntity> projectRelationshipEntityMap =
                    checkerSetProjectRelationshipEntityList.stream().collect(Collectors.toMap(
                            CheckerSetProjectRelationshipEntity::getCheckerSetId, Function.identity(), (k, v) -> v));
            String binaryCodeLang = Long.toBinaryString(codeLang);
            Long originalCodeLang = 1L << (binaryCodeLang.length() - 1);
            for (int i = 0; i < binaryCodeLang.length(); i++) {
                if ((binaryCodeLang.charAt(i) + "").equals("1")) {
                    Long selectedCodeLang = originalCodeLang >> i;
                    if (taskCheckerSetList.stream().allMatch(taskCheckerSetEntity ->
                            (selectedCodeLang & taskCheckerSetEntity.getCodeLang()) == 0L)) {
                        needToBindCheckerSet.addAll(projectCheckerSetList.stream().filter(checkerSet -> {
                            CheckerSetProjectRelationshipEntity projectRelationship =
                                    projectRelationshipEntityMap.get(checkerSet.getCheckerSetId());
                            //条件1, 符合相应语言的
                            return (checkerSet.getCodeLang() & selectedCodeLang) > 0L
                                    //条件2, 默认的
                                    && ((CheckerSetSource.DEFAULT.name().equals(checkerSet.getCheckerSetSource())
                                    && null == projectRelationship)
                                    || (null != projectRelationship
                                    && null != projectRelationship.getDefaultCheckerSet()
                                    && projectRelationship.getDefaultCheckerSet()))
                                    //条件3，非legacy
                                    && !(null != checkerSet.getLegacy() && checkerSet.getLegacy());
                        }).map(CheckerSetEntity::getCheckerSetId).collect(Collectors.toSet()));
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(needToBindCheckerSet)) {
                checkerSetProjectRelationshipRepository.saveAll(
                        needToBindCheckerSet.stream().filter(checkerSetId -> !checkerSetIds.contains(checkerSetId))
                                .map(checkerSetId -> {
                                    CheckerSetProjectRelationshipEntity checkerSetProjectRelationshipEntity =
                                            new CheckerSetProjectRelationshipEntity();
                                    checkerSetProjectRelationshipEntity.setCheckerSetId(checkerSetId);
                                    checkerSetProjectRelationshipEntity.setProjectId(projectId);
                                    checkerSetProjectRelationshipEntity.setDefaultCheckerSet(true);
                                    checkerSetProjectRelationshipEntity.setUselatestVersion(true);
                                    checkerSetProjectRelationshipEntity.setVersion(officialMap.get(checkerSetId));
                                    return checkerSetProjectRelationshipEntity;
                                }).collect(Collectors.toList()));

                checkerSetTaskRelationshipRepository.saveAll(
                        needToBindCheckerSet.stream().filter(StringUtils::isNotBlank)
                                .map(checkerSetId -> {
                                    CheckerSetTaskRelationshipEntity checkerSetTaskRelationshipEntity =
                                            new CheckerSetTaskRelationshipEntity();
                                    checkerSetTaskRelationshipEntity.setTaskId(taskId);
                                    checkerSetTaskRelationshipEntity.setProjectId(projectId);
                                    checkerSetTaskRelationshipEntity.setCheckerSetId(checkerSetId);
                                    return checkerSetTaskRelationshipEntity;
                                }).collect(Collectors.toSet()));
                for (String checkerSetId : needToBindCheckerSet) {
                    if (officialMap.containsKey(checkerSetId)) {
                        newCheckerSetVersionMap.put(checkerSetId, officialMap.get(checkerSetId));
                    }
                }
            }
            // 对各任务设置强制全量扫描标志，并修改告警状态
            Map<Long, Map<String, Integer>> currentTaskUseCheckerSetsMap = Maps.newHashMap();
            currentTaskUseCheckerSetsMap.put(taskId, oldCheckerSetVersionMap);
            Map<Long, Map<String, Integer>> updatedTaskUseCheckerSetsMap = Maps.newHashMap();
            updatedTaskUseCheckerSetsMap.put(taskId, newCheckerSetVersionMap);
            ThreadPoolUtil.addRunnableTask(() -> setForceFullScanAndUpdateDefectAndToolStatus(
                    currentTaskUseCheckerSetsMap,
                    updatedTaskUseCheckerSetsMap, user));
        }
        return true;
    }

    private void setForceFullScanAndUpdateDefectAndToolStatus(
            Map<Long, Map<String, Integer>> currentTaskUseCheckerSetsMap,
            Map<Long, Map<String, Integer>> updatedTaskUseCheckerSetsMap, String user) {
        // 获取所有涉及的规则集Entity
        Set<String> checkerSetIds = Sets.newHashSet();
        for (Map.Entry<Long, Map<String, Integer>> entry : currentTaskUseCheckerSetsMap.entrySet()) {
            checkerSetIds.addAll(entry.getValue().keySet());
        }
        for (Map.Entry<Long, Map<String, Integer>> entry : updatedTaskUseCheckerSetsMap.entrySet()) {
            checkerSetIds.addAll(entry.getValue().keySet());
        }
        Map<String, CheckerSetEntity> checkerSetIdVersionMap = Maps.newHashMap();
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetIdIn(checkerSetIds);
        for (CheckerSetEntity checkerSetEntity : checkerSetEntities) {
            checkerSetIdVersionMap.put(checkerSetEntity.getCheckerSetId() + "_" + checkerSetEntity.getVersion(),
                    checkerSetEntity);
        }

        // 设置强制全量扫描标志并更新告警状态
        for (Map.Entry<Long, Map<String, Integer>> entry : currentTaskUseCheckerSetsMap.entrySet()) {
            List<CheckerSetEntity> fromCheckerSets = Lists.newArrayList();
            List<CheckerSetEntity> toCheckerSets = Lists.newArrayList();
            Map<String, Integer> updatedCheckerSetVersionMap = updatedTaskUseCheckerSetsMap.get(entry.getKey());
            if (MapUtils.isNotEmpty(entry.getValue())) {
                for (Map.Entry<String, Integer> checketSetIdVersionEntry : entry.getValue().entrySet()) {
                    fromCheckerSets.add(checkerSetIdVersionMap.get(checketSetIdVersionEntry.getKey() + "_"
                            + checketSetIdVersionEntry.getValue()));
                }
            }
            if (MapUtils.isNotEmpty(updatedCheckerSetVersionMap)) {
                for (Map.Entry<String, Integer> checketSetIdVersionEntry : updatedCheckerSetVersionMap.entrySet()) {
                    toCheckerSets.add(checkerSetIdVersionMap.get(checketSetIdVersionEntry.getKey() + "_"
                            + checketSetIdVersionEntry.getValue()));
                }
            }
            setForceFullScanAndUpdateDefectAndToolStatus(entry.getKey(), fromCheckerSets, toCheckerSets);
        }

        // 更新工具状态
        for (Map.Entry<Long, Map<String, Integer>> entry : updatedTaskUseCheckerSetsMap.entrySet()) {
            Set<String> updatedToolSet = Sets.newHashSet();
            if (MapUtils.isNotEmpty(entry.getValue())) {
                for (Map.Entry<String, Integer> checkerSetVersionEntry : entry.getValue().entrySet()) {
                    CheckerSetEntity checkerSetEntity = checkerSetIdVersionMap.get(checkerSetVersionEntry.getKey()
                            + "_" + checkerSetVersionEntry.getValue());
                    if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())) {
                        for (CheckerPropsEntity checkerPropsEntity : checkerSetEntity.getCheckerProps()) {
                            if (StringUtils.isNotBlank(checkerPropsEntity.getToolName())
                                    && !ComConstants.Tool.CLOC.name()
                                    .equalsIgnoreCase(checkerPropsEntity.getToolName())) {
                                updatedToolSet.add(checkerPropsEntity.getToolName());
                            }
                        }
                    }
                }
            }

            updateTools(user, entry.getKey(), updatedToolSet);
        }
    }

    private void setForceFullScanAndUpdateDefectAndToolStatus(long taskId, List<CheckerSetEntity> fromCheckerSets,
            List<CheckerSetEntity> toCheckerSets) {
        // 初始化结果对象
        List<CheckerPropsEntity> openDefectCheckerProps = Lists.newArrayList();
        List<CheckerPropsEntity> closeDefectCheckeProps = Lists.newArrayList();
        List<CheckerPropsEntity> updatePropsCheckers = Lists.newArrayList();
        Set<String> toolNames = Sets.newHashSet();

        // 初始化校验用的切换后规则集临时Map
        Map<String, Map<String, CheckerPropsEntity>> toToolCheckersMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(toCheckerSets)) {
            for (CheckerSetEntity checkerSetVO : toCheckerSets) {

                if (CollectionUtils.isNotEmpty(checkerSetVO.getCheckerProps())) {
                    for (CheckerPropsEntity checkerPropVO : checkerSetVO.getCheckerProps()) {
                        toToolCheckersMap.computeIfAbsent(checkerPropVO.getToolName(), k -> Maps.newHashMap());
                        toToolCheckersMap.get(checkerPropVO.getToolName()).put(checkerPropVO.getCheckerKey(),
                                checkerPropVO);
                        toolNames.add(checkerPropVO.getToolName());
                    }
                }
            }
        }

        // 初始化校验用的切换前规则集临时Map，并记录需要关闭和需要更新的规则列表
        Map<String, Map<String, CheckerPropsEntity>> fromToolCheckersMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(fromCheckerSets)) {
            for (CheckerSetEntity checkerSetVO : fromCheckerSets) {

                if (CollectionUtils.isNotEmpty(checkerSetVO.getCheckerProps())) {
                    for (CheckerPropsEntity checkerPropVO : checkerSetVO.getCheckerProps()) {
                        fromToolCheckersMap.computeIfAbsent(checkerPropVO.getToolName(), k -> Maps.newHashMap());
                        fromToolCheckersMap.get(checkerPropVO.getToolName()).put(checkerPropVO.getCheckerKey(),
                                checkerPropVO);

                        if (toToolCheckersMap.get(checkerPropVO.getToolName()) == null
                                || !toToolCheckersMap.get(checkerPropVO.getToolName())
                                .containsKey(checkerPropVO.getCheckerKey())) {
                            closeDefectCheckeProps.add(checkerPropVO);
                        } else {
                            updatePropsCheckers.add(toToolCheckersMap.get(checkerPropVO.getToolName())
                                    .get(checkerPropVO.getCheckerKey()));
                        }
                    }
                }
            }
        }

        // 记录需要打开的规则列表
        if (CollectionUtils.isNotEmpty(toCheckerSets)) {
            for (CheckerSetEntity checkerSetVO : toCheckerSets) {
                if (CollectionUtils.isNotEmpty(checkerSetVO.getCheckerProps())) {
                    for (CheckerPropsEntity checkerPropVO : checkerSetVO.getCheckerProps()) {
                        if (fromToolCheckersMap.get(checkerPropVO.getToolName()) == null
                                || !fromToolCheckersMap.get(checkerPropVO.getToolName())
                                .containsKey(checkerPropVO.getCheckerKey())) {
                            openDefectCheckerProps.add(checkerPropVO);
                        }
                    }
                }
            }
        }

        // 设置强制全量扫描
        toolBuildInfoService.setForceFullScan(taskId, Lists.newArrayList(toolNames));

        // 刷新告警状态
        Map<String, ConfigCheckersPkgReqVO> toolDefectRefreshConfigMap = Maps.newHashMap();
        for (CheckerPropsEntity checkerPropsEntity : openDefectCheckerProps) {
            ConfigCheckersPkgReqVO configCheckersPkgReq = getConfigCheckersReqVO(taskId,
                    checkerPropsEntity.getToolName(), toolDefectRefreshConfigMap);
            configCheckersPkgReq.getOpenedCheckers().add(checkerPropsEntity.getCheckerKey());
        }
        for (CheckerPropsEntity checkerPropsEntity : closeDefectCheckeProps) {
            ConfigCheckersPkgReqVO configCheckersPkgReq = getConfigCheckersReqVO(taskId,
                    checkerPropsEntity.getToolName(), toolDefectRefreshConfigMap);
            configCheckersPkgReq.getClosedCheckers().add(checkerPropsEntity.getCheckerKey());
        }
        for (Map.Entry<String, ConfigCheckersPkgReqVO> entry : toolDefectRefreshConfigMap.entrySet()) {
            rabbitTemplate.convertAndSend(EXCHANGE_TASK_CHECKER_CONFIG, ROUTE_IGNORE_CHECKER, entry.getValue());
        }
    }

    private void updateTools(String user, long taskId, Set<String> updatedToolSet) {
        BatchRegisterVO batchRegisterVO = new BatchRegisterVO();
        batchRegisterVO.setTaskId(taskId);
        List<ToolConfigInfoVO> toolConfigInfoVOS = Lists.newArrayList();
        for (String toolName : updatedToolSet) {
            ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
            toolConfigInfoVO.setTaskId(taskId);
            toolConfigInfoVO.setToolName(toolName);
            toolConfigInfoVOS.add(toolConfigInfoVO);
        }
        batchRegisterVO.setTools(toolConfigInfoVOS);
        client.get(ServiceToolRestResource.class).updateTools(taskId, user, batchRegisterVO);
    }

    private ConfigCheckersPkgReqVO getConfigCheckersReqVO(long taskId, String toolName, Map<String,
            ConfigCheckersPkgReqVO> toolDefectRefreshConfigMap) {
        ConfigCheckersPkgReqVO configCheckersPkgReq = toolDefectRefreshConfigMap.get(toolName);
        if (configCheckersPkgReq == null) {
            configCheckersPkgReq = new ConfigCheckersPkgReqVO();
            configCheckersPkgReq.setTaskId(taskId);
            configCheckersPkgReq.setToolName(toolName);
            configCheckersPkgReq.setOpenedCheckers(Lists.newArrayList());
            configCheckersPkgReq.setClosedCheckers(Lists.newArrayList());
            toolDefectRefreshConfigMap.put(toolName, configCheckersPkgReq);
        }
        return configCheckersPkgReq;
    }

    /**
     * 校验规则集是否重复
     *
     * @param checkerSetId
     */
    private void checkIdDuplicate(String checkerSetId) {
        boolean checkerSetIdDuplicate = false;
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
        for (CheckerSetEntity checkerSets : checkerSetEntities) {
            if (checkerSetIdDuplicate) {
                break;
            }
            if (checkerSets.getCheckerSetId().equals(checkerSetId)) {
                checkerSetIdDuplicate = true;
            }
        }
        StringBuffer errMsg = new StringBuffer();
        if (checkerSetIdDuplicate) {
            errMsg.append("规则集ID[").append(checkerSetId).append("]");
        }
        if (errMsg.length() > 0) {
            String errMsgStr = errMsg.toString();
            log.error("{}已存在", errMsgStr);
            throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{errMsgStr}, null);
        }
    }

    /**
     * 校验规则集名称是否与公开规则集重复
     *
     * @param checkerSetName
     */
    private void checkNameExistInPublic(String checkerSetName) {
        boolean checkerSetNameDuplicate = false;
        List<CheckerSetEntity> checkerSetEntities =
                checkerSetRepository.findByScope(CheckerConstants.CheckerSetScope.PUBLIC.code());
        for (CheckerSetEntity checkerSets : checkerSetEntities) {
            if (checkerSetNameDuplicate) {
                break;
            }
            if (checkerSets.getCheckerSetName().equals(checkerSetName)) {
                checkerSetNameDuplicate = true;
            }
        }
        StringBuffer errMsg = new StringBuffer();
        if (checkerSetNameDuplicate) {
            errMsg.append("规则集名称[").append(checkerSetName).append("]");
        }
        if (errMsg.length() > 0) {
            String errMsgStr = errMsg.toString();
            log.error("{}已存在", errMsgStr);
            throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{errMsgStr}, null);
        }
    }

    /**
     * 校验规则集名称是否与项目规则集重复
     *
     * @param checkerSetName
     * @param projectId
     */
    private void checkNameExistInProject(String checkerSetName, String projectId) {
        boolean checkerSetNameDuplicate = false;
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByProjectId(projectId);
        for (CheckerSetEntity checkerSets : checkerSetEntities) {
            if (checkerSetNameDuplicate) {
                break;
            }
            if (checkerSets.getCheckerSetName().equals(checkerSetName)) {
                checkerSetNameDuplicate = true;
            }
        }
        StringBuffer errMsg = new StringBuffer();
        if (checkerSetNameDuplicate) {
            errMsg.append("规则集名称[").append(checkerSetName).append("]");
        }
        if (errMsg.length() > 0) {
            String errMsgStr = errMsg.toString();
            log.error("{}已存在", errMsgStr);
            throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{errMsgStr}, null);
        }
    }

    private Map<String, Integer> getLatestVersionMap(Set<String> checkerSetIds) {
        Map<String, Integer> latestVersionMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(checkerSetIds)) {
            List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetIdIn(checkerSetIds);
            for (CheckerSetEntity checkerSetEntity : checkerSetEntities) {
                if (latestVersionMap.get(checkerSetEntity.getCheckerSetId()) == null
                        || checkerSetEntity.getVersion() > latestVersionMap.get(checkerSetEntity.getCheckerSetId())) {
                    latestVersionMap.put(checkerSetEntity.getCheckerSetId(), checkerSetEntity.getVersion());
                }
            }
        }
        return latestVersionMap;
    }

    private List<CheckerSetCatagoryEntity> getCatagoryEntities(List<String> catatories) {
        if (CollectionUtils.isEmpty(catatories)) {
            return Lists.newArrayList();
        }

        /*
         * 1、国际化之后，修改规则集时put上来的报文可能是中文或英文，都需要做映射
         * 2、历史原因，Entity中enName统一存枚举的name()，以便其他逻辑做统计
         */
        Map<String, CheckerSetCategoryModel> catagoryNameMap = Maps.newHashMap();
        for (CheckerSetCategory enumObj : CheckerSetCategory.values()) {
            String enumName = enumObj.name();
            String resourceCode = enumObj.getI18nResourceCode();
            String enName = I18NUtils.getMessage(resourceCode, I18NUtils.EN);
            String cnName = I18NUtils.getMessage(resourceCode, I18NUtils.CN);
            CheckerSetCategoryModel checkerSetCategoryModel = new CheckerSetCategoryModel(enName, cnName, enumName);
            catagoryNameMap.put(enName, checkerSetCategoryModel);
            catagoryNameMap.put(cnName, checkerSetCategoryModel);
        }

        List<CheckerSetCatagoryEntity> retList = Lists.newArrayList();
        for (String categoryName : catatories) {
            CheckerSetCategoryModel model = catagoryNameMap.get(categoryName);
            if (model == null) {
                log.info("checker set category name invalid: {}", categoryName);
                continue;
            }

            retList.add(new CheckerSetCatagoryEntity(model.getEnumName(), model.getCnName()));
        }

        return retList;
    }

    /**
     * 更新规则集信息
     *
     * @param userName 更新人
     * @param updateCheckerSetReqExtVO 基本信息
     * @return boolean
     */
    @Override
    public Boolean updateCheckerSetBaseInfoByOp(String userName,
            @NotNull V3UpdateCheckerSetReqExtVO updateCheckerSetReqExtVO) {
        boolean result = false;
        CheckerSetEntity checkerSetEntity = checkerSetRepository
                .findFirstByCheckerSetIdAndVersion(updateCheckerSetReqExtVO.getCheckerSetId(),
                        updateCheckerSetReqExtVO.getVersion());
        if (null != checkerSetEntity) {
            checkerSetEntity.setCheckerSetName(updateCheckerSetReqExtVO.getCheckerSetName());
            checkerSetEntity.setCatagories(getCatagoryEntities(updateCheckerSetReqExtVO.getCatagories()));
            checkerSetEntity.setCheckerSetSource(updateCheckerSetReqExtVO.getCheckerSetSource());
            checkerSetEntity.setDescription(updateCheckerSetReqExtVO.getDescription());
            checkerSetEntity.setUpdatedBy(userName);
            // 判断发布者是否为空，如果为空则不修改发布者
            if (StringUtils.isNotEmpty(updateCheckerSetReqExtVO.getCreator())) {
                checkerSetEntity.setCreator(updateCheckerSetReqExtVO.getCreator());
            }
            checkerSetEntity.setLastUpdateTime(System.currentTimeMillis());
            checkerSetRepository.save(checkerSetEntity);
            result = true;
        }

        return result;
    }
}
