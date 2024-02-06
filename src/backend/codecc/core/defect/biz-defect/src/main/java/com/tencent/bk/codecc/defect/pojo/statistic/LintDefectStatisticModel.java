package com.tencent.bk.codecc.defect.pojo.statistic;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.model.CheckerStatisticEntity;
import com.tencent.bk.codecc.defect.model.NotRepairedAuthorEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.model.statistic.LintStatisticEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.ToString;

/**
 * 用于保存 lint 类工具在统计流程中需要的初始化指标和计算出的中间结果和最终结果
 * 计算完成后通过 builder convert 为表实体类保存
 *
 * @author warmli
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LintDefectStatisticModel extends AbstractDefectStatisticModel {

    protected LintDefectStatisticModel() {
    }

    private int totalOldSerious;

    private int totalOldNormal;

    private int totalOldPrompt;

    private int totalNewSerious;

    private int totalNewNormal;

    private int totalNewPrompt;

    private int fileChange;

    private int defectChange;

    private LintStatisticEntity lintStatisticEntity;

    private Set<String> filePathSet = Sets.newHashSet();

    private List<LintDefectV2Entity> allDefects;

    private List<LintDefectV2Entity> allNewDefects = Lists.newArrayList();

    private List<CheckerStatisticEntity> checkerStatisticList = Lists.newArrayList();

    private Map<String, NotRepairedAuthorEntity> authorDefectMap = Maps.newHashMap();

    private Map<String, NotRepairedAuthorEntity> existAuthorMap = Maps.newHashMap();

    private LintDefectStatisticModelBuilder builder;

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
