package com.tencent.devops.common.api.checkerset;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规则集语言视图
 *
 * @version V1.0
 * @date 2020/1/5
 */
@Data
@ApiModel("规则集语言视图")
@AllArgsConstructor
@NoArgsConstructor
public class CheckerSetCodeLangVO {
    /**
     * 代码语言
     */
    private Integer codeLang;

    /**
     * 展示名称
     */
    private String displayName;
}
