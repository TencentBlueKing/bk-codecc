package com.tencent.bk.codecc.scanschedule.vo;

import lombok.Data;

@Data
public class SimpleCheckerSetVO {

    /**
     * 规则集名称
     */
    private String checkerSet;

    /**
     * 规则集版本号
     */
    private int checkerSetVersion;
}
