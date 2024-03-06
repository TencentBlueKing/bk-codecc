package com.tencent.bk.codecc.task.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GetLatestBuildIdMapRequest {

    private List<Long> taskIdList;
}
