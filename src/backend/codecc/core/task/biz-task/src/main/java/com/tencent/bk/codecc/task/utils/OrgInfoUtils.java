package com.tencent.bk.codecc.task.utils;

import com.tencent.devops.common.api.OrgInfoVO;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import com.tencent.devops.project.api.service.ServiceProjectResource;
import com.tencent.devops.project.pojo.ProjectVO;
import com.tencent.devops.project.pojo.Result;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrgInfoUtils {

    private static final Logger logger = LoggerFactory.getLogger(OrgInfoUtils.class);

    /**
     * 获取项目的组织信息
     *
     * @param projectId
     * @return
     */
    public static OrgInfoVO getOrgInfoByProjectId(String projectId) {
        try {
            Client client = SpringContextUtil.Companion.getBean(Client.class);
            Result<ProjectVO> result = client.getDevopsService(ServiceProjectResource.class).get(projectId);
            if (result.isNotOk() || result.getData() == null) {
                return new OrgInfoVO();
            }
            ProjectVO projectVO = result.getData();
            return new OrgInfoVO(
                    StringUtils.isNumeric(projectVO.getBgId()) ? Integer.parseInt(projectVO.getBgId()) : -1,
                    StringUtils.isNumeric(projectVO.getDeptId()) ? Integer.parseInt(projectVO.getDeptId()) : -1,
                    StringUtils.isNumeric(projectVO.getCenterId()) ? Integer.parseInt(projectVO.getCenterId()) : -1,
                    -1
            );
        } catch (Exception e) {
            logger.error("getOrgInfoByProjectId cause error. projectId:{}", projectId, e);
        }
        return new OrgInfoVO();
    }

}
