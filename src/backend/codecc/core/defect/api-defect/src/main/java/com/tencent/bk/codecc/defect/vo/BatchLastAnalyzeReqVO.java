package com.tencent.bk.codecc.defect.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "批量获取任务的工具的最后一次分析结果的请求VO")
public class BatchLastAnalyzeReqVO {
    private Map<Long, String> taskIdAndBuildIdMap;
    private String toolName;
}
