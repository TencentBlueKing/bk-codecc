package com.tencent.bk.codecc.defect.vo.redline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 编译类工具红线告警
 *
 * @version V1.0
 * @date 2019/12/6
 */
@Data
@Schema(description = "编译类工具红线告警")
public class RLCompileDefectVO
{
    @Schema(description = "遗留严重告警数")
    private long remainSerious;

    @Schema(description = "遗留一般告警数")
    private long remainNormal;

    @Schema(description = "遗留提示告警数")
    private long remainPrompt;

    @Schema(description = "新严重告警数")
    private long newSerious;

    @Schema(description = "新一般告警数")
    private long newNormal;

    @Schema(description = "新提示告警数")
    private long newPrompt;

    @Schema(description = "历史严重告警数（遗留 - 新）")
    private long historySerious;

    @Schema(description = "历史一般告警数（遗留 - 新）")
    private long historyNormal;

    @Schema(description = "历史提示告警数（遗留 - 新）")
    private long historyPrompt;

    @Schema(description = "规则包告警数")
    private Map<String, Long> checkerPkgCounts;
}
