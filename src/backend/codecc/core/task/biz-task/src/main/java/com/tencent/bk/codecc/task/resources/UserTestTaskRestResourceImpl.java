package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.UserTestTaskRestResource;
import com.tencent.bk.codecc.task.vo.BatchTestResultVO;
import com.tencent.bk.codecc.task.vo.DevopsProjectVO;
import com.tencent.bk.codecc.task.vo.QueryTestReportReqVO;
import com.tencent.bk.codecc.task.vo.RecommendedThresholdVO;
import com.tencent.bk.codecc.task.vo.RepoScaleVO;
import com.tencent.bk.codecc.task.vo.StartRandomTestReqVO;
import com.tencent.bk.codecc.task.vo.StartTestReqVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskIdVO;
import com.tencent.bk.codecc.task.vo.TestTaskReportVO;
import com.tencent.bk.codecc.task.vo.ToolBasicInfoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@RestResource
@Slf4j
public class UserTestTaskRestResourceImpl implements UserTestTaskRestResource {

    @Override
    public Result<String> getLatestVersion(String toolName) {
        return new Result<>(null);
    }

    @Override
    public Result<TaskIdVO> registerTestTask(TaskDetailVO taskDetailVO, String projectId, String userName) {
        return new Result<>(null);
    }

    @Override
    public Result<List<RepoScaleVO>> getRepoScaleList() {
        return new Result<>(null);
    }

    @Override
    public Result<RecommendedThresholdVO> getRecommendedThreshold() {
        return new Result<>(null);
    }

    @Override
    public Result<List<DevopsProjectVO>> getProjectIdsByUserName(String userId) {
        return new Result<>(null);
    }

    @Override
    public Result<ToolBasicInfoVO> getBasicInfo(String toolName) {
        return new Result<>(null);
    }

    @Override
    public Result<List<TaskBaseVO>> listTask(String toolName, String projectId) {
        return new Result<>(null);
    }

    @Override
    public Result<Boolean> hasTestTask(String projectId) {
        return new Result<>(false);
    }

    @Override
    public Result<Boolean> deleteTestTask(String userId, String projectId, Long taskId) {
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> start(String userId, String toolName, StartTestReqVO startTestReqVO) {
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> startRandomTest(String userName, StartRandomTestReqVO request) {
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> getTestStatus(String toolName, String version) {
        return new Result<>(true);
    }

    @Override
    public Result<BatchTestResultVO> getBatchTestResult(String toolName, String version, Integer stage) {
        return new Result<>(null);
    }

    @Override
    public Result<List<String>> listVersion(String toolName, Integer stage) {
        return new Result<>(null);
    }

    @Override
    public Result<List<TestTaskReportVO>> listTestReport(String toolName, Integer stage,
            QueryTestReportReqVO queryTestReportReqVO, Integer pageNum, Integer pageSize) {
        return new Result<>(null);
    }

    @Override
    public Result<Integer> countTestReport(String toolName, Integer stage, QueryTestReportReqVO queryTestReportReqVO) {
        return new Result<>(0);
    }

    @Override
    public Result<Boolean> migrate(String userName, String env, String pdId, String toolName) {
        return new Result<>(null);
    }
}
