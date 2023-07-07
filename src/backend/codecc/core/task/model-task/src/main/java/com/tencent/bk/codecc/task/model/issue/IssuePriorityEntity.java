package com.tencent.bk.codecc.task.model.issue;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
public class IssuePriorityEntity {
    @Field("serious")
    private String serious;
    @Field("normal")
    private String normal;
    @Field("prompt")
    private String prompt;
}
