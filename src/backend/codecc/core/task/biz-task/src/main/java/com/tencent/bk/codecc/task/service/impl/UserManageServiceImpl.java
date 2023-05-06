package com.tencent.bk.codecc.task.service.impl;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_ACCESS_TOKEN;

import com.tencent.bk.codecc.task.service.UserManageService;
import com.tencent.bk.codecc.task.vo.DevopsProjectOrgVO;
import com.tencent.bk.codecc.task.vo.DevopsProjectVO;
import com.tencent.bk.codecc.task.vo.UserVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.project.api.service.ServiceProjectResource;
import com.tencent.devops.project.pojo.ProjectVO;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 用户管理逻辑处理实现类
 *
 * @version V1.0
 * @date 2019/4/25
 */
@Service
@Slf4j
public class UserManageServiceImpl implements UserManageService {

    @Autowired
    private Client client;

    @Override
    public Result<UserVO> getInfo(String userId) {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String bkToken = request.getHeader(AUTH_HEADER_DEVOPS_ACCESS_TOKEN);
        UserVO userVO = new UserVO();
        userVO.setUsername(userId);
        userVO.setAuthenticated(true);
        userVO.setBkToken(bkToken);

        return new Result<>(userVO);
    }

    @Override
    public List<DevopsProjectVO> getProjectList(String userId, String accessToken) {
        com.tencent.devops.project.pojo.Result<List<ProjectVO>> projectResult =
                client.getDevopsService(ServiceProjectResource.class).list(accessToken);
        if (projectResult.isNotOk() || null == projectResult.getData()) {
            log.error("get project list fail!");
            throw new CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR);
        }
        List<ProjectVO> projectVOList = projectResult.getData();
        return projectVOList.stream().
                map(projectVO ->
                        new DevopsProjectVO(
                                projectVO.getProjectId(),
                                projectVO.getProjectName(),
                                projectVO.getProjectCode(),
                                projectVO.getProjectType()
                        )
                ).
                collect(Collectors.toList());
    }

    @Override
    public DevopsProjectOrgVO getDevopsProjectOrg(String projectId) {
        DevopsProjectOrgVO projectOrgVO = new DevopsProjectOrgVO();
        com.tencent.devops.project.pojo.Result<ProjectVO> projectResult =
                client.getShortRunDevopsService(ServiceProjectResource.class).get(projectId);

        if (projectResult.isNotOk() || projectResult.getData() == null) {
            log.error("getDevopsProject fail! [{}]", projectId);
            return projectOrgVO;
        }

        ProjectVO projectVO = projectResult.getData();
        String bgId = projectVO.getBgId();
        if (StringUtils.isBlank(bgId)) {
            log.error("getDevopsProject bgId is empty: [{}]", projectId);
            return projectOrgVO;
        }

        projectOrgVO.setBgId(Integer.parseInt(bgId));
        String deptId = projectVO.getDeptId();
        projectOrgVO.setDeptId(Integer.parseInt(StringUtils.isBlank(deptId) ? "0" : deptId));
        String centerId = projectVO.getCenterId();
        projectOrgVO.setCenterId(Integer.parseInt(StringUtils.isBlank(centerId) ? "0" : centerId));

        return projectOrgVO;
    }
}
