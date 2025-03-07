package com.tencent.bk.codecc.defect.vo.checkerset;

import com.tencent.devops.common.api.OrgInfoVO;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
public class CheckerSetPackageVO {

    private String lang;

    /**
     * 语言
     */
    private Long langValue;

    /**
     * 规则集包类型
     */
    private String type;

    /**
     * 环境类型
     */
    private String envType;

    /**
     * 规则集ID
     */
    private String checkerSetId;

    /**
     * 规则集类型
     */
    private String checkerSetType;

    /**
     * 可见范围
     */
    private List<OrgInfoVO> scopes;

    /**
     * 可见范围-任务创建来源
     */
    private List<String> taskCreateFromScopes;

    /**
     * 版本
     */
    private Integer version;

    /**
     * 最新版本
     */
    private Integer lastVersion;

    /**
     * 工具列表
     */
    private Set<String> toolList;


}
