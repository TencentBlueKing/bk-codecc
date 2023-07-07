package com.tencent.bk.codecc.task.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("code.yml仓库所有者类")
public class CodeYmlRepoOwnerVO {

    private List<String> repoOwners;
}
