package com.tencent.bk.codecc.defect.vo.sca;

import lombok.Data;

@Data
public class SCAPackageFileInfoVO {
    /**
     * 文件名称
     */
    private String fileName;
    /**
     * 文件绝对路径
     */
    private String filePath;
    /**
     * 文件相对路径
     */
    private String relPath;
}
