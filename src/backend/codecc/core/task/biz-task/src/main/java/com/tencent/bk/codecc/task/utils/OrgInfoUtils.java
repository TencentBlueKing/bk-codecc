package com.tencent.bk.codecc.task.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.devops.common.api.OrgInfoVO;
import com.tencent.devops.common.api.codecc.util.JsonUtil;
import com.tencent.devops.common.api.pojo.ProjectVO;
import com.tencent.devops.common.client.pojo.AllProperties;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import com.tencent.devops.common.util.OkhttpUtils;
import com.tencent.devops.project.pojo.Result;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_BK_TOKEN;
import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_PROJECT_ID;

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
            ProjectVO projectVO = getBkCiProjectVO(projectId);
            if (projectVO == null) {
                return new OrgInfoVO();
            }
            return new OrgInfoVO(
                    StringUtils.isNumeric(projectVO.getBgId()) ? Integer.parseInt(projectVO.getBgId()) : -1,
                    StringUtils.isNumeric(projectVO.getBusinessLineId())
                            ? Integer.parseInt(projectVO.getBusinessLineId()) : -1,
                    StringUtils.isNumeric(projectVO.getDeptId()) ? Integer.parseInt(projectVO.getDeptId()) : -1,
                    StringUtils.isNumeric(projectVO.getCenterId()) ? Integer.parseInt(projectVO.getCenterId()) : -1,
                    -1
            );
        } catch (Exception e) {
            logger.error("getOrgInfoByProjectId cause error. projectId:{}", projectId, e);
        }
        return new OrgInfoVO();
    }

    /**
     * 获取蓝盾项目信息
     *
     * @param projectId 蓝盾任务id
     * @return ProjectVO
     */
    @Nullable
    public static ProjectVO getBkCiProjectVO(String projectId) {
        /* TODO 经确认devopsVersion 在1.x.x版本没有businessLineId，待升级到2.x.x版本后再使用client
        Client client = SpringContextUtil.Companion.getBean(Client.class);
        Result<ProjectVO> result = client.getDevopsService(ServiceProjectResource.class).get(projectId);
        if (result.isNotOk() || result.getData() == null) {
            return new OrgInfoVO();
        }
        ProjectVO projectVO = result.getData();
        */
        AllProperties allProperties = SpringContextUtil.Companion.getBean(AllProperties.class);
        String reqUrl = "https://" + allProperties.getDevopsDevUrl() + "/ms/project/api/service/projects/" + projectId;
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put(AUTH_HEADER_DEVOPS_BK_TOKEN, allProperties.getDevopsToken());
            headers.put(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId);
            headers.put("Accept", "application/json");
            headers.put("ContentType", "application/json");

            String result = OkhttpUtils.INSTANCE.doGet(reqUrl, headers);
            Result<ProjectVO> resultObj = JsonUtil.INSTANCE.to(result, new TypeReference<Result<ProjectVO>>() {
            });
            if (resultObj.isNotOk() || resultObj.getData() == null) {
                return null;
            }
            return resultObj.getData();
        } catch (Exception e) {
            logger.error("getBkCiProjectVO failed! default return null, exception:{" + e.getMessage() + "}", e);
            return null;
        }
    }
}
