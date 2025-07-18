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

package com.tencent.bk.codecc.defect.bkcheck.resource;

import com.tencent.bk.codecc.defect.bkcheck.api.BuildBkcheckTaskLogRestResource;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 获取任务分析记录接口实现
 * @author jamikxu
 * @version V1.0
 * @date 2023/2/20
 */
@RestResource
public class BuildBkcheckTaskLogRestResourceImpl implements BuildBkcheckTaskLogRestResource {

    @Autowired
    private TaskLogService taskLogService;

    @Override
    public Result<TaskLogVO> getBuildTaskLog(long taskId, String toolName) {
        return new Result<>(taskLogService.getLastTaskLogByTaskIdAndToolName(taskId, toolName));
    }
}
