package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 指定测试开始接口的请求视图
 *
 * @date 2024/03/22
 */
@Data
@Schema(description = "指定测试开始接口的请求视图")
public class StartTestReqVO {

    private String version;

    private String projectId;

    private String projectName;

    @Deprecated
    @Schema(description = "任务 id 列表")
    private List<Long> taskIdList;

}
