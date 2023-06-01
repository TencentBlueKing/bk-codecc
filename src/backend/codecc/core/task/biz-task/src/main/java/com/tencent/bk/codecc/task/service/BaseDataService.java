/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

import com.tencent.bk.codecc.task.vo.DefaultFilterPathVO;
import com.tencent.bk.codecc.task.vo.ReleaseDateVO;
import com.tencent.bk.codecc.task.vo.checkerset.OpenSourceCheckerSetVO;
import com.tencent.bk.codecc.task.vo.pipeline.PipelineTaskLimitVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.pojo.Page;

import java.util.List;
import java.util.Set;

/**
 * 基础数据服务代码
 *
 * @version V1.0
 * @date 2019/5/28
 */
public interface BaseDataService {
    /**
     * 根据参数类型和参数代码查询信息
     *
     * @param paramType
     * @param paramCode
     * @return
     */
    List<BaseDataVO> findBaseDataInfoByTypeAndCode(String paramType, String paramCode);

    /**
     * 根据参数类型和参数代码查询信息
     *
     * @param paramType
     * @param paramCodeList
     * @return
     */
    List<BaseDataVO> findBaseDataInfoByTypeAndCodeList(String paramType, List<String> paramCodeList);

    /**
     * 根据参数类型查询参数列表
     *
     * @param paramType
     * @return
     */
    List<BaseDataVO> findBaseDataInfoByType(String paramType);

    /**
     * 根据参数类型，参数码和参数值查询
     *
     * @param paramType
     * @param paramCode
     * @param paramValue
     * @return
     */
    List<BaseDataVO> findBaseDataInfoByTypeAndCodeAndValue(String paramType, String paramCode, String paramValue);

    /**
     * 保存元数据
     */
    int batchSave(String userId, List<BaseDataVO> baseDataVOList);

    /**
     * 删除元数据
     */
    int deleteById(String id);


    /**
     * 更新屏蔽用户名单
     *
     * @param baseDataVO vo
     * @return boolean
     */
    Boolean updateExcludeUserMember(BaseDataVO baseDataVO, String userName);


    /**
     * 获取用户名单
     *
     * @return list
     */
    List<String> queryMemberListByParamType(String paramType);


    /**
     * 更新管理员名单
     *
     * @param baseDataVO vo
     * @return boolean
     */
    Boolean updateAdminMember(BaseDataVO baseDataVO, String userName);

    /**
     * 获取基础元数据
     */
    List<BaseDataVO> findBaseData();

    Boolean updateOpenSourceCheckerSetByLang(OpenSourceCheckerSetVO reqVO, String userName);

    /**
     * 删除开源扫描规则集配置
     *
     * @param reqVO    请求体
     * @param userName 用户名
     * @return boolean
     */
    Boolean deleteOpenSourceCheckerSetByLang(OpenSourceCheckerSetVO reqVO, String userName);


    List<BaseDataVO> getPreCICheckerSetList();

    /**
     * 编辑PreCI规则集配置
     *
     * @param userName 用户名
     * @param reqVO    请求体
     * @return boolean
     */
    Boolean editPreCICheckerSet(String userName, BaseDataVO reqVO);

    /**
     * 删除PreCI规则集配置
     *
     * @param reqVO 请求体
     * @return boolean
     */
    Boolean deletePreCICheckerSet(BaseDataVO reqVO);

    /**
     * 根据参数代码获取数据
     *
     * @param paramCode
     * @return
     */
    BaseDataVO findBaseDataByCode(String paramCode);

    ReleaseDateVO getReleaseDate(String manageType, String versionType);

    Boolean updateReleaseDate(ReleaseDateVO reqVO, String userName);

    Set<String> updateLangPreProdConfig(List<CheckerSetVO> checkerSetVOS);

    /**
     * 分页获取系统默认屏蔽路径列表
     *
     * @param paramValue 搜索参数
     * @param pageNum    页码
     * @param pageSize   每页数量
     * @param sortField  排序字段
     * @param sortType   排序类型
     * @return page
     */
    Page<DefaultFilterPathVO> getDefaultFilterPathList(String paramValue, Integer pageNum, Integer pageSize,
                                                       String sortField, String sortType);

    /**
     * 删除一条系统默认屏蔽路径
     *
     * @param paramValue 屏蔽路径
     */

    Boolean deleteBaseDataEntityByParamValue(String paramValue);

    /**
     * 新增一条系统默认屏蔽路径
     *
     * @param paramValue 屏蔽路径
     * @param createBy   新增路径的管理员名称
     */

    Boolean insertBaseDataEntityByParamValueAndCreatedBy(String paramValue, String createBy);

    /**
     * 配置preCI用户登录列表初始化天数
     *
     * @param daily 初始化天数
     * @return boolean
     */
    Boolean configurePreCIInitDaily(String daily);

    Page<PipelineTaskLimitVO> queryPipelineTaskLimitPage(String paramCode, Integer pageNum, Integer pageSize);

    Boolean updatePipelineTaskLimit(PipelineTaskLimitVO reqVO);

    Boolean deletePipelineTaskLimit(String entityId, String userId);

    /**
     * 配置工具/语言顺序
     *
     * @param paramType 区分工具/语言
     * @param reqVO     更新的值
     * @return boolean
     */
    Boolean editToolLangSort(String paramType, BaseDataVO reqVO);

    /**
     * 根据参数类型，参数名和参数值更新数据
     *
     * @return boolean
     */
    Boolean updateByParamTypeAndParamNameAndParamValue(String paramType, String paramName, String paramValue,
            long updatedDate, String updateBy);
}
