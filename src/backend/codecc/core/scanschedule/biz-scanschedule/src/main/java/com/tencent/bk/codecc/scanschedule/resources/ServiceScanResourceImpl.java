/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.scanschedule.resources;

import com.tencent.bk.codecc.scanschedule.api.ServiceScanResource;
import com.tencent.bk.codecc.scanschedule.pojo.record.ScanRecord;
import com.tencent.bk.codecc.scanschedule.service.ToolScanService;
import com.tencent.bk.codecc.scanschedule.vo.ScanResultVO;
import com.tencent.bk.codecc.scanschedule.vo.ContentVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.web.RestResource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Collectors;

/**
 * 工具扫描接口，支持代码片段扫描
 *
 * @version V1.0
 * @date 2023/14/17
 */
@RestResource
public class ServiceScanResourceImpl implements ServiceScanResource {
    @Autowired
    private ToolScanService toolScanService;


    @Override
    public Result<ScanResultVO> scan(String appCode, ContentVO contentVO) {
        //初始化扫描记录
        ScanRecord scanRecord = toolScanService.generateScanRecord(appCode, contentVO);
        //初始化
        scanRecord = toolScanService.initScan(scanRecord);
        if (scanRecord.getStatus() != 1
                && CollectionUtils.isNotEmpty(scanRecord.getToolRecordList())
                && scanRecord.getToolRecordList()
                .stream().filter(it -> it.getStatus() == 1)
                .collect(Collectors.toList()).isEmpty()) {
            //触发扫描
            scanRecord = toolScanService.scan(scanRecord);
        }
        //保存扫描记录
        toolScanService.saveScanRecord(scanRecord);
        //返回结果
        ScanResultVO scanResultVO = new ScanResultVO();
        BeanUtils.copyProperties(scanRecord, scanResultVO);
        return new Result<>(scanResultVO);
    }
}
