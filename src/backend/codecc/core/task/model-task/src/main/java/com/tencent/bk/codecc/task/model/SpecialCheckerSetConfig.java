package com.tencent.bk.codecc.task.model;

import lombok.Data;

import java.util.List;

@Data
public class SpecialCheckerSetConfig {

    /**
     * 社区正式版
     */
    private List<OpenSourceCheckerSet> prodCommunityOpenScan;

    /**
     * 社区预发布版
     */
    private List<OpenSourceCheckerSet> preProdCommunityOpenScan;

    /**
     * 内网正式版
     */
    private List<OpenSourceCheckerSet> openSourceCheckerSets;

    /**
     * 内网预发布版
     */
    private List<OpenSourceCheckerSet> preProdOpenSourceCheckerSets;

    /**
     * EPC正式版
     */
    private List<OpenSourceCheckerSet> epcCheckerSets;

    /**
     * EPC预发布版
     */
    private List<OpenSourceCheckerSet> preProdEpcCheckerSets;
}
