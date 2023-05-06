package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.UserManageRestResource;
import com.tencent.bk.codecc.task.service.UserManageService;
import com.tencent.bk.codecc.task.vo.DevopsProjectVO;
import com.tencent.bk.codecc.task.vo.UserVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import java.util.List;
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

    @Override
    public Result<UserVO> getInfo(String userId) {
        return userManageService.getInfo(userId);
    }

    @Override
    public Result<List<DevopsProjectVO>> getProjectList(String userId, String accessToken) {
        return new Result<>(userManageService.getProjectList(userId, accessToken));
    }
}
