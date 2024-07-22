package com.tencent.bk.codecc.scanschedule.pojo.record;

import lombok.Data;

/**
 * 工具记录类
 * @author jimxzcai
 */
@Data
public class ToolRecord {

    /**
     * scanId
     */
    private String scanId;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具版本
     */
    private String toolVersion;

    /**
     * 工具运行命令
     */
    private String toolCommand;

    /**
     * 工具扫描开始时间
     */
    private Long startTime;

    /**
     * 工具扫描结束时间
     */
    private Long endTime;

    /**
     * 工具扫描耗时
     */
    private Long elapseTime;

    /**
     * 扫描状态: 0：成功，1：失败
     */
    private int status;

    /**
     * 扫描失败工具信息
     */
    private String failMsg;

    /**
     * 扫描告警数
     */
    private int defectCount;

}
