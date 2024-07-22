package com.tencent.bk.codecc.scanschedule.vo;

import lombok.Data;

/**
 * 简单规则集信息
 * @author jimxzcai
 */
@Data
public class SimpleCheckerSetVO {

    /**
     * 规则集名称
     */
    private String checkerSet;

    /**
     * 规则集版本号
     */
    private String checkerSetVersion;
}
