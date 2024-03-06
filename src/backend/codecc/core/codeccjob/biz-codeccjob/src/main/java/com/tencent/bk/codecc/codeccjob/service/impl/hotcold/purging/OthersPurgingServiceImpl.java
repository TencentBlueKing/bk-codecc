package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.purging;

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.BuildRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.CodeRepoInfoRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.FileDefectGatherRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.RedLineRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.TaskLogOverviewRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.TaskLogRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.TaskPersonalStatisticRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.ToolBuildStackRepository;
import com.tencent.bk.codecc.defect.model.TaskLogOverviewEntity;
import com.tencent.devops.common.constant.ComConstants.ColdDataPurgingType;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * 其他清理
 */
@Service
public class OthersPurgingServiceImpl extends AbstractDefectPurgingTemplate {

    @Autowired
    private TaskLogRepository taskLogRepository;
    @Autowired
    private TaskLogOverviewRepository taskLogOverviewRepository;
    @Autowired
    private ToolBuildInfoRepository toolBuildInfoRepository;
    @Autowired
    private ToolBuildStackRepository toolBuildStackRepository;
    @Autowired
    private CodeRepoInfoRepository codeRepoInfoRepository;
    @Autowired
    private FileDefectGatherRepository fileDefectGatherRepository;
    @Autowired
    private TaskPersonalStatisticRepository taskPersonalStatisticRepository;
    @Autowired
    private BuildRepository buildRepository;
    @Autowired
    private RedLineRepository redLineRepository;

    @Override
    protected long purgeCore(long taskId) {
        // 因t_build中不记录task_id，先从task_log_overview取出关联build_id
        List<String> buildIdList = taskLogOverviewRepository.findByTaskId(taskId).stream()
                .map(TaskLogOverviewEntity::getBuildId)
                .filter(x -> !ObjectUtils.isEmpty(x))
                .collect(Collectors.toList());

        long buildRepositoryDelCount = 0L;
        long redLineDelCount = 0L;
        if (!CollectionUtils.isEmpty(buildIdList)) {
            buildRepositoryDelCount = buildRepository.deleteByBuildIdIn(buildIdList);
            redLineDelCount = redLineRepository.deleteByBuildIdIn(buildIdList);
        }

        return buildRepositoryDelCount + redLineDelCount
                + taskLogRepository.deleteByTaskId(taskId)
                + toolBuildInfoRepository.deleteByTaskId(taskId)
                + toolBuildStackRepository.deleteByTaskId(taskId)
                + codeRepoInfoRepository.deleteByTaskId(taskId)
                + fileDefectGatherRepository.deleteByTaskId(taskId)
                + taskPersonalStatisticRepository.deleteByTaskId(taskId)
                // 清理逻辑中taskLogOverview为最后清理，利于异常后再次幂等处理
                + taskLogOverviewRepository.deleteByTaskId(taskId);
    }

    @Override
    public ColdDataPurgingType coldDataPurgingType() {
        return ColdDataPurgingType.OTHERS;
    }


    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }
}
