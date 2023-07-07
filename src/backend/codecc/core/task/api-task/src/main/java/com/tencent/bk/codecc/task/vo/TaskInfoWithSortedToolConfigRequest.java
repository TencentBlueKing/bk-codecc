package com.tencent.bk.codecc.task.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskInfoWithSortedToolConfigRequest {

    private List<Long> taskIdList;

    private Boolean needSorted;
}
