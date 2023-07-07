package com.tencent.bk.codecc.defect.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;

/**
 * 规则集Id与最大版本映射
 */
@Data
public class CheckerMaxVersionAggModel {

    @Id
    private String checkerSetId;
    private Integer maxVersion;
}
