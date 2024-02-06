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

import com.tencent.bk.codecc.defect.dao.defect.mongorepository.HeadFileRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.HeadFileDao;
import com.tencent.bk.codecc.defect.model.HeadFileEntity;
import com.tencent.bk.codecc.defect.service.IHeadFileService;
import com.tencent.bk.codecc.defect.vo.HeadFileVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 头文件识别服务逻辑
 * 
 * @date 2021/12/31
 * @version V1.0
 */
@Slf4j
@Service
public class HeadFileServiceImpl implements IHeadFileService {
    @Autowired
    private HeadFileRepository headFileRepository;

    @Autowired
    private HeadFileDao headFileDao;

    @Override
    public HeadFileVO findHeadFileInfo(Long taskId) {
        HeadFileEntity headFileEntity = headFileRepository.findFirstByTaskId(taskId);
        if (null == headFileEntity) {
            return null;
        }
        HeadFileVO headFileVO = new HeadFileVO();
        headFileVO.setTaskId(taskId);
        headFileVO.setHeadFileSet(headFileEntity.getHeadFileSet());
        return headFileVO;
    }

    @Override
    public Boolean deleteHeadFileInfo(Long taskId, Set<String> headFileSet) {
        log.info("delete head file info, task id: {}, head file set size: {}", taskId,
                null == headFileSet ? 0 : headFileSet.size());
        if (CollectionUtils.isNotEmpty(headFileSet)) {
            HeadFileEntity headFileEntity = new HeadFileEntity();
            headFileEntity.setTaskId(taskId);
            headFileEntity.setHeadFileSet(headFileSet);
            headFileDao.deleteHeadFileInfo(headFileEntity);
        } else {
            headFileDao.deleteHeadFileInfo(taskId);
        }
        return true;
    }

    @Override
    public Boolean upsertHeadFileInfo(HeadFileEntity headFileEntity) {
        log.info("upsert head file info, task id: {}, head file set size: {}", headFileEntity.getTaskId(),
                null == headFileEntity.getHeadFileSet() ? 0 : headFileEntity.getHeadFileSet().size());
        headFileDao.addHeadFileInfo(headFileEntity);
        return true;
    }
}
