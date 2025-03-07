package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.ServiceGrayToolProjectResource;
import com.tencent.bk.codecc.task.service.GrayToolProjectService;
import com.tencent.bk.codecc.task.vo.GrayTaskStatVO;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.condition.CommunityCondition;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;

@RestResource
@Conditional(CommunityCondition.class)
public class ServiceGrayToolProjectResourceImpl implements ServiceGrayToolProjectResource {

    @Autowired
    private GrayToolProjectService grayToolProjectService;

    @Override
    public Result<List<GrayToolProjectVO>> getGrayToolProjectDetail(String projectId) {
        return Result.success(grayToolProjectService.getGrayToolListByProjectId(projectId));
    }

    @Override
    public Result<List<GrayToolProjectVO>> getGrayToolProjectByProjectIds(Set<String> projectIdSet) {
        return new Result<>(grayToolProjectService.findGrayToolProjectByProjectIds(projectIdSet));
    }

    @Override
    public Result<Boolean> processGrayReport(Long taskId, String buildId, String toolName,
            GrayTaskStatVO grayTaskStatVO) {
        return new Result<>(true);
    }
}
