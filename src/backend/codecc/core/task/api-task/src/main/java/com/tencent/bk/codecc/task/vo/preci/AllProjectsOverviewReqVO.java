package com.tencent.bk.codecc.task.vo.preci;

import lombok.Data;

import java.util.List;

@Data
public class AllProjectsOverviewReqVO {

    private String userName;
    private String gitUrl;
    private String ide;
    private String ideVersion;
    private String preCiVersion;
}
