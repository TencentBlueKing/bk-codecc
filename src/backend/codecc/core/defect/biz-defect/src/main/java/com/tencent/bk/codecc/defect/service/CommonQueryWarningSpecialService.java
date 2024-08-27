package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.DefectDetailVO;

/**
 * 减少原代码侵入，数据迁移后，前端common服务扩展
 */
public interface CommonQueryWarningSpecialService {

    /**
     * 获取告警文件内容
     *
     * @param defectDetailVO
     * @return
     */
    DefectDetailVO getFilesContent(DefectDetailVO defectDetailVO);

    /**
     * 获取告警文件信息
     *
     * @date 2023/6/13
     * @param defectDetailVO
     * @return
     */
    DefectDetailVO getFilesInfo(DefectDetailVO defectDetailVO);
}
