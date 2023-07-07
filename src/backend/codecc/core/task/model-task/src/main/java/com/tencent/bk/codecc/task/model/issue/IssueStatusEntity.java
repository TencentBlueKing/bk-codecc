package com.tencent.bk.codecc.task.model.issue;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
public class IssueStatusEntity {
    @Field("close")
    private String close;
    @Field("open")
    private String open;
    @Field("reject")
    private String reject;
    @Field("resolve")
    private String resolve;
}
