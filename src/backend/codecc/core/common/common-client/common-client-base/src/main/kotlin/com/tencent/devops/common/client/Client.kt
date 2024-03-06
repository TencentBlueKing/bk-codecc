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

package com.tencent.devops.common.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.ms.DevopsServiceTarget
import com.tencent.devops.common.client.ms.ThirdServiceTarget
import com.tencent.devops.common.client.pojo.AllProperties
import com.tencent.devops.common.client.proxy.DevopsProxy
import com.tencent.devops.common.exception.ClientException
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.util.TraceBuildIdThreadCacheUtils
import feign.Feign
import feign.Request
import feign.RequestInterceptor
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.jaxrs.JAXRSContract
import feign.okhttp.OkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.util.StringUtils
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

abstract class Client constructor(
    protected open val clientErrorDecoder: ClientErrorDecoder,
    protected open val allProperties: AllProperties,
    protected val objectMapper: ObjectMapper
) {

    /**
     * defect 服务名
     */
    private val SERVICE_NAME_DEFECT: String = "defect"

    /**
     * report 服务名
     */
    private val SERVICE_NAME_REPORT: String = "report"

    private val interfaces = ConcurrentHashMap<KClass<*>, String>()

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(5L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .writeTimeout(60L, TimeUnit.SECONDS)
            .build()

    protected val longRunClient = OkHttpClient(
        okhttp3.OkHttpClient.Builder()
                .connectTimeout(10L, TimeUnit.SECONDS)
                .readTimeout(30L, TimeUnit.MINUTES)
                .writeTimeout(30L, TimeUnit.MINUTES)
                .build()
    )

    protected val shortRunClient = OkHttpClient(
        okhttp3.OkHttpClient.Builder()
                .connectTimeout(5L, TimeUnit.SECONDS)
                .readTimeout(10L, TimeUnit.SECONDS)
                .writeTimeout(10L, TimeUnit.SECONDS)
                .build()
    )

    protected val feignClient = OkHttpClient(okHttpClient)
    protected val jaxRsContract = JAXRSContract()
    protected val jacksonDecoder = JacksonDecoder(objectMapper)
    protected val jacksonEncoder = JacksonEncoder(objectMapper)

    @Value("\${service.prefix:#{null}}")
    private val servicePrefix: String? = null

    @Value("\${service.suffix:#{null}}")
    private val serviceSuffix: String? = null

    companion object {

        const val codeccPackagePath = """com.tencent.bk.codecc.([a-z]+).api.([a-zA-Z]+)"""

        const val devopsPackagePath = """com.tencent.devops.([a-z]+).api.([a-zA-Z]+)"""
    }

    fun <T : Any> getDevopsService(clz: Class<T>): T {
        // 获取为feign定义的拦截器
        val feignProxy = Feign.builder()
                .client(feignClient)
                .errorDecoder(clientErrorDecoder)
                .encoder(JacksonEncoder(JsonUtil.getObjectMapper()))
                .decoder(JacksonDecoder(JsonUtil.getObjectMapper()))
                .contract(jaxRsContract)
                .options(Request.Options(10000, 30000))
                .requestInterceptor(
                    SpringContextUtil.getBean(
                        RequestInterceptor::class.java,
                        "devopsRequestInterceptor"
                    )
                )
                .target(
                    DevopsServiceTarget(
                        findThridServiceName(clz.kotlin), clz, allProperties.devopsDevUrl
                            ?: ""
                    )
                )
        val devopsProxy = DevopsProxy(feignProxy, clz, allProperties.devopsAutoTag)
        return clz.cast(
            Proxy.newProxyInstance(
                feignProxy.javaClass.classLoader,
                feignProxy.javaClass.interfaces,
                devopsProxy
            )
        )
    }

    fun <T : Any> getThirdService(clz: Class<T>, rootPath: String): T {
        // 获取为feign定义的拦截器
        val feignProxy = Feign.builder()
                .client(feignClient)
                .errorDecoder(clientErrorDecoder)
                .encoder(jacksonEncoder)
                .decoder(jacksonDecoder)
                .contract(jaxRsContract)
                .options(Request.Options(10000, 30000))
                .requestInterceptor(
                    SpringContextUtil.getBean(
                        RequestInterceptor::class.java,
                        "normalRequestInterceptor"
                    )
                ) // 获取为feign定义的拦截器
                .target(ThirdServiceTarget(findThridServiceName(clz.kotlin), clz, rootPath))
        val devopsProxy = DevopsProxy(feignProxy, clz, allProperties.devopsAutoTag)
        return clz.cast(
            Proxy.newProxyInstance(
                feignProxy.javaClass.classLoader,
                feignProxy.javaClass.interfaces, devopsProxy
            )
        )
    }

    fun <T : Any> getDevopsService(clz: Class<T>, projectId: String): T {
        // 获取为feign定义的拦截器
        DevopsProxy.projectIdThreadLocal.set(projectId)
        val feignProxy = Feign.builder()
                .client(feignClient)
                .errorDecoder(clientErrorDecoder)
                .encoder(JacksonEncoder(JsonUtil.getObjectMapper()))
                .decoder(JacksonDecoder(JsonUtil.getObjectMapper()))
                .contract(jaxRsContract)
                .options(Request.Options(10000, 30000))
                .requestInterceptor(
                    SpringContextUtil.getBean(
                        RequestInterceptor::class.java, "devopsRequestInterceptor"
                    )
                )
                .target(
                    DevopsServiceTarget(
                        findThridServiceName(clz.kotlin), clz,
                        allProperties.devopsDevUrl
                            ?: ""
                    )
                )
        val devopsProxy = DevopsProxy(feignProxy, clz, allProperties.devopsAutoTag)
        return clz.cast(
            Proxy.newProxyInstance(feignProxy.javaClass.classLoader, feignProxy.javaClass.interfaces, devopsProxy)
        )
    }

    /**
     * 短时间响应的客户端  5秒连接，10秒收发
     */
    fun <T : Any> getShortRunDevopsService(clz: Class<T>): T {
        // 获取为feign定义的拦截器
        val feignProxy = Feign.builder()
                .client(shortRunClient)
                .errorDecoder(clientErrorDecoder)
                .encoder(JacksonEncoder(JsonUtil.getObjectMapper()))
                .decoder(JacksonDecoder(JsonUtil.getObjectMapper()))
                .contract(jaxRsContract)
                .options(Request.Options(5000, 10000))
                .requestInterceptor(
                    SpringContextUtil.getBean(
                        RequestInterceptor::class.java,
                        "devopsRequestInterceptor"
                    )
                )
                .target(
                    DevopsServiceTarget(
                        findThridServiceName(clz.kotlin),
                        clz, allProperties.devopsDevUrl
                            ?: ""
                    )
                )
        val devopsProxy = DevopsProxy(feignProxy, clz, allProperties.devopsAutoTag)
        return clz.cast(
            Proxy.newProxyInstance(
                feignProxy.javaClass.classLoader,
                feignProxy.javaClass.interfaces, devopsProxy
            )
        )
    }

    fun <T : Any> get(clz: Class<T>): T = get(clz.kotlin)

    abstract fun <T : Any> get(clz: KClass<T>): T

    abstract fun <T : Any> getWithSpecialTag(clz: Class<T>, specialTag: String): T

    /**
     * 不带任何公共URL前缀构建feign
     */
    abstract fun <T : Any> getNoneUrlPrefix(clz: Class<T>): T

    abstract fun <T : Any> getWithoutRetry(clz: KClass<T>): T

    abstract fun <T : Any> getWithoutRetry(clz: Class<T>): T

    protected fun findServiceName(clz: KClass<*>): String {
        return findServiceName(clz, servicePrefix, serviceSuffix)
    }

    private fun findThridServiceName(clz: KClass<*>): String {
        return findServiceName(clz, "", "")
    }

    private fun findServiceName(clz: KClass<*>, prefix: String?, suffix: String?): String {
        var serviceName = interfaces.getOrPut(clz) {
            val serviceInterface = AnnotationUtils.findAnnotation(clz.java, ServiceInterface::class.java)
            if (serviceInterface != null) {
                serviceInterface.value
            } else {
                val packageName = clz.qualifiedName.toString()
                val codeccRegex = Regex(codeccPackagePath)
                val devopsRegex = Regex(devopsPackagePath)
                val matches = codeccRegex.find(packageName) ?: devopsRegex.find(packageName)
                ?: throw ClientException("无法根据接口[$packageName]分析所属的服务")
                matches.groupValues[1]
            }
        }

        /**
         * 如果缓存中存在BuildId,就标识来源于Build接口，请求转发到Report服务
         */
        val traceBuildId = TraceBuildIdThreadCacheUtils.getBuildId()
        if (serviceName == SERVICE_NAME_DEFECT && StringUtils.hasLength(traceBuildId)) {
            serviceName = SERVICE_NAME_REPORT
        }
        return getServiceNameWithPrefix(serviceName, prefix, suffix)
    }

    protected fun getServiceNameWithPrefix(serviceName: String): String {
        return getServiceNameWithPrefix(serviceName, servicePrefix, serviceSuffix)
    }

    private fun getServiceNameWithPrefix(serviceName: String, prefix: String?, suffix: String?): String {
        return if (prefix.isNullOrBlank() && suffix.isNullOrBlank()) {
            serviceName
        } else if (suffix.isNullOrBlank()) {
            "$prefix$serviceName"
        } else {
            "$serviceName$suffix"
        }
    }

    abstract fun getServiceNodeNum(serviceName: String): Int

}
