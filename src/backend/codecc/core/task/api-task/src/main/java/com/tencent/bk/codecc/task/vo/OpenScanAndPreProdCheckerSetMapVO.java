package com.tencent.bk.codecc.task.vo;

import com.tencent.bk.codecc.task.vo.checkerset.OpenSourceCheckerSetVO;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.List;

/**
 * 开源治理/EPC对应规则集下，所对应的工具映射
 */
@Data
@NoArgsConstructor
@ApiModel("开源治理或预发布版规则集所有对应规则集映射")
public class OpenScanAndPreProdCheckerSetMapVO {

    /**
     * 开源治理对应的规则集
     */
    private Map<String, List<OpenSourceCheckerSetVO>> prodOpenScan;

    private Map<String, List<OpenSourceCheckerSetVO>> prodCommunityOpenScan;

    private Map<String, List<OpenSourceCheckerSetVO>> prodEpcScan;

    private Map<String, List<OpenSourceCheckerSetVO>> prodPrivateScan;

    /**
     * 预发布版对应的规则集
     */
    private Map<String, List<OpenSourceCheckerSetVO>> preProdOpenScan;

    private Map<String, List<OpenSourceCheckerSetVO>> preProdCommunityOpenScan;

    private Map<String, List<OpenSourceCheckerSetVO>> preProdEpcScan;

    private Map<String, List<OpenSourceCheckerSetVO>> preProdPrivateScan;

    private TimeUnit prodOpenScanTimeGap;

    private TimeUnit preProdOpenScanTimeGap;

    private TimeUnit prodCommunityOpenScanTimeGap;

    private TimeUnit preProdCommunityOpenScanTimeGap;

    private TimeUnit prodEpcScanTimeGap;

    private TimeUnit preProdEpcScanTimeGap;

    private TimeUnit prodPrivateScanTimeGap;

    private TimeUnit preProdPrivateScanTimeGap;


    @Data
    @AllArgsConstructor
    public static class TimeUnit {
        private String startTime;

        private String endTime;
    }
}
