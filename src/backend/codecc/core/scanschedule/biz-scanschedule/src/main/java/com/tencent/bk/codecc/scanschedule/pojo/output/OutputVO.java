package com.tencent.bk.codecc.scanschedule.pojo.output;

import com.tencent.bk.codecc.scanschedule.vo.SimpleDefectVO;
import lombok.Data;

import java.util.List;

@Data
public class OutputVO {

    private List<SimpleDefectVO> defects; //告警列表
}
