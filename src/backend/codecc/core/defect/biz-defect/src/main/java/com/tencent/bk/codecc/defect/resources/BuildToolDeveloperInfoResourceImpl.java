package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.BuildToolDeveloperInfoResource;
import com.tencent.bk.codecc.defect.dao.core.mongorepository.ToolDeveloperInfoRepository;
import com.tencent.bk.codecc.defect.model.ToolDeveloperInfoEntity;
import com.tencent.bk.codecc.defect.vo.ToolMemberInfoVO;
import com.tencent.bk.codecc.defect.vo.developer.ToolDeveloperInfoVO;
import com.tencent.devops.common.api.ToolTestConstants;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
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
    ToolDeveloperInfoRepository toolDeveloperInfoRepository;

    @Override
    public Result<ToolDeveloperInfoVO> getPermissionInfo(String toolName) {
        ToolDeveloperInfoEntity entity = toolDeveloperInfoRepository.findFirstByToolName(toolName);

        ToolDeveloperInfoVO result = new ToolDeveloperInfoVO();
        if (entity == null) {
            return new Result<>(result);
        }

        BeanUtils.copyProperties(entity, result);

        return new Result<>(result);
    }

    @Override
    public Result<Boolean> syncToolMembers(String toolName, List<ToolMemberInfoVO> toolMemberInfoList) {
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

        return new Result<>(true);
    }
}
