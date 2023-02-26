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

import com.tencent.bk.codecc.defect.api.BuildBuildRestResource;
import com.tencent.bk.codecc.defect.service.BuildService;
import com.tencent.bk.codecc.defect.service.SnapShotService;
import com.tencent.bk.codecc.defect.vo.common.BuildVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * 构建信息build接口
 * 
 * @date 2021/11/19
 * @version V1.0
 */
@RestResource
@Slf4j
public class BuildBuildRestResourceImpl implements BuildBuildRestResource {
    @Autowired
    private BuildService buildService;

    @Autowired
    private SnapShotService snapShotService;

    @Override
    public Result<Boolean> updateBuildInfo(BuildVO buildVO) {
        BuildVO resultBuildVO = buildService.upsertAndGetBuildInfo(buildVO);

        // 兼容旧插件，新插件会上传项目相关信息
        if (!StringUtils.isEmpty(buildVO.getProjectId())
                && !StringUtils.isEmpty(buildVO.getBuildId())) {
            // buildTime在一次完整构建上下文中(atomContext)是固定不变的，可作重试边界标识
            snapShotService.allocateSnapshotOnBuildStart(
                    buildVO.getProjectId(),
                    buildVO.getPipelineId(),
                    buildVO.getTaskId(),
                    buildVO.getBuildId(),
                    buildVO.getBuildTime()
            );
        } else {
            log.info("codecc plugin may be old, build id: {}", buildVO.getBuildId());
        }

        return new Result<>(null != resultBuildVO);
    }
}
