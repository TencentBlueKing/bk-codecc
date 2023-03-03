package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.task.api.ServiceGrayToolProjectResource;
import com.tencent.bk.codecc.task.vo.GrayTaskStatVO;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@RestResource
public class ServiceGrayToolProjectResourceImpl implements ServiceGrayToolProjectResource {

    @Override
    public Result<GrayToolProjectVO> getGrayToolProjectInfoByProjectId(String projectId) {
        return new Result<>(null);
    }

    @Override
    public Result<List<GrayToolProjectVO>> getGrayToolProjectByProjectIds(Set<String> projectIdSet) {
        return new Result<>(Collections.emptyList());
    }

    @Override
    public Result<Boolean> processGrayReport(Long taskId, String buildId, GrayTaskStatVO grayTaskStatVO) {
        return new Result<>(true);
    }
}
