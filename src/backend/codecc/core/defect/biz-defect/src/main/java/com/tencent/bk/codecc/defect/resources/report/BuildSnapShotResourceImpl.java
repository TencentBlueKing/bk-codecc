package com.tencent.bk.codecc.defect.resources.report;

import com.tencent.bk.codecc.defect.api.report.BuildSnapShotResource;
import com.tencent.bk.codecc.defect.service.SnapShotService;
import com.tencent.bk.codecc.defect.vo.common.SnapShotVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

@RestResource
public class BuildSnapShotResourceImpl implements BuildSnapShotResource {

    @Autowired
    private SnapShotService snapShotService;

    @Override
    public Result<SnapShotVO> get(String projectId, Long taskId, String buildId) {
        return new Result<>(snapShotService.getTaskToolBuildSnapShot(projectId, buildId, taskId));
    }

    @Override
    public Result<Boolean> getMetadataReportStatus(String projectId, Long taskId, String buildId) {
        return new Result<>(snapShotService.getSnapShotVO(projectId, taskId, buildId).getMetadataReport());
    }
}
