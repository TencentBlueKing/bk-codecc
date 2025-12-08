package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 代码库规模信息
 *
 * @date 2024/04/29
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RepoScaleVO extends CommonVO {

    @Schema(description = "代码库规模上限")
    private Long upperLimit;
    @Schema(description = "代码库规模下限")
    private Long lowerLimit;
    @Schema(description = "名字")
    private String name;

}
