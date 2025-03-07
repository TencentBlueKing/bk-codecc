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

package com.tencent.devops.common.constant

/**
 * 业务错误码
 * 返回码制定规则：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表平台（如23代表CodeCC平台）
 * 3、第3位和第4位数字代表子服务模块（00：common-公共模块 01：task-任务模块 02：rule-规则模块 03:defect-告警模块 04:coverity-Coverity模块 05:schedule-调度模块）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如0001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 */
object CommonMessageCode {

    const val DB_QUERY_TIME_OUT= "-99"

    /**
     * 成功
     */
    const val SUCCESS = "0"

    /**
     * 系统内部繁忙，请稍后再试
     */
    const val SYSTEM_ERROR = "2300001"

    /**
     * {0}不能为空
     */
    const val PARAMETER_IS_NULL = "2300002"

    /**
     * {0}已经存在系统，请换一个再试
     */
    const val KEY_IS_EXIST = "2300003"

    /**
     * {0}为非法数据
     */
    const val PARAMETER_IS_INVALID = "2300004"

    /**
     * 无效的token，请先oauth认证
     */
    const val OAUTH_TOKEN_IS_INVALID = "2300005"

    /**
     * 查看代码内容文件过大
     */
    const val FILE_CONTENT_TOO_LARGE = "2100019"

    /**
     *  {0}无权限
     */
    const val PERMISSION_DENIED = "2300006"

    /**
     * [{0}{1}]记录不存在
     */
    const val RECORD_NOT_EXITS = "2300007"

    /**
     * {0}记录已经存在
     */
    const val RECORD_EXIST = "2300008"

    /**
     * 调用第三方接口失败，请查询日志
     */
    const val THIRD_PARTY_SYSTEM_FAIL = "2300009"

    /**
     * 调用内部服务接口失败，请查询日志
     */
    const val INTERNAL_SYSTEM_FAIL = "2300010"

    /**
     * 调用蓝盾接口失败，请查询日志
     */
    const val BLUE_SHIELD_INTERNAL_ERROR = "2300011"

    /**
     * 代码运行失败，请查看日志
     */
    const val UTIL_EXECUTE_FAIL = "2300012"

    /**
     * 找不到对应的处理器
     */
    const val NOT_FOUND_PROCESSOR = "2300013"

    /**
     * 工具{0}是无效工具，检查参数是否正确或者稍后重试
     */
    const val INVALID_TOOL_NAME = "2300014"

    /**
     * 找不到任何有效的{0}服务提供者
     */
    const val ERROR_SERVICE_NO_FOUND = "2300015"

    /**
     * 定时任务集群启动失败
     */
    const val SCHEDULE_TASK_ERROR = "2300016"

    /**
     * 非管理员，操作失败
     */
    const val IS_NOT_ADMIN_MEMBER = "2300017"

    /**
     * 查看工蜂告警代码文件失败
     */
    const val CODE_CONTENT_ERROR = "2300018"

    /**
     * 查看告警代码文件失败
     */
    const val CODE_NORMAL_CONTENT_ERROR = "2300019"

    /**
     * 通过json文件传入的参数非法
     */
    const val JSON_PARAM_IS_INVALID = "2300020"

    /**
     * 提单失败
     */
    const val ISSUE_SUBMIT_ERROR = "2300021"

    /**
     * 任务提单没oauth授权
     */
    const val ISSUE_SUBMIT_NOT_OAUTH_ERROR = "2300022"

    /**
     * 任务提单没oauth授权
     */
    const val REACH_LIMIT = "2300023"

    const val UNKNOWN_ERROR = "2300024"

    const val INTERNAL_SERVICE_ERROR = "2300025"

    const val IO_ERROR = "2300026"

    const val QUERY_PARAM_REQUEST_ERROR = "2300027"

    const val REQUEST_BODY_PARAM_ERROR = "2300028"

    const val REGULAR_EXP_INVALID = "2300029"

    const val UNAUTHORIZED_ACCESS_TO_RESOURCES = "2300030"

    /**
     * 项目告警数过多
     */
    const val PROJECT_DEFECT_TOO_MANY = "2300031"

    /**
     * 任务告警数过多
     */
    const val TASK_DEFECT_TOO_MANY = "2300032"

    /**
     * 不允许存在多个灰度工具的规则
     */
    const val NOT_ALLOW_MULTI_GRAY_TOOL_RULE = "2300033"

    /**
     * 记录的生效范围重复
     */
    const val RECORD_RANGE_IS_DUPLICATED = "2300034"

    /**
     * 批量请求过多
     */
    const val BATCH_REQUEST_TOO_MANY = "2303035"

    /**
     * 表示资源资源未找到的状态码
     */
    const val RESOURCE_NOT_FOUND = "2121036"

    /**
     * 操作的数量为空
     */
    const val MODIFY_ZERO_ENTITY = "2300037"
}
