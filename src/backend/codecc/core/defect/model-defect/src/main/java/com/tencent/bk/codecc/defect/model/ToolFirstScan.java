package com.tencent.bk.codecc.defect.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 工具第一次扫描该代码库时间
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToolFirstScan {

    @Field("tool_name")
    private String toolName;

    @Field("first_scan_time")
    private Long firstScanTime;
}
