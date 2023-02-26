package com.tencent.bk.codecc.task.vo.preci;

import java.util.List;
import lombok.Data;

@Data
public class ReportScanLogReqVO {

    public String preCITasKId;
    public List<ScanLogVO> scanLogVOList;

    @Data
    public static class ScanLogVO {

        private String toolName;
        private int times;
        private long updateTime;
        private long byDaily;
    }
}
