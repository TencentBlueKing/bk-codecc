package com.tencent.bk.codecc.defect.service.impl;

import static com.tencent.devops.common.auth.api.pojo.external.AuthExConstantsKt.KEY_CREATE_FROM;
import static com.tencent.devops.common.auth.api.pojo.external.AuthExConstantsKt.PREFIX_TASK_INFO;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CODECC_GENERAL_NOTIFY;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_IGNORE_EMAIL_SEND;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CODECC_RTX_NOTIFY_SEND;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_IGNORE_EMAIL_SEND;

import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.component.ScheduleJobComponent;
import com.tencent.bk.codecc.defect.constant.DefectMessageCode;
import com.tencent.bk.codecc.defect.dao.mongorepository.IgnoreTypeProjectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.IgnoreTypeSysRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.DefectDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.IgnoreTypeProjectDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.dto.IgnoreTypeEmailDTO;
import com.tencent.bk.codecc.defect.dto.IgnoreTypeStatModel;
import com.tencent.bk.codecc.defect.model.ignore.IgnoreTypeNotifyEntity;
import com.tencent.bk.codecc.defect.model.ignore.IgnoreTypeProjectConfig;
import com.tencent.bk.codecc.defect.model.ignore.IgnoreTypeSysEntity;
import com.tencent.bk.codecc.defect.service.IIgnoreTypeService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreStatDetail;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeDefectStatResponse;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeNotifyVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeProjectConfigVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeReportDetailVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeSysVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.CodeLibraryInfoVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.TaskCodeLibraryVO;
import com.tencent.bk.codecc.task.vo.ToolConfigBaseVO;
import com.tencent.devops.common.api.RtxNotifyVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.IgnoreTypeNotifyReceiverType;
import com.tencent.devops.common.constant.ComConstants.NotifyTypeCreateFrom;
import com.tencent.devops.common.constant.ComConstants.ToolType;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.notify.enums.WeworkReceiverType;
import com.tencent.devops.common.notify.enums.WeworkTextType;
import com.tencent.devops.common.redis.lock.RedisLock;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.util.CronBuilder;
import com.tencent.devops.common.util.NotifyUtils;
import com.tencent.devops.common.util.ThreadPoolUtil;
import com.tencent.devops.common.web.validate.ValidateProject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

/**
 * 告警忽略类型接口实现
 */
@Slf4j
@Service
public class IgnoreTypeServiceImpl implements IIgnoreTypeService {

    /**
     * 起始ID号
     */
    private static final Integer START_ID = 10;
    private static final String JOB_NAME_PREFIX = "IGNORE_NOTIFY_JOB_";
    private static final String JOB_SCHEDULE_CLASS_NAME = "IgnoreTypeNotifyTask";
    private static final Integer RTX_MAX_SHOW_TASK_COUNT = 20;
    private static final Integer EMAIL_MAX_SHOW_COUNT = 15;
    private static final int IGNORE_STATUS =
            ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.IGNORE.value();
    /**
     * 默认10点进行通知
     */
    private static final Integer IGNORE_NOTIFY_HOUR = 10;
    @Autowired
    private IgnoreTypeSysRepository ignoreTypeSysRepository;
    @Autowired
    private IgnoreTypeProjectRepository ignoreTypeProjectRepository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private ScheduleJobComponent scheduleJobComponent;
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;
    @Autowired
    private DefectDao defectDao;
    @Autowired
    private CCNDefectDao ccnDefectDao;
    @Autowired
    private Client client;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ToolMetaCacheService toolMetaCacheService;
    @Autowired
    private IgnoreTypeProjectDao ignoreTypeProjectDao;
    @Autowired
    private AuthExPermissionApi authExPermissionApi;
    @Value("${bkci.public.url:#{null}}")
    private String devopsHost;
    @Value("${codecc.public.url:#{null}}")
    private String codeccHost;
    private LoadingCache<String, List<IgnoreTypeSysEntity>> sysIgnoreTypeCache = CacheBuilder.newBuilder()
            .maximumSize(1)
            .refreshAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<String, List<IgnoreTypeSysEntity>>() {
                @Override
                public List<IgnoreTypeSysEntity> load(String name) {
                    return ignoreTypeSysRepository.findAll();
                }
            });


    private IgnoreTypeSysEntity getIgnoreTypeSysFromCache(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        List<IgnoreTypeSysEntity> sysEntities = sysIgnoreTypeCache.getUnchecked("SYS_IGNORE_TYPE");
        if (CollectionUtils.isEmpty(sysEntities)) {
            return null;
        }
        for (IgnoreTypeSysEntity sysEntity : sysEntities) {
            if (sysEntity.getName().equals(name)) {
                return sysEntity;
            }
        }
        return null;
    }

    /**
     * 新增/修改系统默认告警忽略类型
     */
    @Override
    public Boolean ignoreTypeSysUpdate(String userName, IgnoreTypeSysVO reqVO) {
        log.info("ignoreTypeSysUpdate reqVO: {}", reqVO);
        try {
            String ignoreTypeName = reqVO.getName();
            Integer ignoreTypeId = reqVO.getIgnoreTypeId();
            if (StringUtils.isBlank(ignoreTypeName)) {
                log.warn("ignoreTypeName is blank! update failed.");
                return false;
            }
            IgnoreTypeSysEntity ignoreTypeSysEntity = ignoreTypeSysRepository.findFirstByName(ignoreTypeName);
            if (null == ignoreTypeSysEntity) {
                boolean isNewAdd = false;

                // 如果用名称查询不到,则用ignoreTypeId来查
                if (null != ignoreTypeId) {
                    ignoreTypeSysEntity = ignoreTypeSysRepository.findFirstByIgnoreTypeId(ignoreTypeId);
                }
                if (null == ignoreTypeSysEntity) {
                    isNewAdd = true;
                    ignoreTypeSysEntity = new IgnoreTypeSysEntity();
                    // 自动生成id
                    ignoreTypeSysEntity.setIgnoreTypeId(generateId());
                    ignoreTypeSysEntity.applyAuditInfo(userName, userName);
                } else {
                    ignoreTypeSysEntity.applyAuditInfo(userName);
                }
                String ignoreTypeNameOld = ignoreTypeSysEntity.getName();
                ignoreTypeSysEntity.setName(ignoreTypeName);

                if (!isNewAdd && !ignoreTypeName.equals(ignoreTypeNameOld)) {
                    // 异步更新project表所有该名称为新名称
                    ThreadPoolUtil.addRunnableTask(() -> ignoreTypeProjectDao
                            .updateIgnoreTypeNameById(ignoreTypeId, ignoreTypeNameOld, ignoreTypeName));
                }
            } else {
                ignoreTypeSysEntity.applyAuditInfo(userName);
            }

            // 默认0，启用
            ignoreTypeSysEntity.setStatus(reqVO.getStatus() == null ? Integer.valueOf(0) : reqVO.getStatus());

            IgnoreTypeSysEntity saved = ignoreTypeSysRepository.save(ignoreTypeSysEntity);
            log.info("ignoreTypeSysUpdate success: {}", saved);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取所有系统默认的告警忽略类型
     */
    @Override
    public List<IgnoreTypeSysVO> queryIgnoreTypeSysList() {
        List<IgnoreTypeSysEntity> ignoreTypeSysEntityList = ignoreTypeSysRepository.findAll();
        return ignoreTypeSysEntityList.stream().map(ignoreTypeSysEntity -> {
            IgnoreTypeSysVO ignoreTypeSysVO = new IgnoreTypeSysVO();
            BeanUtils.copyProperties(ignoreTypeSysEntity, ignoreTypeSysVO);
            return ignoreTypeSysVO;
        }).collect(Collectors.toList());
    }

    private IgnoreTypeSysEntity getIgnoreTypeSysByName(String name) {
        return ignoreTypeSysRepository.findFirstByName(name);
    }

    @Override
    @ValidateProject
    public Boolean ignoreTypeProjectSave(String projectId, String userName, IgnoreTypeProjectConfigVO projectConfigVo) {
        if (StringUtils.isBlank(projectId) || projectConfigVo == null
                || StringUtils.isBlank(projectConfigVo.getName())) {
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"projectId and name"});
        }
        if (!authExPermissionApi.authProjectMultiManager(projectId, userName)) {
            throw new CodeCCException(CommonMessageCode.PERMISSION_DENIED, new String[]{userName});
        }
        projectConfigVo.setProjectId(projectId);
        log.info("begin save project:{},ignoreType:{} userName:{}", projectId, projectConfigVo.getName(), userName);
        //校验名称重复
        IgnoreTypeProjectConfig projectConfig =
                ignoreTypeProjectRepository.findFirstByProjectIdAndName(projectConfigVo.getProjectId(),
                        projectConfigVo.getName());
        if (checkIfNameRepeat(projectConfigVo, projectConfig)) {
            throw new CodeCCException(CommonMessageCode.KEY_IS_EXIST, new String[]{projectConfigVo.getName()});
        }
        IgnoreTypeNotifyEntity oldNotifyConfig = projectConfig == null ? null : projectConfig.getNotify();
        // 设置属性值
        if (projectConfig == null) {
            projectConfig = newProjectConfig(projectId, userName, projectConfigVo);
        } else {
            projectConfig.setName(projectConfigVo.getName());
            projectConfig.setNotify(new IgnoreTypeNotifyEntity());
        }
        BeanUtils.copyProperties(projectConfigVo.getNotify() == null ? new IgnoreTypeNotifyVO()
                : projectConfigVo.getNotify(), projectConfig.getNotify());
        projectConfig.setCreateFrom(getIgnoreTypeSysFromCache(projectConfigVo.getName()) == null
                ? ComConstants.NotifyTypeCreateFrom.PROJECT.getType()
                : ComConstants.NotifyTypeCreateFrom.SYS.getType());
        projectConfig.setStatus(ComConstants.Status.ENABLE.value());
        projectConfig.setUpdatedDate(System.currentTimeMillis());
        projectConfig.setUpdatedBy(userName);
        ignoreTypeProjectRepository.save(projectConfig);
        //通知相关
        processNotifySetting(projectConfig, oldNotifyConfig);
        log.info("end save project:{},ignoreType:{} userName:{}", projectId, projectConfigVo.getName(), userName);
        return true;
    }

    /**
     * 检查名称是否重复
     *
     * @param projectConfigVo
     * @param projectConfig
     * @return
     */
    private Boolean checkIfNameRepeat(IgnoreTypeProjectConfigVO projectConfigVo,
            IgnoreTypeProjectConfig projectConfig) {
        if (projectConfig == null) {
            return false;
        }
        if (StringUtils.isBlank(projectConfigVo.getEntityId())
                && projectConfig.getStatus() == ComConstants.Status.DISABLE.value()) {
            return false;
        }
        return StringUtils.isBlank(projectConfigVo.getEntityId())
                || !projectConfig.getEntityId().equals(projectConfigVo.getEntityId());
    }

    private IgnoreTypeProjectConfig newProjectConfig(String projectId, String userName,
            IgnoreTypeProjectConfigVO projectConfigVo) {
        IgnoreTypeProjectConfig projectConfig = new IgnoreTypeProjectConfig();
        BeanUtils.copyProperties(projectConfigVo, projectConfig);
        projectConfig.setProjectId(projectId);
        IgnoreTypeSysEntity sysEntity = getIgnoreTypeSysFromCache(projectConfigVo.getName());
        projectConfig.setIgnoreTypeId(sysEntity == null ? generateId() : sysEntity.getIgnoreTypeId());
        projectConfig.setStatus(ComConstants.Status.ENABLE.value());
        projectConfig.setNotify(new IgnoreTypeNotifyEntity());
        projectConfig.setCreatedDate(System.currentTimeMillis());
        projectConfig.setCreatedBy(userName);
        return projectConfig;
    }


    private void processNotifySetting(IgnoreTypeProjectConfig config, IgnoreTypeNotifyEntity oldNotifyConfig) {
        String jobName = getNotifyJobName(config.getProjectId(), config.getIgnoreTypeId());
        String cronExpression = CronBuilder.builder().hours(IGNORE_NOTIFY_HOUR).build();
        Map<String, Object> jobParams = getJobParams(config);
        IgnoreTypeNotifyEntity notifyEntity = config.getNotify();
        //新增
        if (notifyEntity != null && notifyEntity.hasNotifyConfig()
                && (oldNotifyConfig == null || !oldNotifyConfig.hasNotifyConfig())) {
            scheduleJobComponent.addJob(jobName, JOB_SCHEDULE_CLASS_NAME, cronExpression, jobParams);
            log.info("projectId:{},name:{},IgnoreTypeId:{} job has add! cron:{}", config.getProjectId(),
                    config.getName(), config.getIgnoreTypeId(), cronExpression);
        } else if ((notifyEntity == null || !notifyEntity.hasNotifyConfig()) && oldNotifyConfig != null
                && oldNotifyConfig.hasNotifyConfig()) {
            //去除配置，不进行通知了
            scheduleJobComponent.removeJob(jobName, JOB_SCHEDULE_CLASS_NAME, cronExpression, jobParams);
            log.info("projectId:{},name:{},IgnoreTypeId:{} job has remove! cron:{}", config.getProjectId(),
                    config.getName(), config.getIgnoreTypeId(), cronExpression);
        }
        log.info("projectId:{},name:{},IgnoreTypeId:{} job not change", config.getProjectId(), config.getName(),
                config.getIgnoreTypeId());
    }

    private Map<String, Object> getJobParams(IgnoreTypeProjectConfig config) {
        Map<String, Object> jobParams = new HashMap<>();
        jobParams.put("name", config.getName());
        jobParams.put("projectId", config.getProjectId());
        jobParams.put("ignoreTypeId", config.getIgnoreTypeId());
        jobParams.put("createFrom", config.getCreateFrom());
        return jobParams;
    }

    private String getNotifyJobName(String projectId, Integer ignoreTypeId) {
        return JOB_NAME_PREFIX + projectId + "_" + ignoreTypeId;
    }

    /**
     * 获取列表
     *
     * @param projectId
     * @param userName
     * @return
     */
    @Override
    public List<IgnoreTypeProjectConfigVO> queryIgnoreTypeProjectList(String projectId, String userName) {
        //获取系统列表
        List<IgnoreTypeSysEntity> ignoreTypeSysEntities =
                ignoreTypeSysRepository.findByStatusOrderByIgnoreTypeId(ComConstants.Status.ENABLE.value());
        boolean projectManager = authExPermissionApi.authProjectMultiManager(projectId, userName);
        //获取项目配置
        List<IgnoreTypeProjectConfig> ignoreTypeProjectConfigs = ignoreTypeProjectRepository
                .findByProjectIdAndStatusOrderByIgnoreTypeId(projectId, ComConstants.Status.ENABLE.value());
        Map<String, IgnoreTypeProjectConfig> nameMapping = null;
        if (ignoreTypeProjectConfigs != null) {
            nameMapping = ignoreTypeProjectConfigs.stream()
                    .collect(Collectors.toMap(IgnoreTypeProjectConfig::getName, it -> it));
        }
        List<IgnoreTypeProjectConfigVO> vos = new LinkedList<>();
        if (CollectionUtils.isNotEmpty(ignoreTypeSysEntities)) {
            for (IgnoreTypeSysEntity entity : ignoreTypeSysEntities) {
                IgnoreTypeProjectConfigVO vo = new IgnoreTypeProjectConfigVO();
                vo.setNotify(IgnoreTypeNotifyVO.newIgnoreTypeNotifyVO(true));
                if (nameMapping != null && nameMapping.containsKey(entity.getName())) {
                    vo = convertModelToVo(nameMapping.get(entity.getName()));
                    nameMapping.remove(entity.getName());
                } else {
                    BeanUtils.copyProperties(entity, vo);
                }
                vo.setEdit(projectManager);
                vo.setCreateFrom(ComConstants.NotifyTypeCreateFrom.SYS.getType());
                vos.add(vo);
            }
        }
        if (nameMapping != null && CollectionUtils.isNotEmpty(nameMapping.values())) {
            for (IgnoreTypeProjectConfig entity : ignoreTypeProjectConfigs) {
                if (!nameMapping.containsKey(entity.getName())) {
                    continue;
                }
                IgnoreTypeProjectConfigVO configVO = convertModelToVo(entity);
                // 转换为Project
                if (configVO.getCreateFrom().equals(ComConstants.NotifyTypeCreateFrom.SYS.getType())) {
                    configVO.setCreateFrom(NotifyTypeCreateFrom.PROJECT.getType());
                }
                configVO.setEdit(projectManager);
                vos.add(configVO);
            }
        }
        return vos;
    }

    @Override
    public IgnoreTypeProjectConfigVO ignoreTypeProjectDetail(String projectId, String userName, Integer ignoreTypeId) {

        //先查询项目配置
        IgnoreTypeProjectConfig projectConfig =
                ignoreTypeProjectRepository.findFirstByProjectIdAndIgnoreTypeId(projectId, ignoreTypeId);
        if (projectConfig != null) {
            return convertModelToVo(projectConfig);
        }
        IgnoreTypeSysEntity sysEntity = ignoreTypeSysRepository.findFirstByIgnoreTypeId(ignoreTypeId);
        if (sysEntity != null) {
            IgnoreTypeProjectConfigVO projectConfigVO = new IgnoreTypeProjectConfigVO();
            projectConfigVO.setProjectId(projectId);
            projectConfigVO.setIgnoreTypeId(sysEntity.getIgnoreTypeId());
            projectConfigVO.setName(sysEntity.getName());
            projectConfigVO.setNotify(new IgnoreTypeNotifyVO());
            projectConfigVO.setStatus(ComConstants.Status.ENABLE.value());
            return projectConfigVO;
        }
        throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"projectId", "ignoreTypeId"});
    }

    /**
     * 更新状态 - 删除与恢复
     *
     * @param projectId
     * @param userName
     * @param projectConfigVo
     * @return
     */
    @Override
    public Boolean updateIgnoreTypeProjectStatus(String projectId, String userName,
            IgnoreTypeProjectConfigVO projectConfigVo) {
        if (StringUtils.isBlank(projectId) || projectConfigVo == null
                || StringUtils.isBlank(projectConfigVo.getEntityId())) {
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{"projectId and entityId"});
        }
        if (!authExPermissionApi.authProjectMultiManager(projectId, userName)) {
            throw new CodeCCException(CommonMessageCode.PERMISSION_DENIED, new String[]{userName});
        }
        IgnoreTypeProjectConfig projectConfig =
                ignoreTypeProjectRepository.findFirstByEntityId(projectConfigVo.getEntityId());
        if (projectConfig == null) {
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"entityId"});
        }
        //校验是否还有告警使用该忽略类型
        List<IgnoreTypeDefectStatResponse> statResponses = getIgnoreTypeDefectStat(projectId, userName,
                Sets.newHashSet(projectConfig.getIgnoreTypeId()));
        if (CollectionUtils.isNotEmpty(statResponses) && statResponses.get(0) != null) {
            IgnoreTypeDefectStatResponse response = statResponses.get(0);
            if ((response.getDefect() != null && response.getDefect() > 0)
                    || (response.getRiskFunction() != null && response.getRiskFunction() > 0)) {
                throw  new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                        "当前忽略类型问题或者风险函数数量大于0, 无法被删除");
            }
        }

        projectConfig.setStatus(projectConfigVo.getStatus());
        projectConfig.setUpdatedDate(System.currentTimeMillis());
        projectConfig.setUpdatedBy(userName);
        ignoreTypeProjectRepository.save(projectConfig);

        Integer preStatus = projectConfig.getStatus();
        if (preStatus.equals(projectConfig.getStatus())) {
            return true;
        }
        String jobName = getNotifyJobName(projectConfig.getProjectId(), projectConfig.getIgnoreTypeId());
        String cronExpression = CronBuilder.builder().hours(IGNORE_NOTIFY_HOUR).build();
        Map<String, Object> jobParams = getJobParams(projectConfig);
        if (projectConfigVo.getStatus() == ComConstants.Status.DISABLE.value()) {
            //去除配置，不进行通知了
            scheduleJobComponent.removeJob(jobName, JOB_SCHEDULE_CLASS_NAME, cronExpression, jobParams);
            log.info("projectId:{},name:{},IgnoreTypeId:{} job has remove! cron:{}", projectConfig.getProjectId(),
                    projectConfig.getName(), projectConfig.getIgnoreTypeId(), cronExpression);
        } else if (projectConfigVo.getStatus() == ComConstants.Status.ENABLE.value()) {
            scheduleJobComponent.addJob(jobName, JOB_SCHEDULE_CLASS_NAME, cronExpression, jobParams);
            log.info("projectId:{},name:{},IgnoreTypeId:{} job has add! cron:{}", projectConfig.getProjectId(),
                    projectConfig.getName(), projectConfig.getIgnoreTypeId(), cronExpression);
        }
        return true;
    }


    private IgnoreTypeProjectConfigVO convertModelToVo(IgnoreTypeProjectConfig projectConfig) {
        IgnoreTypeProjectConfigVO vo = new IgnoreTypeProjectConfigVO();
        vo.setNotify(new IgnoreTypeNotifyVO());
        BeanUtils.copyProperties(projectConfig, vo);
        if (projectConfig.getNotify() != null) {
            BeanUtils.copyProperties(projectConfig.getNotify(), vo.getNotify());
            vo.setNextNotifyTime(getNextExecuteDate(projectConfig.getNotify()));
        }
        return vo;
    }

    /**
     * 计算下次执行的日期
     *
     * @param notifyEntity
     * @return
     */
    private Long getNextExecuteDate(IgnoreTypeNotifyEntity notifyEntity) {
        if (notifyEntity == null || !notifyEntity.hasNotifyConfig()) {
            return null;
        }

        if ((notifyEntity.getEveryWeek() != null && notifyEntity.getEveryWeek())
                || CollectionUtils.isEmpty(notifyEntity.getNotifyWeekOfMonths())
                || notifyEntity.getNotifyWeekOfMonths().size() == 5) {
            //每周
            CronBuilder builder = getCronBuilderWithMonthAndHour(notifyEntity);
            builder.dayOfWeek(notifyEntity.getNotifyDayOfWeeks().toArray(new Integer[]{}));
            CronExpression expression = CronExpression.parse(builder.build());
            return expression.next(LocalDateTime.now()).atZone(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();
        }
        // 计算最近的时间
        Long min = null;
        for (Integer notifyWeekOfMonth : notifyEntity.getNotifyWeekOfMonths()) {
            for (Integer notifyDayOfWeek : notifyEntity.getNotifyDayOfWeeks()) {
                CronBuilder builder = getCronBuilderWithMonthAndHour(notifyEntity);
                builder.dayOfWeek(notifyDayOfWeek, notifyWeekOfMonth, false);
                long time = CronExpression.parse(builder.build()).next(LocalDateTime.now())
                        .atZone(ZoneId.systemDefault())
                        .toInstant().toEpochMilli();
                if (min == null || min > time) {
                    min = time;
                }
            }
        }
        return min;
    }

    private CronBuilder getCronBuilderWithMonthAndHour(IgnoreTypeNotifyEntity notifyEntity) {
        CronBuilder builder = CronBuilder.builder();
        //设置今日时间
        builder.hours(IGNORE_NOTIFY_HOUR);
        if ((notifyEntity.getEveryMonth() != null && notifyEntity.getEveryMonth())
                || CollectionUtils.isEmpty(notifyEntity.getNotifyMonths())
                || notifyEntity.getNotifyMonths().size() == 12) {
            //每月
            builder.month();
        } else {
            //指定月份
            builder.month(notifyEntity.getNotifyMonths().toArray(new Integer[]{}));
        }
        return builder;
    }

    /**
     * 生成IgnoreTypeId，频率低，使用Redis与锁进行生成
     *
     * @return
     */
    private Integer generateId() {
        Long index = 0L;
        try (RedisLock redisLock = new RedisLock(redisTemplate, "IGNORE_TYPE_INDEX_LOCK",
                TimeUnit.MINUTES.toSeconds(1))) {
            redisLock.lock(5000L);
            String indexKey = "IGNORE_TYPE_INDEX_VALUE";
            //判断是否存在
            if (redisTemplate.opsForValue().get(indexKey) == null) {
                //设置初始值
                redisTemplate.opsForValue().set(indexKey, String.valueOf(getInitIgnoreIndex()));
            }
            index = redisTemplate.opsForValue().increment(indexKey);
        } catch (Exception e) {
            log.error("generateId CodeCC Ignore Type Id Error.", e);
        }

        if (index == null) {
            throw new CodeCCException(DefectMessageCode.IGNORE_GENERATE_ID_FAIL);
        }

        return index.intValue();
    }

    private Integer getInitIgnoreIndex() {
        IgnoreTypeSysEntity lastSysEntity = ignoreTypeSysRepository.findFirstByOrderByIgnoreTypeIdDesc();
        IgnoreTypeProjectConfig lastProjectEntity = ignoreTypeProjectRepository.findFirstByOrderByIgnoreTypeIdDesc();
        Integer index = START_ID;
        if (lastSysEntity != null && lastSysEntity.getIgnoreTypeId() > index) {
            index = lastSysEntity.getIgnoreTypeId();
        }
        if (lastProjectEntity != null && lastProjectEntity.getIgnoreTypeId() > index) {
            index = lastProjectEntity.getIgnoreTypeId();
        }
        return index;
    }

    /**
     * 检查入参查询的数据
     *
     * @param ignoreTypeProjectConfig entity
     * @return boolean
     */
    private boolean checkConfigIsValid(IgnoreTypeProjectConfig ignoreTypeProjectConfig) {
        if (ignoreTypeProjectConfig == null) {
            log.warn("ignoreTypeProjectConfig is not found! abort and exit.");
            return false;
        }

        if (ignoreTypeProjectConfig.getStatus() != ComConstants.Status.ENABLE.value()) {
            log.warn("project:{}, ignoreTypeName:{} is disable", ignoreTypeProjectConfig.getProjectId(),
                    ignoreTypeProjectConfig.getName());
            return false;
        }

        IgnoreTypeNotifyEntity notifyEntity = ignoreTypeProjectConfig.getNotify();
        if (null == notifyEntity || (CollectionUtils.isEmpty(notifyEntity.getNotifyReceiverTypes())
                && CollectionUtils.isEmpty(notifyEntity.getExtReceiver()))
                || CollectionUtils.isEmpty(notifyEntity.getNotifyTypes())) {
            log.warn("notify config is invalid!");
            return false;
        }
        // 检查通过
        return true;
    }

    /**
     * eg:
     *
     * 忽略类型：历史代码bkcheck空指针
     * 忽略数量（共3个任务，含81个问题、10个风险函数）
     * MR扫描(hongzheli/semgrep-test@master)   38个问题   0个风险函数
     * 每日定时构建(carryhdwang/semgrep-test@master)    33个问题   5个风险函数
     * 转测前深度检查(carryhdwang/semgrep-test@master)    10个问题    5个风险函数
     *
     * @return str content
     */
    private String generateNotifyContent(@NotNull IgnoreTypeReportDetailVO detailVO) {
        StringBuilder strBuilder = new StringBuilder();
        // 标题
        strBuilder.append("【CodeCC】").append(detailVO.getProjectName()).append("忽略问题Review\n");

        String queryParam = String.format("status=%d&ignoreTypeId=%s", ComConstants.DefectStatus.IGNORE.value(),
                detailVO.getIgnoreTypeId());

        strBuilder.append("忽略类型：").append(detailVO.getIgnoreTypeName()).append("\n");
        strBuilder.append("忽略数量（共").append(detailVO.getTaskIgnoreSum()).append("个任务，含[")
                .append(detailVO.getDefectIgnoreSum()).append("个问题](")
                .append(String.format("http://%s/console/codecc/%s/defect/list", devopsHost, detailVO.getProjectId()))
                .append("?").append(queryParam).append(")、[")
                .append(detailVO.getCcnIgnoreSum()).append("个风险函数](")
                .append(String.format("http://%s/console/codecc/%s/ccn/list", devopsHost, detailVO.getProjectId()))
                .append("?").append(queryParam).append(")）").append("\n");

        List<IgnoreStatDetail> ignoreTaskList = detailVO.getTaskList();
        if (CollectionUtils.isEmpty(ignoreTaskList)) {
            return strBuilder.toString();
        }

        int size = ignoreTaskList.size();
        if (size > RTX_MAX_SHOW_TASK_COUNT) {
            ignoreTaskList = ignoreTaskList.subList(0, RTX_MAX_SHOW_TASK_COUNT);
        }
        for (IgnoreStatDetail detail : ignoreTaskList) {
            Long taskId = detail.getTaskId();
            String nameCn = detail.getNameCn();
            String createFrom = (String) redisTemplate.opsForHash().get(PREFIX_TASK_INFO + taskId, KEY_CREATE_FROM);

            strBuilder.append(nameCn).append("(").append(detail.getGitRepo()).append(")");
            Integer defectIgnoreCount = detail.getDefectIgnoreCount();
            if (defectIgnoreCount != null) {
                String codeIssueUrl = NotifyUtils
                        .getCodeIssueUrl(detailVO.getProjectId(), nameCn, taskId, codeccHost, devopsHost, createFrom,
                                detail.getDimension());
                strBuilder.append("    [").append(defectIgnoreCount).append("个问题](").append(codeIssueUrl).append("&")
                        .append(queryParam).append(")");
            }

            Integer ccnIgnoreCount = detail.getCcnIgnoreCount();
            if (ccnIgnoreCount != null) {
                String codeCcnUrl = NotifyUtils
                        .getCodeCcnUrl(detailVO.getProjectId(), nameCn, taskId, codeccHost, devopsHost, createFrom);
                strBuilder.append("    [").append(ccnIgnoreCount).append("个风险函数](").append(codeCcnUrl).append("?")
                        .append(queryParam).append(")");
            }
            strBuilder.append("\n");
        }
        strBuilder.deleteCharAt(strBuilder.length() - 1);

        return strBuilder.toString();
    }

    /**
     * 获取告警统计的处理人（或者忽略人）
     *
     * @return set
     */
    private Set<String> getIgnoreAuthor(@NotNull List<IgnoreTypeStatModel> defectAuthor) {
        return defectAuthor.stream().filter(model -> StringUtils.isNotEmpty(model.getIgnoreAuthor()))
                .map(IgnoreTypeStatModel::getIgnoreAuthor).collect(Collectors.toSet());
    }

    /**
     * 获取存在告警的任务创建者
     */
    private void getDefectTaskCreator(@NotNull List<IgnoreTypeStatModel> defectStatModels,
            Map<Long, String> taskCreatorMap, Set<String> creatorSet) {
        for (IgnoreTypeStatModel model : defectStatModels) {
            String creator = taskCreatorMap.get(model.getTaskId());
            if (StringUtils.isNotBlank(creator)) {
                creatorSet.add(creator);
            }
        }
    }

    /**
     * 把models里的告警数按任务id分组到Map里
     */
    private void groupTaskDefectCount(@NotNull List<IgnoreTypeStatModel> defectStatModels,
            Map<Long, List<Integer>> taskIgnoreMap) {
        for (IgnoreTypeStatModel statModel : defectStatModels) {
            List<Integer> defectCountList =
                    taskIgnoreMap.computeIfAbsent(statModel.getTaskId(), k -> Lists.newArrayList());
            defectCountList.add(statModel.getDefectCount());
        }
    }

    /**
     * 把models里的告警数按处理人分组到Map里
     */
    private void groupAuthorDefectCount(@NotNull List<IgnoreTypeStatModel> defectStatModels,
            Map<String, Integer> authorIgnoreCountMap, Map<String, IgnoreStatDetail> authorIgnoreMap) {
        for (IgnoreTypeStatModel statModel : defectStatModels) {
            String ignoreAuthor = statModel.getIgnoreAuthor();
            int defectCount = statModel.getDefectCount();

            IgnoreStatDetail ignoreStatDetail =
                    authorIgnoreMap.computeIfAbsent(ignoreAuthor, k -> new IgnoreStatDetail(ignoreAuthor));
            ignoreStatDetail.addDefectIgnoreCount(defectCount);

            // 找出最大忽略数的任务
            Integer maxCount = authorIgnoreCountMap.get(ignoreAuthor);
            if (null == maxCount || maxCount < defectCount) {
                ignoreStatDetail.setTaskId(statModel.getTaskId());
                authorIgnoreCountMap.put(ignoreAuthor, defectCount);
            }
        }
    }

    private void groupAuthorDefectCount4CCN(@NotNull List<IgnoreTypeStatModel> defectStatModels,
            Map<String, IgnoreStatDetail> authorIgnoreMap) {
        for (IgnoreTypeStatModel statModel : defectStatModels) {
            String ignoreAuthor = statModel.getIgnoreAuthor();
            IgnoreStatDetail ignoreStatDetail =
                    authorIgnoreMap.computeIfAbsent(ignoreAuthor, k -> new IgnoreStatDetail(ignoreAuthor));
            ignoreStatDetail.addCcnIgnoreCount(statModel.getDefectCount());
            // 如果只接入CCN,也要给个维度兼容邮件HTML内容生成
            if (null == ignoreStatDetail.getDimension()) {
                ignoreStatDetail.setDimension(ToolType.CCN.name());
            }
        }
    }

    /**
     * 查询指定忽略类型的所有告警处理人
     *
     * @return set
     */
    private Set<String> queryAllDefectAuthor(int ignoreTypeId, Map<Long, Set<String>> taskLintToolsMap,
            Map<Long, Set<String>> taskCommonToolsMap, Set<Long> taskCCNSet) {
        Set<String> author = Sets.newHashSet();

        // lint告警处理人
        List<IgnoreTypeStatModel> defectAuthorLint =
                lintDefectV2Dao.findIgnoreDefectAuthor(taskLintToolsMap, ignoreTypeId, IGNORE_STATUS);
        // 缺陷类告警处理人
        List<IgnoreTypeStatModel> defectAuthorCommon =
                defectDao.findIgnoreDefectAuthor(taskCommonToolsMap, ignoreTypeId, IGNORE_STATUS);
        // 风险函数处理人
        List<IgnoreTypeStatModel> defectAuthorCCN =
                ccnDefectDao.findIgnoreDefectAuthor(taskCCNSet, ignoreTypeId, IGNORE_STATUS);

        author.addAll(getIgnoreAuthor(defectAuthorLint));
        author.addAll(getIgnoreAuthor(defectAuthorCommon));
        author.addAll(getIgnoreAuthor(defectAuthorCCN));
        return author;
    }

    /**
     * 获取工具维度
     */
    private String getDefaultToolDimension(Set<String> commonTool, Set<String> lintTool) {
        if (CollectionUtils.isNotEmpty(commonTool)) {
            return toolMetaCacheService.getToolBaseMetaCache(commonTool.iterator().next()).getType();
        }

        if (CollectionUtils.isNotEmpty(lintTool)) {
            return toolMetaCacheService.getToolBaseMetaCache(lintTool.iterator().next()).getType();
        }
        return null;
    }

    /**
     * 记录蓝盾项目名称
     */
    private void recordProjectName(@NotNull Map<String, String> projectNameMap, @NotNull TaskBaseVO taskBaseVO) {
        String projectName = projectNameMap.get(taskBaseVO.getProjectId());
        if (StringUtils.isEmpty(projectName) && StringUtils.isNotEmpty(taskBaseVO.getProjectName())) {
            projectNameMap.put(taskBaseVO.getProjectId(), taskBaseVO.getProjectName());
        }
    }

    /**
     * 拼接邮件标题
     */
    private String getEmailTitle(String projectName, String ignoreTypeName) {
        return String.format("【CodeCC】%s忽略问题Review-%s", projectName, ignoreTypeName);
    }

    /**
     * 生成邮件URL根路径
     *
     * @param projectId 项目id
     * @return url
     */
    private String generateEmailRootUrl(String projectId) {
        return NotifyUtils.isGongfengScanProject(projectId) || NotifyUtils.isStream2_0(projectId)
                ? String.format("http://%s/codecc/%s/", codeccHost, projectId)
                : String.format("http://%s/console/codecc/%s/", devopsHost, projectId);
    }

    /**
     * 触发项目维度的忽略统计并发送通知
     *
     * @param projectId 项目id ——必须
     * @param ignoreTypeName 类型名称
     * @param ignoreTypeId 类型id ——必须
     * @param createFrom 类型来源 sys , project
     */
    @Override
    public void triggerProjectStatisticAndSend(String projectId, String ignoreTypeName,
            Integer ignoreTypeId, String createFrom) {
        log.info("triggerProjectStatistic param: projectId:{}, ignoreTypeName:{}, id:{}, from: {}", projectId,
                ignoreTypeName, ignoreTypeId, createFrom);

        IgnoreTypeProjectConfig ignoreTypeProjectConfig =
                ignoreTypeProjectRepository.findFirstByProjectIdAndIgnoreTypeId(projectId, ignoreTypeId);
        if (!checkConfigIsValid(ignoreTypeProjectConfig)) {
            return;
        }

        List<TaskBaseVO> taskBaseVOList =
                client.get(ServiceTaskRestResource.class).getTaskListByProjectId(projectId).getData();
        if (CollectionUtils.isEmpty(taskBaseVOList)) {
            log.warn("query effect task list by project:[{}] is empty! stop statistic!", projectId);
            return;
        }

        int taskIgnoreSum = 0;
        Integer defectIgnoreSum = null;
        Integer ccnIgnoreSum = null;
        Map<String, String> projectNameMap = Maps.newHashMap();
        Map<Long, String> taskNameCnMap = Maps.newHashMap();
        Map<Long, String> taskRepoMap = Maps.newHashMap();
        Map<Long, String> taskCreatorMap = Maps.newHashMap();

        Map<Long, Set<String>> taskLintToolsMap = Maps.newHashMap();
        Map<Long, Set<String>> taskCommonToolsMap = Maps.newHashMap();
        Set<Long> taskCCNSet = Sets.newHashSet();
        for (TaskBaseVO taskBaseVO : taskBaseVOList) {
            long taskId = taskBaseVO.getTaskId();
            List<ToolConfigBaseVO> enableToolList = taskBaseVO.getEnableToolList();
            if (CollectionUtils.isEmpty(enableToolList)) {
                log.info("task enable tool list is empty: {}", taskId);
                continue;
            }

            for (ToolConfigBaseVO toolConfigBaseVO : enableToolList) {
                String toolName = toolConfigBaseVO.getToolName();
                String toolType = toolMetaCacheService.getToolBaseMetaCache(toolName).getType();
                Set<String> toolList;

                // 按类型分组工具待查询
                if (ToolType.STANDARD.name().equals(toolType) || ToolType.SECURITY.name().equals(toolType)) {
                    if (defectIgnoreSum == null) {
                        defectIgnoreSum = 0;
                    }
                    toolList = taskLintToolsMap.computeIfAbsent(taskId, k -> Sets.newHashSet());
                } else if (ToolType.DEFECT.name().equals(toolType)) {
                    if (defectIgnoreSum == null) {
                        defectIgnoreSum = 0;
                    }
                    toolList = taskCommonToolsMap.computeIfAbsent(taskId, k -> Sets.newHashSet());
                } else if (ToolType.CCN.name().equals(toolType)) {
                    if (ccnIgnoreSum == null) {
                        ccnIgnoreSum = 0;
                    }
                    taskCCNSet.add(taskId);
                    continue;
                } else {
                    log.warn("tool don't need to statistic, continue: {}", toolName);
                    continue;
                }
                toolList.add(toolName);
            }

            TaskCodeLibraryVO codeLibraryInfo = taskBaseVO.getCodeLibraryInfo();
            if (null != codeLibraryInfo) {
                List<CodeLibraryInfoVO> codeInfo = codeLibraryInfo.getCodeInfo();
                if (CollectionUtils.isNotEmpty(codeInfo)) {
                    CodeLibraryInfoVO next = codeInfo.iterator().next();
                    taskRepoMap.put(taskId, String.format("%s@%s", next.getAliasName(), next.getBranch()));
                }
            }
            taskNameCnMap.put(taskId, taskBaseVO.getNameCn());
            taskCreatorMap.put(taskId, taskBaseVO.getCreatedBy());
            // 记录蓝盾项目名称
            recordProjectName(projectNameMap, taskBaseVO);
        }

        // 统计出指定忽略类型的告警数
        List<IgnoreTypeStatModel> lintIgnoreTypeStatModels =
                lintDefectV2Dao.statisticLintIgnoreDefect(taskLintToolsMap, ignoreTypeId, IGNORE_STATUS);
        List<IgnoreTypeStatModel> commonIgnoreTypeStatModels =
                defectDao.statisticIgnoreDefect(taskCommonToolsMap, ignoreTypeId, IGNORE_STATUS);
        List<IgnoreTypeStatModel> ccnIgnoreTypeStatModels =
                ccnDefectDao.statisticIgnoreDefect(taskCCNSet, ignoreTypeId, IGNORE_STATUS);

        // 如果没告警数据，则不需要发送通知
        if (CollectionUtils.isEmpty(lintIgnoreTypeStatModels) && CollectionUtils.isEmpty(commonIgnoreTypeStatModels)
                && CollectionUtils.isEmpty(ccnIgnoreTypeStatModels)) {
            log.warn("param: projectId:{}, ignoreTypeId:{} defect is empty!", projectId, ignoreTypeId);
            return;
        }

        IgnoreTypeNotifyEntity notify = ignoreTypeProjectConfig.getNotify();
        List<String> notifyReceiverTypes = notify.getNotifyReceiverTypes();

        Set<String> receiverSet = Sets.newHashSet();
        // 统计指定忽略类型id 的告警处理人
        if (notifyReceiverTypes.contains(IgnoreTypeNotifyReceiverType.DEFECT_AUTHOR.getType())) {
            Set<String> authors = queryAllDefectAuthor(ignoreTypeId, taskLintToolsMap, taskCommonToolsMap, taskCCNSet);
            receiverSet.addAll(authors);
        }
        // 忽略人
        if (notifyReceiverTypes.contains(IgnoreTypeNotifyReceiverType.IGNORE_AUTHOR.getType())) {
            receiverSet.addAll(getIgnoreAuthor(lintIgnoreTypeStatModels));
            receiverSet.addAll(getIgnoreAuthor(commonIgnoreTypeStatModels));
            receiverSet.addAll(getIgnoreAuthor(ccnIgnoreTypeStatModels));
        }
        // 任务创建者
        if (notifyReceiverTypes.contains(IgnoreTypeNotifyReceiverType.TASK_CREATOR.getType())) {
            Set<String> taskCreator = Sets.newHashSet();
            getDefectTaskCreator(lintIgnoreTypeStatModels, taskCreatorMap, taskCreator);
            getDefectTaskCreator(commonIgnoreTypeStatModels, taskCreatorMap, taskCreator);
            getDefectTaskCreator(ccnIgnoreTypeStatModels, taskCreatorMap, taskCreator);
            receiverSet.addAll(taskCreator);
        }
        // 附加通知人
        Set<String> extReceiver = notify.getExtReceiver();
        if (CollectionUtils.isNotEmpty(extReceiver)) {
            receiverSet.addAll(extReceiver);
        }
        if (CollectionUtils.isEmpty(receiverSet)) {
            log.warn("this notify don't have receiver, projectId:{}, ignoreTypeName:{}, id:{}",
                    projectId, ignoreTypeName, ignoreTypeId);
            return;
        }

        String projectName = projectNameMap.getOrDefault(projectId, "");

        IgnoreTypeReportDetailVO reportDetailVO =
                new IgnoreTypeReportDetailVO(projectId, projectName, ignoreTypeId, ignoreTypeProjectConfig.getName());
        reportDetailVO.setUrlRoot(generateEmailRootUrl(projectId));

        // 按任务维度统计忽略数
        Map<Long, List<Integer>> taskIgnoreMap = Maps.newHashMap();
        groupTaskDefectCount(lintIgnoreTypeStatModels, taskIgnoreMap);
        groupTaskDefectCount(commonIgnoreTypeStatModels, taskIgnoreMap);

        Map<Long, List<Integer>> ccnTaskIgnoreMap = Maps.newHashMap();
        groupTaskDefectCount(ccnIgnoreTypeStatModels, ccnTaskIgnoreMap);

        // 组装任务维度的统计数据
        List<IgnoreStatDetail> ignoreTaskList = Lists.newArrayList();
        for (TaskBaseVO taskBaseVO : taskBaseVOList) {
            Long taskId = taskBaseVO.getTaskId();

            List<Integer> defectCount = taskIgnoreMap.get(taskId);
            List<Integer> ccnDefectCount = ccnTaskIgnoreMap.get(taskId);
            if (CollectionUtils.isEmpty(defectCount) && CollectionUtils.isEmpty(ccnDefectCount)) {
                log.warn("the task don't have ignored defect: {}", taskId);
                continue;
            }

            IgnoreStatDetail ignoreStatDetail =
                    new IgnoreStatDetail(taskId, taskNameCnMap.getOrDefault(taskId, "Unknown"),
                            taskRepoMap.getOrDefault(taskId, "--"));

            if (CollectionUtils.isNotEmpty(defectCount)) {
                int ignoreDefectCount = defectCount.stream().reduce(Integer::sum).orElse(0);
                if (defectIgnoreSum == null) {
                    defectIgnoreSum = 0;
                }
                defectIgnoreSum += ignoreDefectCount;
                ignoreStatDetail.setDefectIgnoreCount(ignoreDefectCount);
                ignoreStatDetail.setDimension(
                        getDefaultToolDimension(taskCommonToolsMap.get(taskId), taskLintToolsMap.get(taskId)));
            }

            if (CollectionUtils.isNotEmpty(ccnDefectCount)) {
                int ignoreDefectCount = ccnDefectCount.stream().reduce(Integer::sum).orElse(0);
                if (ccnIgnoreSum == null) {
                    ccnIgnoreSum = 0;
                }
                ccnIgnoreSum += ignoreDefectCount;
                ignoreStatDetail.setCcnIgnoreCount(ignoreDefectCount);
            }
            taskIgnoreSum++;
            ignoreTaskList.add(ignoreStatDetail);
        }
        reportDetailVO.setTaskIgnoreSum(taskIgnoreSum);
        reportDetailVO.setDefectIgnoreSum(defectIgnoreSum);
        reportDetailVO.setCcnIgnoreSum(ccnIgnoreSum);

        // 任务维度按忽略数降序排列
        ignoreTaskList.sort(Comparator
                .comparing(IgnoreStatDetail::getDefectIgnoreCount, Comparator.nullsFirst(Integer::compareTo))
                .thenComparing(IgnoreStatDetail::getCcnIgnoreCount, Comparator.nullsFirst(Integer::compareTo))
                .reversed()
        );
        reportDetailVO.setTaskList(ignoreTaskList);

        List<String> notifyTypes = notify.getNotifyTypes();
        // 发送RTX
        if (notifyTypes.contains(ComConstants.NotifyType.RTX.getType())) {
            String notifyContent = generateNotifyContent(reportDetailVO);

            RtxNotifyVO rtxNotifyVO = new RtxNotifyVO(
                    receiverSet,
                    null,
                    notifyContent,
                    WeworkReceiverType.single.name(),
                    WeworkTextType.markdown.name()
            );
            rabbitTemplate
                    .convertAndSend(EXCHANGE_CODECC_GENERAL_NOTIFY, ROUTE_CODECC_RTX_NOTIFY_SEND, rtxNotifyVO);
            log.info("trigger send RTX notify successfully!");
        }

        // 发送EMail
        if (notifyTypes.contains(ComConstants.NotifyType.EMAIL.getType())) {
            Map<String, Integer> authorIgnoreCountMap = Maps.newHashMap();
            Map<String, IgnoreStatDetail> authorIgnoreMap = Maps.newHashMap();
            groupAuthorDefectCount(lintIgnoreTypeStatModels, authorIgnoreCountMap, authorIgnoreMap);
            groupAuthorDefectCount(commonIgnoreTypeStatModels, authorIgnoreCountMap, authorIgnoreMap);
            groupAuthorDefectCount4CCN(ccnIgnoreTypeStatModels, authorIgnoreMap);

            List<IgnoreStatDetail> ignoreAuthorList = Lists.newArrayList(authorIgnoreMap.values());

            ignoreAuthorList.sort(Comparator
                    .comparing(IgnoreStatDetail::getDefectIgnoreCount, Comparator.nullsFirst(Integer::compareTo))
                    .thenComparing(IgnoreStatDetail::getCcnIgnoreCount, Comparator.nullsFirst(Integer::compareTo))
                    .reversed());

            // 仅展示 EMAIL_MAX_SHOW_COUNT 个
            if (ignoreAuthorList.size() > EMAIL_MAX_SHOW_COUNT) {
                ignoreAuthorList = ignoreTaskList.subList(0, EMAIL_MAX_SHOW_COUNT);
            }
            for (IgnoreStatDetail detail : ignoreAuthorList) {
                detail.setNameCn(taskNameCnMap.getOrDefault(detail.getTaskId(), "Unknown"));
            }
            reportDetailVO.setAuthorList(ignoreAuthorList);

            List<IgnoreStatDetail> taskList = reportDetailVO.getTaskList();
            if (CollectionUtils.isNotEmpty(taskList) && taskList.size() > EMAIL_MAX_SHOW_COUNT) {
                taskList = taskList.subList(0, EMAIL_MAX_SHOW_COUNT);
                reportDetailVO.setTaskList(taskList);
            }

            String emailTitle = getEmailTitle(projectName, ignoreTypeProjectConfig.getName());
            IgnoreTypeEmailDTO ignoreTypeEmailDTO = new IgnoreTypeEmailDTO(emailTitle, receiverSet, reportDetailVO);
            rabbitTemplate.convertAndSend(EXCHANGE_IGNORE_EMAIL_SEND, ROUTE_IGNORE_EMAIL_SEND, ignoreTypeEmailDTO);
            log.info("trigger send Email notify successfully!");
        }
    }

    /**
     * 统计使用了忽略类型告警数量
     * @param projectId
     * @param userName
     * @param ignoreTypeIds 如果没有传入，就查询项目下所有的忽略类型
     * @return
     */
    @Override
    public List<IgnoreTypeDefectStatResponse> getIgnoreTypeDefectStat(String projectId, String userName,
            Set<Integer> ignoreTypeIds) {
        log.info("getIgnoreTypeDefectStat start projectId:{}, userName:{}, ignoreTypeIds:{}", projectId, userName,
                CollectionUtils.isEmpty(ignoreTypeIds) ? "null" : JSONObject.toJSONString(ignoreTypeIds));
        if (CollectionUtils.isEmpty(ignoreTypeIds)) {
            List<IgnoreTypeProjectConfigVO> ignoreTypes = queryIgnoreTypeProjectList(projectId, userName);
            if (CollectionUtils.isEmpty(ignoreTypes)) {
                return Collections.emptyList();
            }
            ignoreTypeIds = ignoreTypes.stream().map(IgnoreTypeProjectConfigVO::getIgnoreTypeId).collect(
                    Collectors.toSet());
        }
        List<IgnoreTypeDefectStatResponse> statResponses = new LinkedList<>();
        Map<Integer, IgnoreTypeDefectStatResponse> statResponseMap = new HashMap<>();
        // 记录任务数量
        Map<Integer, Set<Long>> ignoreTypeToTaskIdMap = new HashMap<>();
        for (Integer ignoreTypeId : ignoreTypeIds) {
            IgnoreTypeDefectStatResponse response = new IgnoreTypeDefectStatResponse(ignoreTypeId, 0L, 0L, 0L);
            statResponses.add(response);
            statResponseMap.put(ignoreTypeId, response);
            ignoreTypeToTaskIdMap.put(ignoreTypeId, new HashSet<>());
        }

        // 对任务进行分批统计，每次1000个任务 获取项目的任务列表
        List<Long> taskIds = ParamUtils.allTaskByProjectIdIfEmpty(Lists.newArrayList(), projectId, userName);
        // 对任务进行分批统计，每次1000个任务, 避免内存占用过大
        int pageSize = 1000;
        int page = taskIds.size() / pageSize + (taskIds.size() % pageSize != 0 ? 1 : 0);
        for (int i = 0; i < page; i++) {
            List<Long> pageTaskIds = taskIds.subList(i * pageSize, Math.min(i * pageSize + pageSize, taskIds.size()));
            // 分开代码问题与CCN
            Pair<Map<Long, Set<String>>, Set<Long>> toolMapSetPair = ParamUtils.getDefectAndCCNTaskToToolMapByTaskId(
                    pageTaskIds);
            // 统计代码问题数量，以IgnoreTypeId为维度
            List<IgnoreTypeStatModel> lintModels = lintDefectV2Dao.statisticIgnoreDefectByIgnoreTypeId(
                    toolMapSetPair.getFirst(), ignoreTypeIds);
            lintModels.forEach(model -> {
                if (model != null && model.getIgnoreTypeId() != null) {
                    IgnoreTypeDefectStatResponse response = statResponseMap.get(model.getIgnoreTypeId());
                    ignoreTypeToTaskIdMap.get(model.getIgnoreTypeId()).addAll(model.getTaskIdSet());
                    if (response != null) {
                        response.setDefect(response.getDefect() + model.getDefectCount());
                    }
                }
            });
            // 统计圈复杂度数量，以IgnoreTypeId为维度
            List<IgnoreTypeStatModel> ccnModels = ccnDefectDao.statisticIgnoreDefectByIgnoreTypeId(
                    toolMapSetPair.getSecond(), ignoreTypeIds);
            ccnModels.forEach(model -> {
                if (model != null && model.getIgnoreTypeId() != null) {
                    IgnoreTypeDefectStatResponse response = statResponseMap.get(model.getIgnoreTypeId());
                    ignoreTypeToTaskIdMap.get(model.getIgnoreTypeId()).addAll(model.getTaskIdSet());
                    if (response != null) {
                        response.setRiskFunction(response.getRiskFunction() + model.getDefectCount());
                    }
                }
            });
        }
        // 统计任务数量
        for (Entry<Integer, IgnoreTypeDefectStatResponse> entry : statResponseMap.entrySet()) {
            Set<Long> taskIdSet = ignoreTypeToTaskIdMap.get(entry.getKey());
            entry.getValue().setTaskCount(CollectionUtils.isEmpty(taskIdSet) ? 0L : taskIdSet.size());
        }

        log.info("getIgnoreTypeDefectStat end projectId:{}, userName:{}", projectId, userName);
        return statResponses;
    }


}
