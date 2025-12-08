package com.tencent.bk.codecc.task.model;

import com.tencent.codecc.common.db.CommonEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.checkerframework.common.aliasing.qual.Unique;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Set;

/**
 * 工具的辅助控制信息, 包括工具的可见范围, 开发语言等等信息.
 *
 * @date 2024/08/14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_tool_control")
public class ToolControlInfoEntity extends CommonEntity {
    /**
     * 工具名
     * TODO: 需要保证这个字段的值唯一
     */
    @Field("tool_name")
    private String toolName;
    /**
     * ComConstants.ToolIntegratedStatus.T;
     * 若 status = ComConstants.ToolIntegratedStatus.G, 表示正在灰度;
     * 若 status = ComConstants.ToolIntegratedStatus.P, 表示正在全量.
     */
    @Field("status")
    private Integer status;
    /**
     * 正在测试的版本号
     */
    @Field("test_version")
    private String testVersion;
    /**
     * 正在灰度/全量的版本号(CodeCC 后台的版本号)
     */
    @Field("version")
    private String version;
    /**
     * 最新的已发布版本号
     */
    @Field("latest_version")
    private String latestVersion;
    /**
     * 最新的已发布版本的展示版本号(用户填写的版本号)
     */
    @Field("latest_display_version")
    private String latestDisplayVersion;
    /**
     * 展示版本号(用户填写的版本号)
     */
    private String displayVersion;
    /**
     * 工具发布者
     */
    @Field("publisher")
    private String publisher;
    /**
     * 可以使用该工具的项目(的项目 id)
     */
    @Field("visible_projects")
    private Set<String> visibleProjects;
    /**
     * 可以使用该工具的组织(的组织 id, 以 tof 的数据为准)
     */
    @Field("visible_org_ids")
    private Set<String> visibleOrgIds;
    /**
     * 该工具的灰度项目, 当 status 为灰度时该字段有效
     */
    @Field("grayed_projects")
    private Set<String> grayedProjects;
    /**
     * 该工具的灰度组织(的组织 id, 以 tof 的数据为准)
     */
    @Field("grayed_org_ids")
    private Set<String> grayedOrgIds;
    /**
     * 工具开发语言
     */
    @Field("dev_language")
    private String devLanguage;
    /**
     * 工具类别中文版
     */
    @Field("tool_cn_types")
    private List<String> toolCnTypes;
    /**
     * 是否需要提供编译脚本
     */
    @Field("need_build_script")
    private Boolean needBuildScript;
    /**
     * 默认的规则集id集合, 如各种 xx_xx_all_checkers
     */
    @Field("default_checker_sets")
    private List<String> defaultCheckerSets;
}
