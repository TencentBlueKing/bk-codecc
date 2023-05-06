package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.task.vo.DevopsProjectOrgVO;
import com.tencent.bk.codecc.task.vo.DevopsProjectVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import java.util.List;

/**
 * 用户管理逻辑处理
 *
 * @version V1.0
 * @date 2019/4/23
 */
public interface UserManageService {
    Result getInfo(String userId);

    /**
     * 获取项目信息
     *
     * @param accessToken
     * @return
     */
    List<DevopsProjectVO> getProjectList(String userId, String accessToken);

    /**
     * 获取蓝盾项目组织架构信息
     *
     * @param projectId 蓝盾项目ID
     * @return org
     */
    DevopsProjectOrgVO getDevopsProjectOrg(String projectId);
}
