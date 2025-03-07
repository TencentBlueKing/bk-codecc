package com.tencent.bk.codecc.task.model.issue;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_issue_system_info")
public class IssueSystemInfoEntity extends CommonEntity {
    @Field("system")
    @Indexed(background = true)
    private String system;

    @Field("name_cn")
    private String nameCn;

    @Field("client_id")
    private String clientId;

    @Field("client_secret")
    private String clientSecret;

    @Field("home_url")
    private String homeUrl;

    @Field("oauth_url")
    private String oauthUrl;

    @Field("oauth_redirect_url")
    private String oauthRedirectUrl;

    @Field("oauth_token_url")
    private String oauthTokenUrl;

    @Field("oauth_token_refresh_url")
    private String oauthTokenRefreshUrl;

    @Field("create_submit_url")
    private String createSubmitUrl;

    @Field("create_header")
    private String createHeader;

    @Field("create_body")
    private String createBody;

    @Field("update_submit_url")
    private String updateSubmitUrl;

    @Field("update_header")
    private String updateHeader;

    @Field("update_body")
    private String updateBody;

    @Field("sub_detail_url")
    private String subDetailUrl;

    @Field("issue_detail_url")
    private String issueDetailUrl;

    @Field("issue_status")
    private IssueStatusEntity issueStatus;

    @Field("issue_severity")
    private IssueSeverityEntity issueSeverity;

    @Field("issue_priority")
    private IssuePriorityEntity issuePriority;

    @Field("retry_regex")
    private String retryRegex;

    @Field("success_regex")
    private String successRegex;

    @Field("issue_id_regex")
    private String issueIdRegex;

    @Field("detail")
    private String detail;
}
