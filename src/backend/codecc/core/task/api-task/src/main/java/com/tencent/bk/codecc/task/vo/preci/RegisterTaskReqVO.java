package com.tencent.bk.codecc.task.vo.preci;

import java.util.List;
import lombok.Data;

@Data
public class RegisterTaskReqVO {

    private String preCITaskId;
    private String userName;
    private String projectName;
    private List<String> codeLangList;
    private List<String> toolNameList;
    private List<String> checkerSetList;
    private String ideName;
    private String ideVersion;
    private String pluginVersion;
}
