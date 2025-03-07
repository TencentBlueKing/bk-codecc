package com.tencent.bk.codecc.defect.api;


import com.tencent.bk.codecc.defect.vo.coderepository.UploadRepositoriesVO;
import com.tencent.bk.codecc.defect.vo.sca.BatchUploadLicenseVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * 上报证书信息
 *
 * @version V1.0
 * @date 2019/5/2
 */
@Api(tags = {"SERVICE_LICENSE"}, description = "上报证书信息")
@Path("/build/license")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BuildLicenseRestResource {

    @ApiOperation("工具侧上报代码仓库信息")
    @Path("/upload")
    @POST
    Result<Boolean> uploadLicense(
            @ApiParam(value = "上报证书信息", required = true)
            List<BatchUploadLicenseVO> uploadLicenseVOs);
}
