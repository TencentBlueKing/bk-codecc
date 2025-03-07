package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.CommonVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 更新 BatchTestStatusEntity 的入参
 *
 * @date 2024/03/27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class BatchTestStatusUpdateVO extends CommonVO {
    private Long taskId;
    private String toolName;
    private String version;
    private Integer stage;

    private Integer status;
    private Boolean isSuccess;
    private Long costTime;
    private Integer defectCount;
    private Long codeCount;
    private Long totalLineCount;
    private Integer failCount;
    private Long failTaskId;
    private Set<Long> taskIdSet;
    private Integer eligibleCount;
}