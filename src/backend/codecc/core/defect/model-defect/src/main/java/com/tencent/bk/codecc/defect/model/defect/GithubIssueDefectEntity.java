package com.tencent.bk.codecc.defect.model.defect;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * Github 统计工具单独
 * task_id url repository_url title  id type state created_at closed_at updated_at  issue创建人 issue解决人
 */
@Data
@Document(value = "t_github_issue_defect")
@CompoundIndexes(
        @CompoundIndex(name = "task_id_1_created_timestamp_1", def = "{'task_id': 1, 'created_timestamp': 1}",
                background = true)
)
public class GithubIssueDefectEntity {

    @Field("task_id")
    private Long taskId;

    @JsonProperty("url")
    private String url;

    @Field("repository_url")
    @JsonProperty("repository_url")
    private String repositoryUrl;

    @JsonProperty("title")
    private String title;

    @Field("id")
    private String id;

    @JsonProperty("state")
    private String state;

    @Field("created_at")
    @JsonProperty("created_at")
    private String createdAt;

    @Field("created_timestamp")
    private Long createdTimestamp;

    @Field("closed_at")
    @JsonProperty("closed_at")
    private String closedAt;

    @Field("closed_timestamp")
    private Long closedTimestamp;

    @Field("updated_at")
    @JsonProperty("updated_at")
    private String updatedAt;

    @Field("updated_timestamp")
    private Long updatedTimestamp;

    @JsonProperty("user")
    private User creator;

    @Field("closed_by")
    @JsonProperty("closed_by")
    private User closedBy;

    @JsonProperty("assignees")
    private List<User> assignees;

    @Data
    public static class User {
        @JsonProperty("login")
        private String login;

        @JsonProperty("type")
        private String type;
    }
}
