package com.tencent.bk.codecc.defect.model.checkerset;

import lombok.Data;

/**
 * 规则集版本实体
 *
 * @version V4.0
 * @date 2019/11/1
 */
@Data
public class CheckerSetTaskCountEntity {
    /**
     * 规则集ID
     */
    private String checkerSetId;

    /**
     * 使用该规则集的任务数
     */
    private Long taskInUseCount;
}
