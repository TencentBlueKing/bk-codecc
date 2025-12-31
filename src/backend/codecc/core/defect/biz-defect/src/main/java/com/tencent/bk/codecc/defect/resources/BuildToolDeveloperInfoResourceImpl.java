package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.BuildToolDeveloperInfoResource;
import com.tencent.bk.codecc.defect.service.ToolDeveloperInfoService;
import com.tencent.bk.codecc.defect.vo.ToolMemberInfoVO;
import com.tencent.bk.codecc.defect.vo.developer.ToolDeveloperInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * BuildToolDeveloperInfoResource 的实现类
 *
 * @date 2024/08/08
 */
@RestResource
@Slf4j
public class BuildToolDeveloperInfoResourceImpl implements BuildToolDeveloperInfoResource {

    @Autowired
    private ToolDeveloperInfoService toolDeveloperInfoService;

    @Override
    public Result<ToolDeveloperInfoVO> getPermissionInfo(String toolName) {
        return new Result<>(toolDeveloperInfoService.getPermissionInfo(toolName));
    }

    @Override
    public Result<Boolean> syncToolMembers(String toolName, List<ToolMemberInfoVO> toolMemberInfoList) {
        return new Result<>(toolDeveloperInfoService.syncToolMembers(toolName, toolMemberInfoList));
    }
}
