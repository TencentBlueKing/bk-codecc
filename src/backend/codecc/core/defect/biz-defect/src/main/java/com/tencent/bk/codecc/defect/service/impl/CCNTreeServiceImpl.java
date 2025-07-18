/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.service.AbstractTreeService;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.PathUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CNN告警文件树
 *
 * @version V1.0
 * @date 2019/5/14
 */
@Service("CCNTreeBizService")
public class CCNTreeServiceImpl extends AbstractTreeService
{

    @Autowired
    private CCNDefectRepository ccnDefectRepository;

    @Override
    public Set<String> getDefectPaths(Long taskId, String toolName)
    {
        List<CCNDefectEntity> ccnFiles = ccnDefectRepository.findByTaskId(taskId);
        Set<String> defectPaths = new HashSet<>();
        // 排除路径屏蔽的的状态
        ccnFiles.stream()
                .filter(ccn -> (ccn.getStatus() & ComConstants.DefectStatus.PATH_MASK.value()) == 0)
                .forEach(fileInfo ->
                {
                    // 获取所有警告文件的绝对路径
                    String relativePath = PathUtils.getRelativePath(fileInfo.getUrl(), fileInfo.getRelPath());
                    if (StringUtils.isNotBlank(relativePath))
                    {
                        defectPaths.add(relativePath);
                    }
                    else
                    {
                        defectPaths.add(fileInfo.getFilePath());
                    }
                });

        return defectPaths;
    }


}
