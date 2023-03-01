package com.tencent.bk.codecc.defect.service.impl.git;


import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.service.git.GitRepoApiService;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GitRepoApiServiceImpl implements GitRepoApiService {


    @Override
    public void addLintGitCodeAnalyzeComment(
            TaskDetailVO taskDetailVO,
            String buildId,
            String buildNum,
            String toolName,
            Set<String> currentFileSet,
            List<LintDefectV2Entity> newDefectList
    ) {

    }

    @Override
    public void addCcnGitCodeAnalyzeComment(
            TaskDetailVO taskDetailVO,
            String buildId,
            String buildNum,
            String toolName,
            Set<String> currentFileSet,
            List<CCNDefectEntity> newDefectList
    ) {

    }
}
