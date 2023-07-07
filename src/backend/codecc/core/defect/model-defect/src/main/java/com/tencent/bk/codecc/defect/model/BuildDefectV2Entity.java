package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Sharded;

/**
 * 告警快照表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_build_defect_v2")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_build_id_1_tool_name_1_defect_id_1",
                def = "{'task_id': 1, 'build_id': 1, 'tool_name': 1, defect_id: 1}", background = true),
        @CompoundIndex(name = "task_id_1_build_id_1_defect_id_1",
                def = "{'task_id': 1, 'build_id': 1, 'defect_id': 1}", background = true)
})
@Sharded(shardKey = "task_id")
public class BuildDefectV2Entity extends CommonEntity {

    @Field("task_id")
    private Long taskId;

    @Field("tool_name")
    private String toolName;

    @Field("build_id")
    private String buildId;

    @Field("build_num")
    private String buildNum;

    @Field("defect_id")
    private String defectId;

    @Field("revision")
    private String revision;

    @Field("branch")
    private String branch;

    @Field("subModule")
    private String subModule;

    @Field("line_num")
    private Integer lineNum;

    /**
     * CCN特有
     */
    @Field("start_lines")
    private Integer startLines;

    /**
     * CCN特有
     */
    @Field("end_lines")
    private Integer endLines;
}
