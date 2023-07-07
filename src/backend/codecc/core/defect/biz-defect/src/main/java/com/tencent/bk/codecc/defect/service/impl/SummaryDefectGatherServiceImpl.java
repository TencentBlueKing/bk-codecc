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

package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.FileDefectGatherRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.SummaryDefectGatherRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.SummaryDefectGatherDao;
import com.tencent.bk.codecc.defect.model.FileDefectGatherEntity;
import com.tencent.bk.codecc.defect.model.SummaryDefectGatherEntity;
import com.tencent.bk.codecc.defect.model.SummaryGatherInfo;
import com.tencent.bk.codecc.defect.service.FileDefectGatherService;
import com.tencent.bk.codecc.defect.service.SummaryDefectGatherService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.FileDefectGatherVO;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件告警收敛业务实现类
 *
 * @version V1.0
 * @date 2020/5/26
 */
@Service
@Slf4j
public class SummaryDefectGatherServiceImpl implements SummaryDefectGatherService {
    @Autowired
    private SummaryDefectGatherRepository summaryDefectGatherRepository;

    @Autowired
    private SummaryDefectGatherDao summaryDefectGatherDao;

    @Override
    public FileDefectGatherVO getSummaryDefectGather(Long taskId, String toolName, String dimension) {
        log.info("getSummaryDefectGather taskId:{},toolName:{},dimension:{}",
                taskId, toolName, dimension);
        SummaryDefectGatherEntity gather = summaryDefectGatherRepository
                .findFirstByTaskIdAndToolNameAndStatus(taskId, toolName, ComConstants.DefectStatus.NEW.value());
        if (gather == null) {
            log.info("getSummaryDefectGather taskId:{},toolName:{},dimension:{} return null",
                    taskId, toolName, dimension);
            return null;
        }
        FileDefectGatherVO fileDefectGatherVO = new FileDefectGatherVO();
        fileDefectGatherVO.setFileName(gather.getFileName());
        fileDefectGatherVO.setDefectCount(gather.getDefectCount());
        fileDefectGatherVO.setFileCount(gather.getFileCount());
        log.info("getSummaryDefectGather success, fileCount:{}, defectCount:{}",
                fileDefectGatherVO.getFileCount(), fileDefectGatherVO.getDefectCount());
        return fileDefectGatherVO;
    }

    @Override
    public void saveSummaryDefectGather(Long taskId, String toolName, SummaryGatherInfo gatherInfo) {
        SummaryDefectGatherEntity oldGather = summaryDefectGatherRepository
                .findFirstByTaskIdAndToolName(taskId, toolName);
        if (gatherInfo == null && oldGather != null) {
            summaryDefectGatherDao.updateStatus(taskId, toolName, ComConstants.Status.DISABLE.value());
        } else if (gatherInfo != null) {
            summaryDefectGatherDao.upsertByGatherInfo(taskId, toolName, gatherInfo);
        }
    }
}
