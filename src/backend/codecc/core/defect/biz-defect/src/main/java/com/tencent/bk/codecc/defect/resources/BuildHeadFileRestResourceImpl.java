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

import com.tencent.bk.codecc.defect.api.BuildHeadFileResource;
import com.tencent.bk.codecc.defect.service.IHeadFileService;
import com.tencent.bk.codecc.defect.vo.HeadFileVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 头文件信息接口实现
 * 
 * @date 2021/12/31
 * @version V1.0
 */
@RestResource
public class BuildHeadFileRestResourceImpl implements BuildHeadFileResource {
    @Autowired
    private IHeadFileService headFileService;

    @Override
    public Result<HeadFileVO> findHeadFileInfo(Long taskId) {
        return new Result<>(headFileService.findHeadFileInfo(taskId));
    }
}
