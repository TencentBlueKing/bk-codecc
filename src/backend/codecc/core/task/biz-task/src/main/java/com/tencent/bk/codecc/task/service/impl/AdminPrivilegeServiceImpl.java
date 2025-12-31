package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.audit.utils.json.JsonUtils;
import com.tencent.bk.codecc.task.dao.mongorepository.AdminPrivilegeInfoRepository;
import com.tencent.bk.codecc.task.model.AdminPrivilegeEntity;
import com.tencent.bk.codecc.task.service.AdminPrivilegeService;
import com.tencent.bk.codecc.task.vo.AdminPrivilegeInfoVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.RedisKeyConstants;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.CommonMessageCode.JSON_PARAM_IS_INVALID;
import static com.tencent.devops.common.constant.CommonMessageCode.PARAMETER_IS_INVALID;
import static com.tencent.devops.common.constant.CommonMessageCode.PARAMETER_IS_NULL;
import static com.tencent.devops.common.constant.CommonMessageCode.RECORD_NOT_EXITS;
import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_ADMIN_PRIVILEGE_REFRESH;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_ADMIN_PRIVILEGE_REFRESH;

@Service
public class AdminPrivilegeServiceImpl implements AdminPrivilegeService {

    private static final Logger logger = LoggerFactory.getLogger(AdminPrivilegeServiceImpl.class);

    @Autowired
    private AdminPrivilegeInfoRepository adminPrivilegeInfoRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public Boolean triggerRefreshAdminPrivilegeStatus() {
        logger.info("start to triggerRefreshAdminPrivilegeStatus");
        rabbitTemplate.convertAndSend(
                EXCHANGE_ADMIN_PRIVILEGE_REFRESH,
                ROUTE_ADMIN_PRIVILEGE_REFRESH,
                ComConstants.EMPTY_STRING
        );
        return Boolean.TRUE;
    }

    @Override
    public Boolean upsertAdminPrivilegeInfo(AdminPrivilegeInfoVO reqVO) {
        logger.info("upsertAdminPrivilegeInfo started with reqVO: {}", JsonUtils.toJson(reqVO));
        // 校验输入参数
        verifyInit(reqVO);
        String userId = reqVO.getUserId();
        AdminPrivilegeEntity adminPrivilegeEntity = adminPrivilegeInfoRepository.findFirstByUserId(userId);
        if (adminPrivilegeEntity != null) {
            handleExistingPrivilege(adminPrivilegeEntity, reqVO);
            adminPrivilegeEntity.applyAuditInfoOnUpdate(reqVO.getUpdatedBy());
        } else {
            adminPrivilegeEntity = createNewPrivilegeEntity(reqVO, userId);
            adminPrivilegeEntity.applyAuditInfoOnCreate(reqVO.getUpdatedBy());
        }
        adminPrivilegeInfoRepository.save(adminPrivilegeEntity);
        // Redis同步BG管理员信息或平台管理员信息
        batchExecute(adminPrivilegeEntity);
        return true;
    }

    @Override
    public Boolean updateAdminPrivilegeStatus(String userName, String userId, Boolean status) {
        AdminPrivilegeEntity adminPrivilegeEntity = adminPrivilegeInfoRepository.findFirstByUserId(userId);
        if (adminPrivilegeEntity == null) {
            throw new CodeCCException(RECORD_NOT_EXITS, "账户ID");
        }
        adminPrivilegeEntity.setStatus(status);
        adminPrivilegeEntity.applyAuditInfoOnUpdate(userName);
        adminPrivilegeInfoRepository.save(adminPrivilegeEntity);
        // Redis同步BG管理员信息或平台管理员信息
        batchExecute(adminPrivilegeEntity);
        return true;
    }

    @Override
    public Boolean renewalAdminPrivilegeValidityDays(String userName, String userId, Integer validityDays) {
        AdminPrivilegeEntity adminPrivilegeEntity = adminPrivilegeInfoRepository.findFirstByUserId(userId);
        if (adminPrivilegeEntity == null) {
            throw new CodeCCException(RECORD_NOT_EXITS, "账户ID");
        }
        if (validityDays == null || validityDays < 1 || validityDays > 365) {
            throw new CodeCCException(PARAMETER_IS_INVALID, "申请有效天数必须在1~365天之间");
        }
        // 如果账户已经过期，按照当前时间续期，未过期则按照结束时间续期
        long baseTime = Math.max(adminPrivilegeEntity.getEndTime(), System.currentTimeMillis());
        adminPrivilegeEntity.setEndTime(baseTime + TimeUnit.DAYS.toMillis(validityDays));
        adminPrivilegeEntity.applyAuditInfoOnUpdate(userName);
        adminPrivilegeInfoRepository.save(adminPrivilegeEntity);
        return true;
    }

    @Override
    public void batchUpdateAdminPrivilegeStatus() {
        // 获取当前时间戳
        long currentTimeMillis = System.currentTimeMillis();
        // 查询已经过期时间的BG和平台管理员
        List<AdminPrivilegeEntity> adminEntities =
                adminPrivilegeInfoRepository.findAllByStatusAndEndTimeBefore(Boolean.TRUE, currentTimeMillis);

        if (adminEntities.isEmpty()) {
            logger.info("No expired admin privileges found to update");
            return;
        }

        // 更新已经过期的bg状态
        adminEntities.forEach(adminPrivilegeEntity -> {
            adminPrivilegeEntity.setStatus(Boolean.FALSE);
            adminPrivilegeEntity.applyAuditInfoOnUpdate(ComConstants.SYSTEM_USER);
        });
        adminPrivilegeInfoRepository.saveAll(adminEntities);
        // 同步Redis
        adminEntities.forEach(this::batchExecute);
    }

    @Override
    public Boolean initializationBgAndGlobalAdminMember() {
        List<AdminPrivilegeEntity> adminPrivilegeEntities = adminPrivilegeInfoRepository.findAllByStatus(Boolean.TRUE);
        if (adminPrivilegeEntities.isEmpty()) {
            return Boolean.FALSE;
        }
        adminPrivilegeEntities.forEach(this::batchExecute);
        return Boolean.TRUE;
    }

    @Override
    public List<String> queryAdminMemberByType(ComConstants.PrivilegeType privilegeType) {
        if (privilegeType == null) {
            logger.info("queryAdminMemberByType is null");
            return Collections.emptyList();
        }
        // 查询管理员列表
        List<AdminPrivilegeEntity> adminEntities = adminPrivilegeInfoRepository.findAllByPrivilegeTypeAndStatus(
                privilegeType.value(), Boolean.TRUE
        );
        return adminEntities.stream()
                .map(AdminPrivilegeEntity::getUserId)
                .collect(Collectors.toList());
    }

    /**
     * 处理已存在的权限实体
     */
    private void handleExistingPrivilege(AdminPrivilegeEntity entity, AdminPrivilegeInfoVO reqVO) {
        // 适配审批接口,如果原来管理员表里面已经有了申请用户，则需要刷新权限有效期和账户状态更改为待审核
        // 如果申请用户已经是管理员且在启用状态,则需要联系op管理员停用该账户才能继续申请
        if (reqVO.getStatus() == null && Boolean.TRUE.equals(entity.getStatus())) {
            String errMsg = String.format("%s 已存在，需要联系OP管理员停用该用户", reqVO.getUserId());
            throw new CodeCCException(errMsg);
        }
        // 初始化有效期，进入到审核状态
        if (reqVO.getStatus() == null) {
            entity.setStatus(null);
            validTimeInit(reqVO, entity);
        }

        // 正常更新逻辑
        updatePrivilegeEntity(entity, reqVO);
    }

    /**
     * 创建新的权限实体
     */
    private AdminPrivilegeEntity createNewPrivilegeEntity(AdminPrivilegeInfoVO reqVO, String userId) {
        if (reqVO.getValidityDays() == null) {
            throw new CodeCCException(PARAMETER_IS_NULL, "创建新的管理员，申请权限天数不能为空");
        }
        AdminPrivilegeEntity newEntity = new AdminPrivilegeEntity();
        newEntity.setUserId(userId);
        // 按照管理员类型赋予不同个性参数
        this.setPersonalizedParams(newEntity, reqVO);
        newEntity.setReason(StringUtils.defaultString(reqVO.getReason()));
        newEntity.setStatus(reqVO.getStatus());
        validTimeInit(reqVO, newEntity);
        return newEntity;
    }

    /**
     * 更新权限实体的通用逻辑
     */
    private void updatePrivilegeEntity(AdminPrivilegeEntity entity, AdminPrivilegeInfoVO reqVO) {
        // 应对不同管理员类型更改，更新前需要清空原管理员个性参数
        this.initParamLists(entity);
        // 按照管理员类型赋予不同个性参数
        this.setPersonalizedParams(entity, reqVO);
        entity.setReason(StringUtils.defaultString(reqVO.getReason()));
    }

    /**
     * 有效时间初始化
     */
    private void validTimeInit(AdminPrivilegeInfoVO reqVO, AdminPrivilegeEntity adminPrivilegeEntity) {
        Instant now = Instant.now();
        ZonedDateTime startTimeZoned = now.atZone(ZoneId.systemDefault());
        // 获取开始日期的 LocalDate（去掉时分秒）
        LocalDate startDate = startTimeZoned.toLocalDate();
        // 计算结束日期（开始日期 + 有效期天数 - 1天）
        // 因为如果用户申请1天，应该是当天00:00:00到当天23:59:59
        LocalDate endDate = startDate.plusDays(reqVO.getValidityDays() - 1);
        // 将开始时间设置为当天的00:00:00
        ZonedDateTime adjustedStartTime = startDate.atStartOfDay(ZoneId.systemDefault());
        // 将结束时间设置为当天的23:59:59
        ZonedDateTime endTimeZoned = endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault());
        long startTime = adjustedStartTime.toInstant().toEpochMilli();
        long endTime = endTimeZoned.toInstant().toEpochMilli();
        adminPrivilegeEntity.setStartTime(startTime);
        adminPrivilegeEntity.setEndTime(endTime);
    }

    /**
     * 校验字段
     */
    private void verifyInit(AdminPrivilegeInfoVO reqVO) {
        if (StringUtils.isBlank(reqVO.getUserId())) {
            throw new CodeCCException(PARAMETER_IS_NULL, new String[]{"用户ID"});
        }

        ComConstants.PrivilegeType privilegeType = ComConstants.PrivilegeType.getByName(reqVO.getPrivilegeType());
        if (privilegeType == null) {
            throw new CodeCCException(PARAMETER_IS_INVALID, new String[]{"管理员类型为非法字段"});
        }

        // 校验不同管理员类型的个性参数
        if (privilegeType == ComConstants.PrivilegeType.BG_ADMIN) {
            validateBgAdmin(reqVO);
        }
    }

    /**
     * 校验BG管理员
     */
    private void validateBgAdmin(AdminPrivilegeInfoVO reqVO) {
        if (CollectionUtils.isEmpty(reqVO.getCreateFroms())) {
            throw new CodeCCException(PARAMETER_IS_NULL, new String[]{"授权来源平台"});
        }
        if (CollectionUtils.isEmpty(reqVO.getBgIdList())) {
            throw new CodeCCException(PARAMETER_IS_NULL, new String[]{"授权BG"});
        }
    }

    /**
     * 初始化管理员个性参数列表
     */
    private void initParamLists(AdminPrivilegeEntity entity) {
        entity.setBgIdList(Collections.emptyList());
        entity.setCreateFroms(Collections.emptyList());
    }

    /**
     * 根据管理员类型设置个性化参数
     */
    private void setPersonalizedParams(AdminPrivilegeEntity entity, AdminPrivilegeInfoVO reqVO) {
        switch (ComConstants.PrivilegeType.valueOf(reqVO.getPrivilegeType())) {
            case GLOBAL_ADMIN:
                break;
            case BG_ADMIN:
                entity.setBgIdList(ListUtils.emptyIfNull(reqVO.getBgIdList()));
                entity.setCreateFroms(ListUtils.emptyIfNull(reqVO.getCreateFroms()));
                break;
            default:
                throw new CodeCCException(JSON_PARAM_IS_INVALID, "传入非法管理员类型: " + reqVO.getPrivilegeType());
        }

        entity.setPrivilegeType(reqVO.getPrivilegeType());
    }

    /**
     * Redis同步BG管理员信息和平台管理员信息
     * @param adminPrivilegeEntity 实体对象
     */
    private void batchExecute(AdminPrivilegeEntity adminPrivilegeEntity) {
        // 处理平台管理员
        handleGlobalAdmin(adminPrivilegeEntity);
        // 处理bg管理员
        handleBgAdmin(adminPrivilegeEntity);
    }

    // 同步平台管理员
    private void handleGlobalAdmin(AdminPrivilegeEntity adminPrivilegeEntity) {
        if (!ComConstants.PrivilegeType.GLOBAL_ADMIN.value().equals(adminPrivilegeEntity.getPrivilegeType())) {
            return;
        }
        // 查询管理员列表
        List<AdminPrivilegeEntity> adminEntities = adminPrivilegeInfoRepository.findAllByPrivilegeTypeAndStatus(
                ComConstants.PrivilegeType.GLOBAL_ADMIN.value(), Boolean.TRUE
        );
        List<String> userIdList = adminEntities.stream()
                .map(AdminPrivilegeEntity::getUserId)
                .collect(Collectors.toList());
        // 分号分隔添加到redis
        String userIds = String.join(ComConstants.SEMICOLON, userIdList);
        // 还是用原来base_data逻辑里面的key : ADMIN_MEMBER
        redisTemplate.opsForValue().set(ComConstants.KEY_ADMIN_MEMBER, userIds);
    }

    // 同步BG管理员
    private void handleBgAdmin(AdminPrivilegeEntity adminPrivilegeEntity) {
        if (!ComConstants.PrivilegeType.BG_ADMIN.value().equals(adminPrivilegeEntity.getPrivilegeType())) {
            return;
        }

        if (CollectionUtils.isEmpty(adminPrivilegeEntity.getCreateFroms())
                || CollectionUtils.isEmpty(adminPrivilegeEntity.getBgIdList())) {
            return;
        }

        String userId = adminPrivilegeEntity.getUserId();
        if (userId == null) {
            return;
        }

        redisTemplate.executePipelined((RedisCallback<Long>) conn -> {
            for (ComConstants.DefectStatType defectStatType : ComConstants.DefectStatType.values()) {
                processDefectStatType(conn, defectStatType, userId,
                        adminPrivilegeEntity.getCreateFroms(),
                        adminPrivilegeEntity.getBgIdList(),
                        adminPrivilegeEntity.getStatus());
            }
            return null;
        });
    }

    private void processDefectStatType(RedisConnection conn, ComConstants.DefectStatType defectStatType,
                                       String userId, List<String> createFroms, List<Integer> bgIdList, Boolean status
    ) {
        String createFrom = defectStatType.value();

        String formatKey = String.format("%s%s:%s",
                RedisKeyConstants.PREFIX_BG_ADMIN, createFrom, userId);
        byte[] key = formatKey.getBytes(StandardCharsets.UTF_8);

        // 清空所有该user_id的所有信息, 然后重新根据create_froms和status同步
        conn.del(key);

        if (status == null || !status || !createFroms.contains(createFrom)) {
            return;
        }

        for (Integer bgId : bgIdList) {
            try {
                if (bgId != null) {
                    conn.sAdd(key, bgId.toString().getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                logger.error("Failed to add BG ID {} to Redis key {}: {}",
                        bgId, formatKey, e.getMessage(), e);
            }
        }
    }
}
