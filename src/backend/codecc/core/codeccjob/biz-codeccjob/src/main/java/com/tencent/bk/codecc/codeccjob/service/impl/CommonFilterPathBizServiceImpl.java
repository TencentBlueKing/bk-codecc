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
import com.tencent.bk.codecc.codeccjob.dao.mongotemplate.DefectDao;
import com.tencent.bk.codecc.codeccjob.dao.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.codeccjob.service.AbstractFilterPathBizService;
import com.tencent.bk.codecc.defect.api.ServiceDefectRestResource;
import com.tencent.bk.codecc.defect.mapping.DefectConverter;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.PathUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kotlin.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 通用工具的路径屏蔽
 *
 * @version V1.0
 * @date 2019/11/1
 */
@Service("CommonFilterPathBizService")
@Slf4j
public class CommonFilterPathBizServiceImpl extends AbstractFilterPathBizService
{
    @Autowired
    private DefectDao defectDao;
    @Autowired
    private Client client;
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;
    @Autowired
    private DefectConverter defectConverter;

    @Override
    public Result processBiz(FilterPathInputVO filterPathInputVO)
    {
        Long taskId = filterPathInputVO.getTaskId();
        String toolName = filterPathInputVO.getToolName();

        boolean isMigrationSuccessful = Boolean.TRUE.equals(
                client.get(ServiceDefectRestResource.class).commonToLintMigrationSuccessful(taskId).getData()
        );

        // 不需要查询已修复的告警
        Set<Integer> excludeStatusSet = Sets.newHashSet(ComConstants.DefectStatus.FIXED.value(),
                ComConstants.DefectStatus.NEW.value() | ComConstants.DefectStatus.FIXED.value());
        String lastId = null;
        int size;
        do {
            List<CommonDefectEntity> defectList =
                    getDefectList(isMigrationSuccessful, filterPathInputVO, taskId, toolName, excludeStatusSet, lastId);

            size = defectList.size();
            if (size > 0) {
                StringBuilder builder = new StringBuilder();
                filterPathInputVO.getFilterPaths().forEach(builder::append);
                List<CommonDefectEntity> needUpdateDefectList = new ArrayList<>();
                long currTime = System.currentTimeMillis();
                defectList.forEach(defect -> {
                    int status = defect.getStatus();
                    if (filterPathInputVO.getAddFile()) {
                        status = status | ComConstants.DefectStatus.PATH_MASK.value();
                        defect.setMaskPath(builder.toString());
                        if (defect.getExcludeTime() == 0) {
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

                if (isMigrationSuccessful) {
                    lintDefectV2Dao.batchUpdateDefectStatusExcludeBit(
                            taskId, defectConverter.commonToLint(needUpdateDefectList)
                    );
                } else {
                    defectDao.batchUpdateDefectStatusExcludeBit(taskId, needUpdateDefectList);
                }

                doAfterFilterPathDone(filterPathInputVO.getTaskId(), filterPathInputVO.getToolName(), needUpdateDefectList);

                lastId = defectList.get(size - 1).getEntityId();
            }
        } while (size == PAGE_SIZE);

        return new Result(CommonMessageCode.SUCCESS);
    }

    private List<CommonDefectEntity> getDefectList(
            boolean isMigrationSuccessful, FilterPathInputVO filterPathInputVO, Long taskId,
            String toolName, Set<Integer> excludeStatusSet, String lastId
    ) {
        if (isMigrationSuccessful) {
            return defectConverter.lintToCommon(
                    lintDefectV2Dao.findDefectsByFilePath(
                            taskId, toolName, excludeStatusSet, filterPathInputVO.getFilterPaths(), PAGE_SIZE, lastId
                    )
            );
        }

        return defectDao.findDefectsByFilePath(
                taskId, toolName, excludeStatusSet, filterPathInputVO.getFilterPaths(), PAGE_SIZE, lastId
        );
    }

    protected Boolean checkIfMaskByPath(CommonDefectEntity defectEntity, Set<String> filterPaths)
    {
        Pair<Boolean, String> pair = PathUtils.checkIfMaskByPath(defectEntity.getFilePath(), filterPaths);
        if (pair.getFirst()) {
            defectEntity.setMaskPath(pair.getSecond());
        }
        return pair.getFirst();
    }
}
