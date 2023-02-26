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
 
package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.HeadFileVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * oc头文件识别接口
 * 
 * @date 2021/12/31
 * @version V1.0
 */
@Api(tags = {"BUILD_HEADFILE"}, description = "oc头文件识别接口")
@Path("/build/headFile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildHeadFileResource {
    @ApiOperation("查询头文件路径信息")
    @Path("/taskId/{taskId}")
    @GET
    Result<HeadFileVO> findHeadFileInfo(
            @ApiParam(value = "任务id", required = true)
            @PathParam("taskId")
                    Long taskId);
}
