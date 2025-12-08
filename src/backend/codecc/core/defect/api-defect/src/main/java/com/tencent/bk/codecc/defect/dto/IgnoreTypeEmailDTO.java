package com.tencent.bk.codecc.defect.dto;

import com.tencent.bk.codecc.defect.vo.ignore.IgnoreTypeReportDetailVO;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "忽略类型的邮件数据传输对象")
public class IgnoreTypeEmailDTO {

    @Schema(description = "邮件标题")
    private String title;

    @Schema(description = "接收人")
    private Set<String> receiverSet;

    @Schema(description = "邮件内容所需的数据")
    private IgnoreTypeReportDetailVO ignoreTypeReportDetailVO;
}
