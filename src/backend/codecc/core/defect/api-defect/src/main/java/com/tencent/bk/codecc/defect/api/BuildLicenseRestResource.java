package com.tencent.bk.codecc.defect.api;


import com.tencent.bk.codecc.defect.vo.coderepository.UploadRepositoriesVO;
import com.tencent.bk.codecc.defect.vo.sca.BatchUploadLicenseVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * 上报证书信息
 *
 * @version V1.0
 * @date 2019/5/2
 */
@Tag(name = "SERVICE_LICENSE", description = "上报证书信息")
@Path("/build/license")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildLicenseRestResource {

    @Operation(summary = "工具侧上报代码仓库信息")
    @Path("/upload")
    @POST
    Result<Boolean> uploadLicense(
            @Parameter(description = "上报证书信息", required = true)
            List<BatchUploadLicenseVO> uploadLicenseVOs);
}
