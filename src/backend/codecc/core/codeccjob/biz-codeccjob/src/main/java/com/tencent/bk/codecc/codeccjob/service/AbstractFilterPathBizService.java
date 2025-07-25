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

package com.tencent.bk.codecc.codeccjob.service;

import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.IBizService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 路径屏蔽的抽象类
 *
 * @version V1.0
 * @date 2019/12/2
 */
@Slf4j
public abstract class AbstractFilterPathBizService implements IBizService<FilterPathInputVO> {

    /**
     * 分页查询告警列表，每页10000
     */
    public static final int PAGE_SIZE = 10000;

    @Autowired
    private AfterFilterPathDoneOpsService afterFilterPathDoneOpsService;

    /**
     * 获取更新状态
     *
     * @param filterPathInput
     * @param orgStatus
     * @return
     */
    protected int getUpdateStatus(FilterPathInputVO filterPathInput, int orgStatus) {
        return filterPathInput.getAddFile() ?
                (orgStatus | ComConstants.TaskFileStatus.PATH_MASK.value()) :
                ((orgStatus & ComConstants.TaskFileStatus.PATH_MASK.value()) > 0 ?
                        orgStatus - ComConstants.TaskFileStatus.PATH_MASK.value() :
                        orgStatus
                );
    }

    protected void doAfterFilterPathDone(long taskId, String toolName, List defectList) {
        afterFilterPathDoneOpsService.doAfterFilterPathDone(taskId, toolName, defectList);
    }
}
