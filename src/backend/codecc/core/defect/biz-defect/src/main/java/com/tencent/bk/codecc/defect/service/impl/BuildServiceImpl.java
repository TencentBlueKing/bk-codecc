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

import com.tencent.bk.codecc.defect.dao.mongorepository.BuildRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.BuildDao;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.service.BuildService;
import com.tencent.bk.codecc.defect.vo.common.BuildVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 构建逻辑实现层
 * 
 * @date 2021/11/19
 * @version V1.0
 */
@Service
public class BuildServiceImpl implements BuildService {

    private static final Logger logger = LoggerFactory.getLogger(BuildServiceImpl.class);

    @Autowired
    private BuildDao buildDao;

    @Autowired
    private BuildRepository buildRepository;

    @Override
    public BuildVO upsertAndGetBuildInfo(BuildVO buildVO) {
        logger.info("start to upsert build info, build id: {}", buildVO.getBuildId());
        BuildEntity buildEntity = new BuildEntity();
        BeanUtils.copyProperties(buildVO, buildEntity);
        buildEntity.setBuildNo(buildVO.getBuildNum());
        buildEntity = buildDao.upsertBuildInfo(buildEntity);
        if (null == buildEntity) {
            return null;
        }
        BuildVO resultBuildVO = new BuildVO();
        BeanUtils.copyProperties(buildEntity, resultBuildVO);
        resultBuildVO.setBuildNum(buildEntity.getBuildNo());
        return resultBuildVO;
    }

    @Override
    public BuildVO getBuildVOByBuildId(String buildId) {
        BuildEntity buildEntity = buildRepository.findFirstByBuildId(buildId);
        if (null == buildEntity) {
            return null;
        }
        BuildVO resultBuildVO = new BuildVO();
        BeanUtils.copyProperties(buildEntity, resultBuildVO);
        resultBuildVO.setBuildNum(buildEntity.getBuildNo());
        return resultBuildVO;
    }

    @Override
    public BuildEntity getBuildEntityByBuildId(String buildId) {
        return buildRepository.findFirstByBuildId(buildId);
    }
}
