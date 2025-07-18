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

package com.tencent.bk.codecc.defect.config;

import com.tencent.bk.codecc.defect.service.*;
import com.tencent.bk.codecc.defect.service.specialparam.ISpecialParamService;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工厂类自动配置
 *
 * @version V1.0
 * @date 2019/5/27
 */
@Configuration
public class FactoryConfiguration
{
    @Bean
    public BizServiceFactory<IConfigCheckerPkgBizService> fileAndConfigCheckerPkgFactory()
    {
        return new BizServiceFactory<>();
    }

    @Bean
    public BizServiceFactory<IDataReportBizService> fileAndDataReportFactory()
    {
        return new BizServiceFactory<>();
    }

    @Bean
    public BizServiceFactory<IQueryWarningBizService> fileAndDefectQueryFactory()
    {
        return new BizServiceFactory<>();
    }

    @Bean
    public BizServiceFactory<IBizService> bizServiceFactory()
    {
        return new BizServiceFactory<>();
    }

    @Bean
    public BizServiceFactory<IQueryStatisticBizService> taskLogAndDefectFactory()
    {
        return new BizServiceFactory<>();
    }

    @Bean
    public BizServiceFactory<TreeService> treeServiceBizServiceFactory()
    {
        return new BizServiceFactory<>();
    }

    @Bean
    public BizServiceFactory<ICheckReportBizService> checkReportBizServiceBizServiceFactory()
    {
        return new BizServiceFactory<>();
    }

    @Bean
    public BizServiceFactory<ISpecialParamService> specialParamServiceBizServiceFactory()
    {
        return new BizServiceFactory<>();
    }

    @Bean
    public BizServiceFactory<IDefectOperateBizService> defectOperateBizServiceFactory()
    {
        return new BizServiceFactory<>();
    }

    @Bean
    public BizServiceFactory<ISCAQueryWarningService> scaQueryWarningBizServiceFactory() {
        return new BizServiceFactory<>();
    }

    @Bean
    public BizServiceFactory<ISCABatchDefectProcessBizService> scaBatchDefectProcessBizServiceFactory() {
        return new BizServiceFactory<>();
    }
}
