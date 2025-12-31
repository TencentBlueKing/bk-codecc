package com.tencent.bk.codecc.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "code.yml仓库所有者类")
public class CodeYmlRepoOwnerVO {

    private List<String> repoOwners;
}
