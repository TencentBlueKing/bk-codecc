package com.tencent.bk.codecc.task.vo.issue;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("任务的提单配置信息")
public class TaskIssueInfoRequest {

    @ApiModelProperty("任务id列表")
    private List<Long> taskIds;
}
