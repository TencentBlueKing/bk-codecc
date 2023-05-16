package com.tencent.bk.codecc.scanschedule.service;

import com.tencent.bk.codecc.scanschedule.pojo.record.ScanRecord;
import com.tencent.bk.codecc.scanschedule.vo.ContentVO;

/**
 * 工具扫描
 */
public interface ToolScanService {

    ScanRecord generateScanRecord(String appCode, ContentVO contentVO);

    ScanRecord scan(ScanRecord scanRecord);

    void saveScanRecord(ScanRecord scanRecord);
}
