package com.tencent.bk.codecc.defect.model.issue;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Set;

@Data
@Document(collection = "t_defect_issue_info")
@CompoundIndexes({
    @CompoundIndex(
        name = "task_id_1_tool_name_1_status_1",
        def = "{'task_id': 1, 'tool_name': 1, 'status': 1}",
        background = true),
    @CompoundIndex(
        name = "task_id_1_defect_entity_1",
        def = "{'task_id': 1, 'defect_entity_id': 1}",
        background = true,
        unique = true)
})
public class DefectIssueInfoEntity {
    @Id
    private String entityId;

    @Field("task_id")
    private Long taskId;

    @Field("tool_name")
    private String toolName;

    @Field("defect_entity_id")
    private String defectEntityId;

    @Field("status")
    private Integer status;

    @Field("severity")
    private Integer severity;

    @Field("issue_system")
    private String issueSystem;

    @Field("issue_sub_system")
    private String issueSubSystem;

    @Field("issue_sub_system_id")
    private String issueSubSystemId;

    @Field("issue_id")
    private String issueId;

    @Field("submit_status")
    private Integer submitStatus;

    @Field("submit_err_msg")
    private String submitErrMsg;

    @Field("submit_stack_trace")
    private String submitStacktrace;

    @Field("resolvers")
    private Set<String> resolvers;

    @Field("creators")
    private Set<String> creators;

    @Field("receivers")
    private Set<String> receivers;

    @Field("find_by_version")
    private String findByVersion;
}
