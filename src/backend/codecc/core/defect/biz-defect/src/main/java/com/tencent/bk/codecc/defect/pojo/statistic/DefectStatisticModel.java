package com.tencent.bk.codecc.defect.pojo.statistic;

import com.tencent.bk.codecc.defect.model.DUPCDefectJsonFileEntity;
import com.tencent.bk.codecc.defect.model.defect.DUPCDefectEntity;
import com.tencent.bk.codecc.defect.model.defect.DefectEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity;
import com.tencent.bk.codecc.defect.model.sca.SCASbomAggregateModel;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 告警统计逻辑入参
 *
 * @author warmli
 */
@Data
@AllArgsConstructor
public class DefectStatisticModel<T extends DefectEntity> {

    /**
     * 向下兼容构造器
     * @param taskDetailVO
     * @param toolName
     * @param averageCcn
     * @param buildId
     * @param toolBuildStackEntity
     * @param defectList
     * @param riskConfigMap
     * @param defectJsonFileEntity
     * @param fastIncrementFlag
     */
    public DefectStatisticModel(
            TaskDetailVO taskDetailVO,
            String toolName,
            float averageCcn,
            String buildId,
            ToolBuildStackEntity toolBuildStackEntity,
            List<T> defectList,
            Map<String, String> riskConfigMap,
            DUPCDefectJsonFileEntity<DUPCDefectEntity> defectJsonFileEntity,
            Boolean fastIncrementFlag
    ) {
        this(taskDetailVO, toolName, averageCcn, buildId, toolBuildStackEntity, defectList, riskConfigMap,
                defectJsonFileEntity, null, fastIncrementFlag, null);
    }

    /**
     * 向下兼容构造器
     * @param taskDetailVO
     * @param toolName
     * @param averageCcn
     * @param buildId
     * @param toolBuildStackEntity
     * @param defectList
     * @param riskConfigMap
     * @param defectJsonFileEntity
     * @param newCountCheckers
     * @param fastIncrementFlag
     */
    public DefectStatisticModel(
            TaskDetailVO taskDetailVO,
            String toolName,
            float averageCcn,
            String buildId,
            ToolBuildStackEntity toolBuildStackEntity,
            List<T> defectList,
            Map<String, String> riskConfigMap,
            DUPCDefectJsonFileEntity<DUPCDefectEntity> defectJsonFileEntity,
            List<String> newCountCheckers,
            Boolean fastIncrementFlag
    ) {
        this(taskDetailVO, toolName, averageCcn, buildId, toolBuildStackEntity, defectList, riskConfigMap,
                defectJsonFileEntity, newCountCheckers, fastIncrementFlag, null);
    }

    /**
     * 向下兼容构造器
     * @param taskDetailVO
     * @param toolName
     * @param buildId
     * @param toolBuildStackEntity
     * @param defectList
     * @param fastIncrementFlag
     * @param sbomAggregateModel
     */
    public DefectStatisticModel(
            TaskDetailVO taskDetailVO,
            String toolName,
            String buildId,
            ToolBuildStackEntity toolBuildStackEntity,
            List<T> defectList,
            Boolean fastIncrementFlag,
            SCASbomAggregateModel sbomAggregateModel

    ) {
        this(taskDetailVO, toolName, 0f, buildId, toolBuildStackEntity, defectList, null,
                null, null, fastIncrementFlag, sbomAggregateModel);
    }

    private TaskDetailVO taskDetailVO;

    private String toolName;

    /**
     * 仅限于CCN工具，其他工具传0
     */
    private float averageCcn;

    private String buildId;

    private ToolBuildStackEntity toolBuildStackEntity;

    private List<T> defectList;

    /**
     * 仅限于DUPC工具，其他工具传 null
     */
    private Map<String, String> riskConfigMap;

    /**
     * 仅限于DUPC工具，其他工具传 null
     */
    private DUPCDefectJsonFileEntity<DUPCDefectEntity> defectJsonFileEntity;

    /**
     * 真实新增告警对应的规则列表
     * common or lint, 其他传null
     */
    private List<String> newCountCheckers;

    /**
     * 超快增量标识
     */
    private Boolean fastIncrementFlag;

    /**
     * 仅限于SCA工具，其他工具传 null
     */
    private SCASbomAggregateModel sbomAggregateModel;
}
