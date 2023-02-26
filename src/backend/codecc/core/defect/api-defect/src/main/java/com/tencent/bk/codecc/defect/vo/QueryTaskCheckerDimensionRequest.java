package com.tencent.bk.codecc.defect.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryTaskCheckerDimensionRequest {

    private List<Long> taskIdList;
    private List<String> toolNameList;
    private String projectId;
}
