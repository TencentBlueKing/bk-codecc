package com.tencent.bk.codecc.defect.pojo.statistic;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.model.statistic.CommonStatisticEntity;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用于保存编译类工具在统计流程中需要的初始化指标和计算出的中间结果和最终结果
 * 计算完成后通过 builder convert 为表实体类保存
 *
 * @author warmli
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommonDefectStatisticModel extends AbstractDefectStatisticModel {

    protected CommonDefectStatisticModel() {
    }

    private int totalOldSerious;

    private int totalOldNormal;

    private int totalOldPrompt;

    private int totalNewSerious;

    private int totalNewNormal;

    private int totalNewPrompt;

    private CommonStatisticEntity commonStatisticEntity;

    private List<CommonDefectEntity> allDefects;

    private List<CommonDefectEntity> allNewDefects = Lists.newArrayList();

    private Set<String> newAuthors = Sets.newHashSet();

    private Set<String> oldAuthors = Sets.newHashSet();

    private Set<String> newPromptAuthors = Sets.newHashSet();

    private Set<String> newNormalAuthors = Sets.newHashSet();

    private Set<String> newSeriousAuthors = Sets.newHashSet();

    private Set<String> oldPromptAuthors = Sets.newHashSet();

    private Set<String> oldNormalAuthors = Sets.newHashSet();

    private Set<String> oldSeriousAuthors = Sets.newHashSet();

    private CommonDefectStatisticModelBuilder builder;

    public void incTotalOldSerious() {
        totalOldSerious++;
    }

    public void incTotalOldNormal() {
        totalOldNormal++;
    }

    public void incTotalOldPrompt() {
        totalOldPrompt++;
    }

    public void incTotalNewSerious() {
        totalNewSerious++;
    }

    public void incTotalNewNormal() {
        totalNewNormal++;
    }

    public void incTotalNewPrompt() {
        totalNewPrompt++;
    }
}
