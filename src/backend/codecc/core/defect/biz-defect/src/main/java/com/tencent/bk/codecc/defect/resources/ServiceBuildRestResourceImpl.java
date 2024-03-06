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
 
package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceBuildRestResource;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.service.BuildService;
import com.tencent.bk.codecc.defect.vo.common.BuildVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.web.RestResource;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 构建信息build接口
 * 
 * @date 2021/11/19
 * @version V1.0
 */
@RestResource
public class ServiceBuildRestResourceImpl implements ServiceBuildRestResource {
    @Autowired
    private BuildService buildService;

    @Override
    public Result<Boolean> updateBuildInfo(BuildVO buildVO) {
        BuildVO resultBuildVO = buildService.upsertAndGetBuildInfo(buildVO);
        return new Result<>(null != resultBuildVO);
    }

    @Override
    public Result<Integer> getBuildNum(String buildId) {
        BuildEntity buildEntity = buildService.getBuildEntityByBuildId(buildId);
        Integer buildNo = buildEntity != null && StringUtils.isNotBlank(buildEntity.getBuildNo())
                && StringUtils.isNumeric(buildEntity.getBuildNo()) ? Integer.valueOf(buildEntity.getBuildNo()) : null;
        return new Result<>(buildNo);
    }

    @Override
    public Result<BuildVO> getBuildInfo(String buildId) {
        BuildEntity buildEntity = buildService.getBuildEntityByBuildId(buildId);
        if (buildEntity == null) {
            return new Result<>(null);
        }
        BuildVO vo = new BuildVO();
        BeanUtils.copyProperties(buildEntity, vo);
        return new Result<>(vo);
    }
}
