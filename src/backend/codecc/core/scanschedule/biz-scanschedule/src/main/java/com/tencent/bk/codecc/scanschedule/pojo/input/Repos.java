package com.tencent.bk.codecc.scanschedule.pojo.input;

import lombok.Data;

@Data
public class Repos {

    private String url; //scm url
    private String scmType; //scm 类型
}
