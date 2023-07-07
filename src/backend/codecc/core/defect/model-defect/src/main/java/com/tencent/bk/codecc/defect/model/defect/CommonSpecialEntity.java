package com.tencent.bk.codecc.defect.model.defect;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * common数据迁移，特有字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommonSpecialEntity extends DefectEntity {

    /**
     * 规则类型，对应Coverity Platform中的Category(类别)
     */
    @Field("display_category")
    private String displayCategory;

    /**
     * 类型子类，对应Coverity Platform中的Type(类型)
     * 同时也是问题描述
     */
    @Field("display_type")
    private String displayType;

    /**
     * 对应第三方缺陷管理系统的ID，这里声明为字符串可以有更好的兼容性
     */
    @Field("ext_bug_id")
    private String extBugId;

    /**
     * 文件对应仓库版本号
     */
    @Field("file_version")
    private String fileVersion;

    /**
     * 第三方平台的buildId
     */
    @Field("platform_build_id")
    private String platformBuildId;

    /**
     * 第三方平台的项目ID
     */
    @Field("platform_project_id")
    private String platformProjectId;

    /**
     * 流名称
     */
    @Field("stream_name")
    private String streamName;
}
