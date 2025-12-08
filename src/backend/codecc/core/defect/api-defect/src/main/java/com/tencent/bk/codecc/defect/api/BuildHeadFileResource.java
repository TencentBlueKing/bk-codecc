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
 
package com.tencent.bk.codecc.defect.api;

import com.tencent.bk.codecc.defect.vo.HeadFileVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * oc头文件识别接口
 * 
 * @date 2021/12/31
 * @version V1.0
 */
@Tag(name = "BUILD_HEADFILE", description = "oc头文件识别接口")
@Path("/build/headFile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildHeadFileResource {
    @Operation(summary = "查询头文件路径信息")
    @Path("/taskId/{taskId}")
    @GET
    Result<HeadFileVO> findHeadFileInfo(
            @Parameter(description = "任务id", required = true)
            @PathParam("taskId")
                    Long taskId);
}
