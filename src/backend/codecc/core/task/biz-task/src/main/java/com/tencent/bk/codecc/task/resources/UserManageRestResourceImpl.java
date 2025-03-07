package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.UserManageRestResource;
import com.tencent.bk.codecc.task.service.UserManageService;
import com.tencent.bk.codecc.task.vo.DevopsProjectVO;
import com.tencent.bk.codecc.task.vo.UserVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.web.RestResource;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 用户管理接口的实现类
 *
 * @version V1.0
 * @date 2019/4/19
 */
@RestResource
public class UserManageRestResourceImpl implements UserManageRestResource {

    private static Logger logger = LoggerFactory.getLogger(UserManageRestResourceImpl.class);

    @Autowired
    private UserManageService userManageService;

    @Autowired
    private AuthExPermissionApi authExPermissionApi;

    @Override
    public Result<UserVO> getInfo(String userId) {
        return userManageService.getInfo(userId);
    }

    @Override
    public Result<List<DevopsProjectVO>> getProjectList(String userId, String accessToken) {
        return new Result<>(userManageService.getProjectList(userId, accessToken));
    }

    @Override
    public Result<Boolean> isProjectManager(String projectId, String userName) {
        if (StringUtils.isBlank(projectId) || StringUtils.isBlank(userName)) {
            return new Result<>(false);
        }
        boolean projectManager = authExPermissionApi.authProjectMultiManager(projectId, userName);
        return new Result<>(projectManager);
    }
}
