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

package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.task.vo.PlatformMigrateReqVO;
import com.tencent.bk.codecc.task.vo.PlatformVO;

import java.util.List;

/**
 * Platform业务接口
 *
 * @version V1.0
 * @date 2019/9/30
 */
public interface PlatformService
{
    /**
     * 获取所有的Platform
     *
     * @return
     * @param toolName
     */
    List<PlatformVO> getPlatformByToolName(String toolName);

    /**
     * 获取任务的Platform IP
     *
     * @param taskId
     * @param toolName
     * @return
     */
    String getPlatformIp(long taskId, String toolName);


    /**
     * 根据工具名和IP获取Platform
     *
     * @return
     * @param toolName
     */
    PlatformVO getPlatformByToolNameAndIp(String toolName, String ip);

    /**
     * 多条件获取Platform信息
     *
     * @param toolName   工具名称
     * @param platformIp Platform IP
     * @return list
     */
    List<PlatformVO> getPlatformInfo(String toolName, String platformIp);

    Boolean batchMigratePlatformForTask(PlatformMigrateReqVO reqVO, String userName);

    Boolean rollsBackMigrateLog(PlatformMigrateReqVO reqVO, String userName);

    /**
     * 修改Platform配置
     *
     * @param userName   用户名
     * @param platformVO 请求体
     * @return boolean
     */
    Boolean updatePlatformInfo(String userName, PlatformVO platformVO);

    /**
     * 删除Platform配置
     *
     * @param platformVO 请求体
     * @return boolean
     */
    Boolean deletePlatformInfo(PlatformVO platformVO);

}
