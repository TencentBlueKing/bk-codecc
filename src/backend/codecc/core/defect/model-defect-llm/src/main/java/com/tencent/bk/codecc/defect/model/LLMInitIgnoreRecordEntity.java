package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * LLM 误报过滤模块:
 * 记录 [toolName].[checkerName] 的用户误报忽略数据正在 [taskId] 的 [buildId] 中被初始化.
 * buildId = 0, 代表曾在用户忽略中被初始化.
 *
 * @date 2025/06/26
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_llm_init_ignore_record")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_tool_name_1", def = "{'task_id': 1, 'tool_name': 1}", background = true)
})
public class LLMInitIgnoreRecordEntity extends CommonEntity {
    @Field("task_id")
    private Long taskId;
    @Field("tool_name")
    private String toolName;
    @Field("checker_name")
    private String checkerName;
    @Field("build_id")
    private String buildId;
}
