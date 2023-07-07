package com.tencent.bk.codecc.scanschedule.pojo.record;

import com.tencent.bk.codecc.scanschedule.vo.SimpleCheckerSetVO;
import com.tencent.bk.codecc.scanschedule.vo.SimpleDefectVO;
import lombok.Data;

import java.util.List;

@Data
public class ScanRecord {

    /**
     * scanId
     */
    private String scanId;

    /**
     * 应用code
     */
    private String appCode;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 扫描内容
     */
    private String content;

    /**
     * 扫描规则集
     */
    private List<SimpleCheckerSetVO> checkerSets;

    /**
     * 扫描开始时间
     */
    private Long startTime;

    /**
     * 扫描结束时间
     */
    private Long endTime;

    /**
     * 扫描耗时
     */
    private Long elapseTime;

    /**
     * 扫描状态: 0：成功，1：失败
     */
    private int status;

    /**
     * 扫描失败信息
     */
    private String failMsg;

    /**
     * 扫描告警数
     */
    private int defectCount;

    /**
     * 告警列表
     */
    private List<SimpleDefectVO> defectList;

    /**
     * 工具扫描信息
     */
    private List<ToolRecord> toolRecordList;

}
