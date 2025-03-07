package com.tencent.bk.codecc.task.vo.itsm;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ItsmSystemInfoVO extends CommonVO {
    @ApiModelProperty("系统标识")
    private String system;

    @ApiModelProperty("系统名称")
    private String nameCn;

    @ApiModelProperty("创建单据URL")
    private String createTicketUrl;

    @ApiModelProperty("创建单据Header")
    private String createTicketHeader;

    @ApiModelProperty("创建单据Body")
    private String createTicketBody;

    @ApiModelProperty("获取单据状态URL")
    private String getTicketStatusUrl;

    @ApiModelProperty("获取单独状态Header")
    private String getTicketStatusHeader;

    @ApiModelProperty("获取单独状态Body")
    private String getTicketStatusBody;

    @ApiModelProperty("操作单据URL")
    private String operateTicketUrl;

    @ApiModelProperty("操作单据Header")
    private String operateTicketHeader;

    @ApiModelProperty("操作单据Body")
    private String operateTicketBody;

}
