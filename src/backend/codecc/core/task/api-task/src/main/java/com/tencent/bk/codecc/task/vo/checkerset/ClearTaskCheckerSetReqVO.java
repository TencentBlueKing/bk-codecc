package com.tencent.bk.codecc.task.vo.checkerset;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 清除任务工具对应的规则集请求体视图
 *
 * @version V4.0
 * @date 2019/11/1
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "清除任务工具对应的规则集请求体视图")
public class ClearTaskCheckerSetReqVO extends CommonVO
{
    @Schema(description = "工具名称列表", required = true)
    private List<String> toolNames;

    @Schema(description = "是否需要同步到流水线", required = true)
    private Boolean needUpdatePipeline;
}
