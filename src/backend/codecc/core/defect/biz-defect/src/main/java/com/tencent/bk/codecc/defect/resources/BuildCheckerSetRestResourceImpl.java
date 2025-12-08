package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.BuildCheckerSetRestResource;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.ICheckerSetIntegratedBizService;
import com.tencent.bk.codecc.defect.service.ICheckerSetManageBizService;
import com.tencent.bk.codecc.defect.service.ICheckerSetQueryBizService;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.integrated.ToolCheckerSetToStatusVo;
import com.tencent.devops.common.api.checkerset.CheckerSetRelationshipVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import com.tencent.devops.common.web.RestResource;
import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.ws.rs.PathParam;

@RestResource
public class BuildCheckerSetRestResourceImpl implements BuildCheckerSetRestResource {

    @Autowired
    private ICheckerSetManageBizService checkerSetManageBizService;

    @Autowired
    private ICheckerSetIntegratedBizService checkerSetIntegratedBizService;

    @Autowired
    private ICheckerSetQueryBizService checkerSetQueryBizService;

    @Autowired
    private CheckerService checkerService;

    @Override
    public Result<Boolean> setRelationships(String user, String type, String projectId, Long taskId,
            List<CheckerSetVO> checkerSetVOList) {
        CheckerSetRelationshipVO checkerSetRelationshipVO = new CheckerSetRelationshipVO();
        checkerSetRelationshipVO.setType(type);
        checkerSetRelationshipVO.setTaskId(taskId);
        checkerSetRelationshipVO.setProjectId(projectId);
        checkerSetVOList.forEach(checkerSet -> {
            checkerSetRelationshipVO.setVersion(checkerSet.getVersion());
            checkerSetManageBizService.setRelationships(checkerSet.getCheckerSetId(), user, checkerSetRelationshipVO);
        });
        return new Result<>(true);
    }

    @Override
    public Result<String> updateToolCheckerSetToStatus(
            String user,
            String buildId,
            String toolName,
            ToolIntegratedStatus fromStatus,
            ToolIntegratedStatus toStatus,
            ToolCheckerSetToStatusVo toolCheckerSetToStatusVo
    ) {
        return new Result<>(
                checkerSetIntegratedBizService.updateToStatus(
                        toolName,
                        buildId,
                        fromStatus,
                        toStatus,
                        user,
                        toolCheckerSetToStatusVo.getCheckerSetIds(),
                        toolCheckerSetToStatusVo.getCheckerIds()
                )
        );
    }

    @Override
    public Result<String> revertToolCheckerSetStatus(String user,
            String toolName,
            ComConstants.ToolIntegratedStatus status,
            Set<String> checkerSetIds) {
        return new Result<>(checkerSetIntegratedBizService.revertStatus(toolName, status, user, checkerSetIds));
    }

    @Override
    public Result<List<CheckerDetailVO>> queryCheckerByToolName(String toolName) {
        return new Result<>(checkerService.queryCheckerByTool(toolName));
    }

    @Override
    public Result<Set<String>> getPackageCheckerNameByType(String type) {
        return new Result<>(checkerSetQueryBizService.getPackageCheckerNameByType(type));
    }
}
