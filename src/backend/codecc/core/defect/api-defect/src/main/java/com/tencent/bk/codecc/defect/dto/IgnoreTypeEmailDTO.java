package com.tencent.bk.codecc.defect.dto;

import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeReportDetailVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 忽略类型的邮件数据传输对象
 *
 * @date 2022/7/27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("忽略类型的邮件数据传输对象")
public class IgnoreTypeEmailDTO {

    @ApiModelProperty("邮件标题")
    private String title;

    @ApiModelProperty("接收人")
    private Set<String> receiverSet;

    @ApiModelProperty("邮件内容所需的数据")
    private IgnoreTypeReportDetailVO ignoreTypeReportDetailVO;
}
