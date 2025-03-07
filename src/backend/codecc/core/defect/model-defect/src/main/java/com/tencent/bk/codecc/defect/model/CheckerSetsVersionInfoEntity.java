package com.tencent.bk.codecc.defect.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
public class CheckerSetsVersionInfoEntity {
    @Field("checker_set_id")
    private String checkerSetId;

    @Field("version")
    private Integer version;
}
