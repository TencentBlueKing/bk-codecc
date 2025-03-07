package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 指定测试开始接口的请求视图
 *
 * @date 2024/03/22
 */
@Data
@ApiModel("指定测试开始接口的请求视图")
public class StartTestReqVO {

    private String version;

    private String projectId;

    private String projectName;

    @Deprecated
    @ApiModelProperty("任务 id 列表")
    private List<Long> taskIdList;

}
