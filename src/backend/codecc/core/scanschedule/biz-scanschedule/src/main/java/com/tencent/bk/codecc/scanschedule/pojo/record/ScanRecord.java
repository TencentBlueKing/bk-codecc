package com.tencent.bk.codecc.scanschedule.pojo.record;

import com.tencent.bk.codecc.scanschedule.vo.SimpleCheckerSetVO;
import com.tencent.bk.codecc.scanschedule.vo.SimpleDefectVO;
import lombok.Data;

import java.util.List;

/**
 * 扫描记录类
 * @author jimxzcai
 */
@Data
public class ScanRecord {

    /**
     * scanId
     */
    public String scanId;

    /**
     * 应用code
     */
    private String appCode;

    /**
     * 用户名
     */
    public String userName;

    /**
     * 扫描内容
     */
    public String content;

    /**
     * 扫描规则集
     */
    public List<SimpleCheckerSetVO> checkerSets;

    /**
     * 扫描开始时间
     */
    public Long startTime;

    /**
     * 扫描结束时间
     */
    public Long endTime;

    /**
     * 扫描耗时
     */
    public Long elapseTime;

    /**
     * 扫描状态: 0：成功，1：失败
     */
    public int status;

    /**
     * 扫描失败信息
     */
    public String failMsg;

    /**
     * 扫描告警数
     */
    public int defectCount;

    /**
     * 告警列表
     */
    public List<SimpleDefectVO> defectList;

    /**
     * 工具扫描信息
     */
    public List<ToolRecord> toolRecordList;

}
