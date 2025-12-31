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

import static com.tencent.devops.common.api.auth.HeaderKt.AUTH_HEADER_DEVOPS_TASK_ID;

import com.tencent.bk.codecc.defect.vo.CodeRepoVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * 代码库信息接口
 *
 * @version V1.0
 * @date 2019/12/3
 */
@Tag(name = "SERVICE_CODEREPO", description = "告警相关接口")
@Path("/service/repo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ServiceRepoResource {

    @Operation(summary = "获取指定任务的代码库清单")
    @Path("/list")
    @GET
    Result<Set<CodeRepoVO>> getCodeRepoByTaskIdAndBuildId(
            @Parameter(description = "任务ID", required = true)
            @HeaderParam(AUTH_HEADER_DEVOPS_TASK_ID)
                    Long taskId,
            @Parameter(description = "构建id", required = true)
            @QueryParam(value = "buildId")
                    String buildId);

    @Operation(summary = "获取指定任务的代码库清单")
    @Path("/lists")
    @POST
    Result<Map<Long, Set<CodeRepoVO>>> getCodeRepoByTaskIds(
            @Parameter(description = "任务ID", required = true)
                    Collection<Long> taskIds);
}
