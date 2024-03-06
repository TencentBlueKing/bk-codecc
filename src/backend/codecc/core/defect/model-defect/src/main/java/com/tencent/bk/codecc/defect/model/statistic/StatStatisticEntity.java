package com.tencent.bk.codecc.defect.model.statistic;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "t_stat_statistic")
@NoArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_build_id_1_tool_name_1",
                def = "{'task_id': 1, 'build_id': 1, 'tool_name': 1}")
})
public class StatStatisticEntity {

    @Id
    private String entityId;
    /**
     * 任务ID
     */
    @Field("task_id")
    private Long taskId;
    /**
     * 工具名称
     */
    @Field("tool_name")
    private String toolName;
    /**
     * 构建ID
     */
    @Field("build_id")
    private String buildId;
    /**
     * 统计的时间
     */
    @Field("time")
    private long time;
}
