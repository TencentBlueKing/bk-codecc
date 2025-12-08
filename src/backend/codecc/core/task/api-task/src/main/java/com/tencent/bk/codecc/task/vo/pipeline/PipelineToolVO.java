package com.tencent.bk.codecc.task.vo.pipeline;

import com.tencent.devops.common.api.CommonVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.checkerset.DividedCheckerSetsVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 流水线工具视图
 *
 * @version V4.0
 * @date 2019/11/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "流水线工具视图")
public class PipelineToolVO extends CommonVO
{
    private String toolName;

    private DividedCheckerSetsVO toolCheckerSets;

    private CheckerSetVO checkerSetInUse;

    private List<PipelineToolParamVO> params;
}
