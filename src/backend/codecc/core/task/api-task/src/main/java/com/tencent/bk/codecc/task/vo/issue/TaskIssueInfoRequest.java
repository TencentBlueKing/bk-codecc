package com.tencent.bk.codecc.task.vo.issue;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "任务的提单配置信息")
public class TaskIssueInfoRequest {

    @Schema(description = "任务id列表")
    private List<Long> taskIds;
}
