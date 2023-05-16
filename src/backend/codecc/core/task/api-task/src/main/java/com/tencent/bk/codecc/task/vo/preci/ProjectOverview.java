package com.tencent.bk.codecc.task.vo.preci;

import lombok.Data;

@Data
public class ProjectOverview {

    private String projectId;

    private String projectName;

    private long taskId;

    private String nameEn;

    private String nameCn;

    private String username;

    private long defectCount;

    private long securityCount;

    private long standardCount;

    private long riskCount;

    private long dupFileCount;
}
