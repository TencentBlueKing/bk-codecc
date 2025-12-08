package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * LLMNDF, 全名 LLM-Negative-Defect-Filter, 基于大模型的误报过滤
 * 这张表保存 LLMNDF 的工具侧控制信息, 即记录工具的哪些规则开启了这个误报过滤功能
 *
 * @date 2025/03/25
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "t_llmndf_tool_access_control")
public class LLMNDFToolAccessControlEntity extends CommonEntity {
    @Field("tool_name")
    private String toolName;

    @Field("open_checkers")
    private List<String> openCheckers;
}
