package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

/**
 * RandomTestTaskStarterMQ 的消费者请求 VO
 *
 * @date 2024/05/27
 */
@Data
@ApiModel("RandomTestTaskStarterMQ 的消费者请求 VO")
public class RandomTestTaskStarterMQConsumerVO {
    @ApiModelProperty("工具名")
    private String toolName;
    @ApiModelProperty("版本号")
    private String version;
    @ApiModelProperty("用户ID")
    private String userName;
    @ApiModelProperty("加入随机测试的代码库数量")
    private Integer need;
    @ApiModelProperty("测试语言列表")
    private List<String> langList;
    @ApiModelProperty("规则集ID列表")
    private List<String> checkerSetIdList;
    @ApiModelProperty("语言列表, digit 版")
    private Long langDigit;
    @ApiModelProperty("代码库体量上限")
    private Long upperLimit;
    @ApiModelProperty("代码库体量下限")
    private Long lowerLimit;
    @ApiModelProperty("灰度项目ID")
    private String projectId;
    @ApiModelProperty("缓存 t_base_data 的 REPO_SCALE, 以 下限1, 上限1, 下限2, 上限2, ... 的顺序保存")
    private List<Long> sales;
}
