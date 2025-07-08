/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 Tencent. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.BuildTaskRestResource;
import com.tencent.bk.codecc.defect.dto.CodeLineModel;
import com.tencent.bk.codecc.defect.service.BuildSnapshotService;
import com.tencent.bk.codecc.defect.service.ICLOCQueryCodeLineService;
import com.tencent.bk.codecc.defect.vo.common.BuildWithBranchVO;
import com.tencent.bk.codecc.defect.vo.common.TaskCodeLineVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

@RestResource
public class BuildTaskRestResourceImpl implements BuildTaskRestResource {

    @Autowired
    private BuildSnapshotService buildSnapshotService;
    @Autowired
    private ICLOCQueryCodeLineService iclocQueryCodeLineService;

    @Override
    public Result<List<BuildWithBranchVO>> queryBuildInfosWithBranches(Long taskId) {
        return new Result<>(buildSnapshotService.getRecentBuildSnapshotSummary(taskId));
    }

    @Override
    public Result<List<TaskCodeLineVO>> getTaskCodeLineInfo(Long taskId) {
        List<CodeLineModel> codeLineModels = iclocQueryCodeLineService.queryCodeLineByTaskId(taskId);
        if (CollectionUtils.isEmpty(codeLineModels)) {
            return new Result<>(Collections.emptyList());
        }
        List<TaskCodeLineVO> taskCodeLineVOS = new LinkedList<>();
        for (CodeLineModel codeLineModel : codeLineModels) {
            taskCodeLineVOS.add(new TaskCodeLineVO(codeLineModel.getBlankLine(), codeLineModel.getCodeLine(),
                    codeLineModel.getCommentLine(), codeLineModel.getLanguage(), codeLineModel.getLangValue(),
                    codeLineModel.getFileNum()));
        }
        return new Result<>(taskCodeLineVOS);
    }
}
