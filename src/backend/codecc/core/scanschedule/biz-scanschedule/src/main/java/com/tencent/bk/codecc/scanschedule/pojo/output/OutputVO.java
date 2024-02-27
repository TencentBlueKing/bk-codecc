package com.tencent.bk.codecc.scanschedule.pojo.output;

import com.tencent.bk.codecc.scanschedule.vo.SimpleDefectVO;
import lombok.Data;

import java.util.List;

/**
 * 输出对象类
 * @author jimxzcai
 */
@Data
public class OutputVO {

    /**
     * 告警列表
     */
    public List<SimpleDefectVO> defects;
}
