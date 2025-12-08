package com.tencent.devops.common.api.checkerset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 规则集中规则与参数视图
 *
 * @version V1.0
 * @date 2020/1/2
 */
@Data
@Schema(description = "规则集中规则与参数视图")
public class CheckerPropVO {
    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 适用语言
     */
    private Long lang;

    /**
     * 规则
     */
    private String checkerKey;

    /**
     * 规则名称
     */
    private String checkerName;

    /**
     * 规则参数
     */
    private String props;
}
