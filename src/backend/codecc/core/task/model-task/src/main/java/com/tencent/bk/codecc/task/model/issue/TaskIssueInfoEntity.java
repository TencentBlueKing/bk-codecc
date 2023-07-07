package com.tencent.bk.codecc.task.model.issue;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_task_issue_info")
public class TaskIssueInfoEntity extends CommonEntity {

    @Field("task_id")
    @Indexed(background = true)
    private Long taskId;

    @Field("project_id")
    private String projectId;

    @Field("pipeline_id")
    @Indexed(background = true)
    private String pipelineId;

    @Field("channel")
    private String channel;

    @Field("system")
    private String system;

    @Field("sub_system")
    private String subSystem;

    @Field("sub_system_id")
    private String subSystemId;

    @Field("sub_system_cn")
    private String subSystemCn;

    @Field("resolvers")
    private Set<String> resolvers;

    @Field("creators")
    private Set<String> creators;

    @Field("receivers")
    private Set<String> receivers;

    @Field("find_by_version")
    private String findByVersion;

    @Field("max_issue")
    private Integer maxIssue;

    @Field("auto_commit")
    private Boolean autoCommit;

    @Field("issue_count")
    private Integer issueCount;

    @Field("tools")
    private Set<String> tools;

    @Field("severities")
    private Set<Integer> severities;
}
