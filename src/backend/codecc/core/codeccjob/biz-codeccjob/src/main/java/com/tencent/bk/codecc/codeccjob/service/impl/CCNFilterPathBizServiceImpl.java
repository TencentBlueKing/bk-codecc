/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.codeccjob.service.impl;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.CCNDefectDao;
import com.tencent.bk.codecc.codeccjob.service.AbstractFilterPathBizService;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ccn类工具的路径屏蔽
 *
 * @version V1.0
 * @date 2019/11/1
 */
@Service("CCNFilterPathBizService")
@Slf4j
public class CCNFilterPathBizServiceImpl extends AbstractFilterPathBizService {

    @Autowired
    private CCNDefectDao ccnDefectDao;

    @Override
    public Result processBiz(FilterPathInputVO filterPathInputVO) {
        // CCN类屏蔽路径
        // 不需要查询已修复的告警
        Set<Integer> excludeStatusSet = Sets.newHashSet(ComConstants.DefectStatus.FIXED.value(),
                ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.FIXED.value());
        String lastId = null;
        int size;
        do {
            List<CCNDefectEntity> ccnFileInfoList = ccnDefectDao.findDefectsByFilePath(filterPathInputVO.getTaskId(),
                    excludeStatusSet, filterPathInputVO.getFilterPaths(), PAGE_SIZE, lastId);
            size = ccnFileInfoList.size();
            if (size > 0) {
                StringBuilder builder = new StringBuilder();
                filterPathInputVO.getFilterPaths().forEach(builder::append);
                List<CCNDefectEntity> needUpdateDefectList = new ArrayList<>();
                long currTime = System.currentTimeMillis();
                ccnFileInfoList.forEach(defect -> {
                    int status = defect.getStatus();
                    if (filterPathInputVO.getAddFile()) {
                        status = status | ComConstants.DefectStatus.PATH_MASK.value();
                        defect.setMaskPath(builder.toString());
                        if (defect.getExcludeTime() == null || defect.getExcludeTime() == 0) {
                            defect.setExcludeTime(currTime);
                        }
                    } else {
                        if ((status & ComConstants.DefectStatus.PATH_MASK.value()) > 0) {
                            status = status - ComConstants.DefectStatus.PATH_MASK.value();
                            if (status < ComConstants.DefectStatus.PATH_MASK.value()) {
                                defect.setMaskPath(null);
                                defect.setExcludeTime(0L);
                            }
                        }
                    }

                    if (defect.getStatus() != status) {
                        defect.setStatus(status);
                        needUpdateDefectList.add(defect);
                    }
                });

                ccnDefectDao.batchUpdateDefectStatusExcludeBit(filterPathInputVO.getTaskId(), needUpdateDefectList);

                doAfterFilterPathDone(filterPathInputVO.getTaskId(), filterPathInputVO.getToolName(),
                        needUpdateDefectList);

                lastId = ccnFileInfoList.get(size - 1).getEntityId();
            }
        } while (size == PAGE_SIZE);

        return new Result(CommonMessageCode.SUCCESS);
    }
}
