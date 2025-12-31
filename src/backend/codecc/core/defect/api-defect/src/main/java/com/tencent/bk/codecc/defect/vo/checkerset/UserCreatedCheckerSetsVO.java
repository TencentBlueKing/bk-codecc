package com.tencent.bk.codecc.defect.vo.checkerset;

import com.tencent.devops.common.api.CommonVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 用户创建的规则集视图
 *
 * @version V4.0
 * @date 2019/11/2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户创建的规则集视图")
public class UserCreatedCheckerSetsVO extends CommonVO
{
    /**
     * 用户创建的规则集列表
     */
    @Schema(description = "规则集ID", required = true)
    private List<CheckerSetVO> userCreatedCheckerSets;
}
