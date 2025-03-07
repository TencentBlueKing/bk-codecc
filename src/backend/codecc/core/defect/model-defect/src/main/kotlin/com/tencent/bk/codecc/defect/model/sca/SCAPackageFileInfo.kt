package com.tencent.bk.codecc.defect.model.sca

import org.springframework.data.mongodb.core.mapping.Field

class SCAPackageFileInfo(
    /**
     * 文件名称
     */
    @Field("file_name")
    var fileName: String? = null,
    /**
     * 文件绝对路径
     */
    @Field("file_path")
    var filePath: String? = null,
    /**
     * 文件相对路径
     */
    @Field("rel_path")
    var relPath: String? = null,
    /**
     * 引入的文件
     */
    @Field("file_element_id")
    var fileElementId: String? = null,

    /**
     * 引入的代码片段列表
     */
    @Field("snippet_element_ids")
    var snippetElementIds: List<String>? = null,
    /**
     * 作者列表
     */
    @Field("authors")
    var authors: List<String>? = null,
    /**
     * 最后更新时间
     */
    @Field("last_update_time")
    var lastUpdateTime: Long = 0L,
)
