package com.tencent.bk.codecc.defect.pojo.statistic;

import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 告警统计抽象model类
 *
 * @author warmli
 */
@Data
public abstract class AbstractDefectStatisticModel {

    private Long taskId;

    private String toolName;

    private String createFrom;

    private String buildId;

    private String baseBuildId;

    /**
     * 规则 to 维度标签
     */
    private Map<String, String> checkerKeyToCategoryMap;

    /**
     * 数据是否迁移
     */
    private boolean migrationSuccessful;

    private DimensionStatisticModel dimensionStatisticModel;

    /**
     * 真实新增告警对应的规则列表
     */
    public List<String> newCountCheckerList;
}
