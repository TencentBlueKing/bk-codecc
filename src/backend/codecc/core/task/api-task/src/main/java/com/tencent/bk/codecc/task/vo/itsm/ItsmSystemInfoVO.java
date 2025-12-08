package com.tencent.bk.codecc.task.vo.itsm;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ItsmSystemInfoVO extends CommonVO {
    @Schema(description = "系统标识")
    private String system;

    @Schema(description = "系统名称")
    private String nameCn;

    @Schema(description = "创建单据URL")
    private String createTicketUrl;

    @Schema(description = "创建单据Header")
    private String createTicketHeader;

    @Schema(description = "创建单据Body")
    private String createTicketBody;

    @Schema(description = "获取单据状态URL")
    private String getTicketStatusUrl;

    @Schema(description = "获取单独状态Header")
    private String getTicketStatusHeader;

    @Schema(description = "获取单独状态Body")
    private String getTicketStatusBody;

    @Schema(description = "操作单据URL")
    private String operateTicketUrl;

    @Schema(description = "操作单据Header")
    private String operateTicketHeader;

    @Schema(description = "操作单据Body")
    private String operateTicketBody;

}
