package com.tencent.bk.codecc.defect.model.sca

class SCASbomAggregateModel(
    /**
     * 任务ID
     */
    var taskId: Long,
    /**
     * 工具名称
     */
    var toolName: String,
    /**
     * 基础信息
     */
    var info: SCASbomInfoEntity,
    /**
     * 组件信息
     */
    var packages: List<SCASbomPackageEntity>,
    /**
     * 文件信息
     */
    var files: List<SCASbomFileEntity>,
    /**
     * 代码片段信息
     */
    var snippets: List<SCASbomSnippetEntity>,
    /**
     * 关联关系
     */
    var relationships: List<SCASbomRelationshipEntity>,
)
