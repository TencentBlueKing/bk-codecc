package com.tencent.bk.codecc.defect.model.redline;

import com.tencent.bk.codecc.defect.model.defect.DefectEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 红线数据生成时额外传输参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedLineExtraParams<T extends DefectEntity> {

    /**
     * 忽略告警列表
     */
    List<T> ignoreDefectList;
}
