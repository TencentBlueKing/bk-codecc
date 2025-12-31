package com.tencent.bk.codecc.defect.vo.report;

import com.tencent.devops.common.constant.ComConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 圈复杂度数据报表作者信息视图
 *
 * @version V1.0
 * @date 2019/12/4
 */
@Data
@Schema(description = "圈复杂度数据报表作者信息视图")
public class CCNChartAuthorVO extends ChartAuthorBaseVO {
    @Schema(description = "超高级别数量")
    private Integer superHigh;

    @Schema(description = "高级别数量")
    private Integer high;

    @Schema(description = "中级别数量")
    private Integer medium;

    @Schema(description = "低级别数量")
    private Integer low;

    public CCNChartAuthorVO() {
        total = 0;
        superHigh = 0;
        high = 0;
        medium = 0;
        low = 0;
    }

    public CCNChartAuthorVO(String author) {
        authorName = author;
        total = 0;
        superHigh = 0;
        high = 0;
        medium = 0;
        low = 0;
    }

    public void add(CCNChartAuthorVO b) {
        this.total += b.getTotal();
        this.superHigh += b.getSuperHigh();
        this.high += b.getHigh();
        this.medium += b.getMedium();
        this.low += b.getLow();
    }

    @Override
    public Integer getTotal() {
        return superHigh + high + medium + low;
    }

    public void count(int severity) {
        if (severity == ComConstants.RiskFactor.SH.value()) {
            superHigh++;
        } else if (severity == ComConstants.RiskFactor.H.value()) {
            high++;
        } else if (severity == ComConstants.RiskFactor.M.value()) {
            medium++;
        } else if (severity == ComConstants.RiskFactor.L.value()) {
            low++;
        }
    }
}
