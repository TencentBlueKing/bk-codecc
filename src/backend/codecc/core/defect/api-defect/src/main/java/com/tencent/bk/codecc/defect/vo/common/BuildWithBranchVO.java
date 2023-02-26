package com.tencent.bk.codecc.defect.vo.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BuildWithBranchVO extends BuildVO {
    private String branch;
}
