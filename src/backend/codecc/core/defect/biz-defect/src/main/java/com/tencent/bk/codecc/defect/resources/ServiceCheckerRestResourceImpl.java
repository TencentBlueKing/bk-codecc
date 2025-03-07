/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceCheckerRestResource;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.QueryTaskCheckerDimensionRequest;
import com.tencent.devops.common.api.checkerset.CheckerPropVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.service.ToolMetaCacheService;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.web.RestResource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 多工具规则接口实现
 *
 * @version V1.0
 * @date 2019/5/23
 */
@RestResource
public class ServiceCheckerRestResourceImpl implements ServiceCheckerRestResource {

    @Autowired
    private CheckerService checkerService;

    @Autowired
    private ToolMetaCacheService toolMetaCacheService;

    @Override
    public Result<List<String>> queryDimensionByToolChecker(
            QueryTaskCheckerDimensionRequest request
    ) {
        return new Result<>(
                checkerService.queryTaskCheckerDimension(
                        request.getTaskIdList(),
                        request.getProjectId(),
                        request.getToolNameList()
                )
        );
    }

    @Override
    public Result<List<CheckerDetailVO>> queryCheckerDetailByCheckerPropVO(List<CheckerPropVO> propVOS) {
        if (CollectionUtils.isEmpty(propVOS)) {
            return new Result<>(new ArrayList<>());
        }
        List<CheckerDetailEntity> checkerDetailEntityList = checkerService.queryCheckerCategoryByCheckerPropVO(propVOS);
        if (CollectionUtils.isEmpty(checkerDetailEntityList)) {
            return new Result<>(new ArrayList<>());
        }

        List<CheckerDetailVO> checkerDetailVOList = checkerDetailEntityList.stream()
                .map(it -> {
                    CheckerDetailVO checkerDetailVO = new CheckerDetailVO();
                    BeanUtils.copyProperties(it, checkerDetailVO);
                    return checkerDetailVO;
                }).collect(Collectors.toList());

        return new Result<>(checkerDetailVOList);
    }
}
