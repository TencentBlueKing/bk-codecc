package com.tencent.bk.codecc.defect.service.git;

import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.vo.customtool.ScmBlameVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface GitRepoApiService {

    void addLintGitCodeAnalyzeComment(
            TaskDetailVO taskDetailVO,
            String buildId,
            String buildNum,
            String toolName,
            Set<String> currentFileSet,
            List<LintDefectV2Entity> newDefectList
    );

    void addCcnGitCodeAnalyzeComment(
            TaskDetailVO taskDetailVO,
            String buildId,
            String buildNum,
            String toolName,
            Set<String> currentFileSet,
            List<CCNDefectEntity> newDefectList
    );
}
