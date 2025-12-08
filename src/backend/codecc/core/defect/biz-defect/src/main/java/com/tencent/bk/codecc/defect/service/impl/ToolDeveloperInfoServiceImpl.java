package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.bkpass.BkPassClientApi;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.ToolDeveloperInfoRepository;
import com.tencent.bk.codecc.defect.model.ToolDeveloperInfoEntity;
import com.tencent.bk.codecc.defect.service.ToolDeveloperInfoService;
import com.tencent.bk.codecc.defect.vo.ToolMemberInfoVO;
import com.tencent.bk.codecc.defect.vo.UserRoleVO;
import com.tencent.bk.codecc.defect.vo.developer.OpToolDeveloperInfoReqVO;
import com.tencent.bk.codecc.defect.vo.developer.ToolDeveloperInfoVO;
import com.tencent.devops.common.api.ToolTestConstants;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.RedisKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;


/**
 * 工具开发者服务实现方法
 *
 * @version V1.0
 * @date 2025/8/6
 */
@Service
@Slf4j
public class ToolDeveloperInfoServiceImpl implements ToolDeveloperInfoService {

    @Autowired
    private ToolDeveloperInfoRepository toolDeveloperInfoRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private BkPassClientApi bkPassClientApi;

    // 工具开发者角色
    private static final UserRoleVO DEVELOPER_ROLE = new UserRoleVO(
            ToolTestConstants.BKRoleType.DEVELOPER.getValue(),
            ToolTestConstants.BKRoleId.DEVELOPER.getValue()
    );

    // 工具管理员角色
    private static final UserRoleVO MANAGER_ROLE = new UserRoleVO(
            ToolTestConstants.BKRoleType.MANAGER.getValue(),
            ToolTestConstants.BKRoleId.MANAGER.getValue()
    );

    @Override
    public ToolDeveloperInfoVO getPermissionInfo(String toolName) {
        ToolDeveloperInfoEntity entity = toolDeveloperInfoRepository.findFirstByToolName(toolName);

        ToolDeveloperInfoVO result = new ToolDeveloperInfoVO();
        if (entity == null) {
            return result;
        }

        BeanUtils.copyProperties(entity, result);
        return result;
    }

    @Override
    public Boolean syncToolMembers(String toolName, List<ToolMemberInfoVO> toolMemberInfoList) {
        log.info("#syncToolMembers. toolName({}), toolMemberInfoList({})", toolName, toolMemberInfoList);
        ToolDeveloperInfoEntity entity = toolDeveloperInfoRepository.findFirstByToolName(toolName);
        if (entity == null) {
            entity = new ToolDeveloperInfoEntity();
            entity.setToolName(toolName);
            entity.applyAuditInfoOnCreate();
        }

        // 这 2 个 role 列表的数据以蓝鲸插件开发者中心为准
        entity.setDevelopers(new HashSet<>());
        entity.setMasters(new HashSet<>());

        for (ToolMemberInfoVO it : toolMemberInfoList) {
            if (ToolTestConstants.BKRoleId.DEVELOPER.getValue().equals(it.getRole().getId())) {
                entity.getDevelopers().add(it.getUsername());
            } else if (ToolTestConstants.BKRoleId.MANAGER.getValue().equals(it.getRole().getId())) {
                entity.getMasters().add(it.getUsername());
            }
        }

        toolDeveloperInfoRepository.save(entity);
        // Redis同步工具开发者信息
        handleToolAdmin(entity);
        return true;
    }

    public Boolean initializationToolDeveloper() {
        log.info("#toolDeveloperInfoService initializationToolDeveloper");
        List<ToolDeveloperInfoEntity> toolDeveloperInfoEntityList = toolDeveloperInfoRepository.findAll();
        if (toolDeveloperInfoEntityList.isEmpty()) {
            return Boolean.FALSE;
        }
        toolDeveloperInfoEntityList.forEach(this::handleToolAdmin);
        return Boolean.TRUE;
    }

    @Override
    public Boolean upsertToolDeveloper(String adminName, OpToolDeveloperInfoReqVO reqVO) {
        log.info("#upsertToolDeveloper adminName:【{}】, toolName:【{}】, userId:【{}】, role:【{}】",
                adminName, reqVO.getToolName(), reqVO.getUserId(), reqVO.getRoleId());
        ToolDeveloperInfoEntity entity = toolDeveloperInfoRepository.findFirstByToolName(reqVO.getToolName());
        if (entity == null) {
            entity = new ToolDeveloperInfoEntity();
            entity.applyAuditInfoOnCreate(adminName);
            entity.setToolName(reqVO.getToolName());
        } else {
            entity.applyAuditInfoOnUpdate(adminName);
        }
        // 根据角色更新集合
        updateRoleSetByType(entity, reqVO.getUserId(), reqVO.getRoleId());
        // 保存库表
        toolDeveloperInfoRepository.save(entity);
        // Redis同步工具开发者信息
        handleToolAdmin(entity);
        // 同步蓝鲸开发者中心
        handleBkDeveloperCenter(entity);
        return Boolean.TRUE;
    }

    @Override
    public Boolean deleteToolDeveloper(String adminName, OpToolDeveloperInfoReqVO reqVO) {
        log.info("#deleteToolDeveloper adminName:【{}】, toolName:【{}】, userId:【{}】, role:【{}】",
                adminName, reqVO.getToolName(), reqVO.getUserId(), reqVO.getRoleId());
        ToolDeveloperInfoEntity entity = toolDeveloperInfoRepository.findFirstByToolName(reqVO.getToolName());
        if (entity == null) {
            return Boolean.FALSE;
        }
        entity.applyAuditInfoOnUpdate(adminName);
        // 删除工具开发者中对应用户
        deleteRoleSetByType(entity, reqVO.getUserId(), reqVO.getRoleId());
        // 保存库表
        toolDeveloperInfoRepository.save(entity);
        // Redis同步工具开发者信息
        handleToolAdmin(entity);
        // 同步蓝鲸开发者中心
        handleBkDeveloperCenter(entity);
        return Boolean.TRUE;
    }

    // 将更新的管理员回调到蓝鲸开发者中心
    private void handleBkDeveloperCenter(ToolDeveloperInfoEntity entity) {
        // 转换数据格式
        List<ToolMemberInfoVO> toolMemberInfoList = new ArrayList<>();

        entity.getDevelopers().forEach(dev ->
                toolMemberInfoList.add(createMember(dev, DEVELOPER_ROLE))
        );
        entity.getMasters().forEach(master ->
                toolMemberInfoList.add(createMember(master, MANAGER_ROLE))
        );
        String toolName = entity.getToolName();
        // 蓝鲸开发者中心
        bkPassClientApi.syncToolMember(toolName, toolMemberInfoList);
    }

    private ToolMemberInfoVO createMember(String username, UserRoleVO role) {
        ToolMemberInfoVO vo = new ToolMemberInfoVO();
        vo.setUsername(username);
        vo.setRole(role);
        return vo;
    }

    // 将开发者工具人员同步到redis
    private void handleToolAdmin(ToolDeveloperInfoEntity entity) {
        Set<String> allRoles = new HashSet<>();
        // 安全添加集合元素
        Optional.ofNullable(entity.getDevelopers()).ifPresent(allRoles::addAll);
        Optional.ofNullable(entity.getOwners()).ifPresent(allRoles::addAll);
        Optional.ofNullable(entity.getMasters()).ifPresent(allRoles::addAll);

        String key = RedisKeyConstants.PREFIX_TOOL_DEVELOPER + entity.getToolName();
        redisTemplate.delete(key);  // 清除旧缓存
        if (!allRoles.isEmpty()) {
            redisTemplate.opsForSet().add(key, allRoles.toArray(new String[0]));
        }
    }

    private void modifyRoleSet(ToolDeveloperInfoEntity entity, Integer role, Consumer<Set<String>> operation) {
        Set<String> roleSet = null;
        Consumer<Set<String>> setter = null;

        if (role == ComConstants.RoleType.DEVELOPER.getValue()) {
            roleSet = Optional.ofNullable(entity.getDevelopers()).orElseGet(HashSet::new);
            setter = entity::setDevelopers;
        } else if (role == ComConstants.RoleType.MASTER.getValue()) {
            roleSet = Optional.ofNullable(entity.getMasters()).orElseGet(HashSet::new);
            setter = entity::setMasters;
        } else if (role == ComConstants.RoleType.OWNER.getValue()) {
            roleSet = Optional.ofNullable(entity.getOwners()).orElseGet(HashSet::new);
            setter = entity::setOwners;
        }

        if (roleSet != null) {
            operation.accept(roleSet);
            setter.accept(roleSet);
        }

    }

    // 添加或更新用户
    private void updateRoleSetByType(ToolDeveloperInfoEntity entity, String userName, Integer role) {
        // 先清除所有角色组下用户，在添加指定角色，实现更新操作
        for (ComConstants.RoleType roleType : ComConstants.RoleType.values()) {
            deleteRoleSetByType(entity, userName, roleType.getValue());
        }
        // 添加或更新指定角色
        modifyRoleSet(entity, role, set -> set.add(userName));
    }

    // 删除指定角色组下用户
    private void deleteRoleSetByType(ToolDeveloperInfoEntity entity, String userName, Integer role) {
        modifyRoleSet(entity, role, set -> set.remove(userName));
    }
}
