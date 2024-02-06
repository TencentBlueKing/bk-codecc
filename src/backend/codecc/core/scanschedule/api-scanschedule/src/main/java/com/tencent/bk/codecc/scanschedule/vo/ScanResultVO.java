package com.tencent.bk.codecc.scanschedule.vo;

import lombok.Data;

import java.util.List;

/**
 * 扫描结果
 * @author jimxzcai
 */
@Data
public class ScanResultVO {

    /**
     * scanId
     */
    private String scanId;

    /**
     * 扫描状态
     */
    private int status;

    /**
     * 失败信息
     */
    private String failMsg;

    /**
     * 告警列表信息
     */
    private List<SimpleDefectVO> defectList;
}
