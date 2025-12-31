package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "RandomTestTaskStarterMQ 的消费者请求 VO")
public class RandomTestTaskStarterMQConsumerVO {
    @Schema(description = "工具名")
    private String toolName;
    @Schema(description = "版本号")
    private String version;
    @Schema(description = "用户ID")
    private String userName;
    @Schema(description = "加入随机测试的代码库数量")
    private Integer need;
    @Schema(description = "测试语言列表")
    private List<String> langList;
    @Schema(description = "规则集ID列表")
    private List<String> checkerSetIdList;
    @Schema(description = "语言列表, digit 版")
    private Long langDigit;
    @Schema(description = "代码库体量上限")
    private Long upperLimit;
    @Schema(description = "代码库体量下限")
    private Long lowerLimit;
    @Schema(description = "灰度项目ID")
    private String projectId;
    @Schema(description = "缓存 t_base_data 的 REPO_SCALE, 以 下限1, 上限1, 下限2, 上限2, ... 的顺序保存")
    private List<Long> sales;
}
