package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.task.vo.AdminPrivilegeInfoVO;
import com.tencent.devops.common.constant.ComConstants;

import java.util.List;

/**
 * 新-管理员授权接口
 */
public interface AdminPrivilegeService {

    /**
     * 触发-自动更新管理员状态
     * @return bool
     */
    Boolean triggerRefreshAdminPrivilegeStatus();

    /**
     * 更新插入bg/平台管理员
     * @param reqVO vo
     * @return bool
     */
    Boolean upsertAdminPrivilegeInfo(AdminPrivilegeInfoVO reqVO);

    /**
     * 更新bg/平台管理员 启用状态
     * @param userName 更新人
     * @param userId 用户id
     * @param status 状态
     * @return bool
     */
    Boolean updateAdminPrivilegeStatus(String userName, String userId, Boolean status);

    /**
     * 续期管理员权限日志
     * @param userName 更新人
     * @param userId 更新的用户id
     * @param validityDays 申请的增加的权限天数
     * @return bool
     */
    Boolean renewalAdminPrivilegeValidityDays(String userName, String userId, Integer validityDays);

    /**
     * 批量更新BG/平台管理员状态：将已过期的
     */
    void batchUpdateAdminPrivilegeStatus();

    /**
     * 初始化服务器管理员
     * @return bool
     */
    Boolean initializationBgAndGlobalAdminMember();


    /**
     * 查询不同类型管理员id列表
     * @param privilegeType 管理员列表
     * @return list
     */
    List<String> queryAdminMemberByType(ComConstants.PrivilegeType privilegeType);
}
