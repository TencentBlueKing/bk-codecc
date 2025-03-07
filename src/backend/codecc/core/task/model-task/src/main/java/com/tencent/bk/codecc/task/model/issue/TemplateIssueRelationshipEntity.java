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

/**
 * 存储模板Id的所有授权信息（含历史授权信息）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_template_issue_relationship")
@CompoundIndexes({
        @CompoundIndex(
                name = "template_id_1_auth_timestamp_1_sub_system_id_1",
                def = "{'template_id': 1, 'auth_timestamp': 1, 'sub_system_id': 1}",
                background = true
        )
})
public class TemplateIssueRelationshipEntity extends CommonEntity {

    @Field("template_id")
    @Indexed(background = true)
    private String templateId;

    @Field("project_id")
    private String projectId;

    @Field("channel")
    private String channel;

    @Field("system")
    private String system;

    @Field("sub_system")
    private String subSystem;

    @Field("sub_system_id")
    @Indexed(background = true)
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

    /**
     * 授权人，新增字段，非所有数据拥有
     */
    @Field("auth_by")
    private String authBy;

    /**
     * 授权时间戳，新增字段，非所有数据有；用于区分单条流水线的多个codecc插件中，哪个插件最后一次授权
     */
    @Field("auth_timestamp")
    private Long authTimestamp;
}
