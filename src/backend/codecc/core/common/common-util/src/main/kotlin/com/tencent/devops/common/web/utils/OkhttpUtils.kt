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

package com.tencent.devops.common.util

import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.TypeReference
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.constant.CommonMessageCode
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object OkhttpUtils {

    private val logger = LoggerFactory.getLogger(OkhttpUtils::class.java)

    val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60L, TimeUnit.SECONDS)
        .readTimeout(30L, TimeUnit.MINUTES)
        .writeTimeout(30L, TimeUnit.MINUTES)
        .build()!!

    val shortRunHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(5L, TimeUnit.SECONDS)
        .readTimeout(10L, TimeUnit.SECONDS)
        .writeTimeout(10L, TimeUnit.SECONDS)
        .build()!!

    fun doGet(url: String, headers: Map<String, String> = mapOf()): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
        if (headers.isNotEmpty()) {
            headers.forEach { key, value ->
                requestBuilder.addHeader(key, value)
            }
        }
        val request = requestBuilder.build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("request failed, url:$url message: ${response.message}")
                throw CodeCCException(CommonMessageCode.THIRD_PARTY_SYSTEM_FAIL)
            }
            return responseContent
        }
    }

    /**
     * 快速 get 请求. 连接超时时间: 5s, 读超时时间: 10s, 写超时时间: 10s.
     *
     * @param url get 请求的 url 地址
     * @param headers get 请求头
     * @return 返回 get 请求所得到的报文
     */
    fun doShortGet(url: String, headers: Map<String, String> = mapOf()): String {
        val requestBuilder = Request.Builder().url(url).get()

        if (headers.isNotEmpty()) {
            headers.forEach { key, value ->
                requestBuilder.addHeader(key, value)
            }
        }
        val request = requestBuilder.build()

        shortRunHttpClient.newCall(request).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("request failed, url:$url message: ${response.message}")
                throw CodeCCException(CommonMessageCode.THIRD_PARTY_SYSTEM_FAIL)
            }
            return responseContent
        }
    }

    fun doGet(url: String, parameters: Map<String, Any>, headers: Map<String, String>): String {
        val parameterUrl = StringBuffer()
        if (parameters.isNotEmpty()) {
            parameters.forEach { parameterUrl.append("&${it.key}=${it.value}") }

            if (!url.contains("?")) {
                parameterUrl.replace(0, 1, "?")
            }
        }
        return doGet(url + parameterUrl, headers)
    }

    /**
     * 将请求中错误信息添加到异常详情
     */
    fun doGetWithResp(url: String, headers: Map<String, String> = mapOf()): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
        headers.forEach { key, value ->
            requestBuilder.addHeader(key, value)
        }
        val request = requestBuilder.build()

        return okHttpClient.newCall(request).execute().use { response ->
            val responseContent = response.body?.string() ?: "{}"
            if (!response.isSuccessful) {
                val result = JSONObject.parseObject(responseContent, object : TypeReference<Map<String, String>>() {})
                val status = result["status"] ?: "Unknown"
                val message = result["message"] ?: "No message provided"
                logger.warn("request failed, url:$url message: ${response.message}")
                throw CodeCCException(status, message)
            }
            responseContent
        }
    }

    fun doHttp(request: Request): Response {
        return okHttpClient.newCall(request).execute()
    }

    fun doHttpPost(url: String, body: String, headers: Map<String, String> = mapOf()): String {
        return doHttpPost(url, body, headers, exitFail = true)
    }

    fun <T : Any> doHttpPost(url: String, body: String, headers: Map<String, String> = mapOf(), clz: Class<T>): T? {
        val response = doHttpPost(url, body, headers, exitFail = true)
        if (response.isEmpty()) {
            return null
        }
        return JSONObject.parseObject(response, clz)
    }

    fun <T : Any> doHttpPost(
        url: String,
        body: String,
        headers: Map<String, String> = mapOf(),
        typeReference: TypeReference<T>
    ): T? {
        val response = doHttpPost(url, body, headers, exitFail = true)
        if (response.isEmpty()) {
            return null
        }
        return JSONObject.parseObject(response, typeReference)
    }

    /**
     * 发起短时间响应的请求
     */
    fun doShortRunHttpPost(url: String, body: String, headers: Map<String, String> = mapOf()): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .post(
                RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(), body
                )
            )
        if (headers.isNotEmpty()) {
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }
        }
        val client = shortRunHttpClient.newBuilder().build()
        val request = requestBuilder.build()
        client.newCall(request).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn(
                    "request failed, url: $url requestBody: $body message: ${response.message}, content: $responseContent"
                )
                throw CodeCCException(CommonMessageCode.THIRD_PARTY_SYSTEM_FAIL)
            }
            return responseContent
        }
    }

    fun doHttpPost(url: String, body: String, headers: Map<String, String> = mapOf(), exitFail: Boolean): String {
        val requestBuilder = Request.Builder()
            .url(url).post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
        if (headers.isNotEmpty()) {
            headers.forEach { key, value ->
                requestBuilder.addHeader(key, value)
            }
        }
        val request = requestBuilder.build()
        val client = okHttpClient.newBuilder().build()
        client.newCall(request).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("request failed, url: $url requestBody: $body message: ${response.message}, content: $responseContent")
                if (exitFail) {
                    throw CodeCCException(CommonMessageCode.THIRD_PARTY_SYSTEM_FAIL)
                }
            }
            return responseContent
        }
    }

    fun doHttpDelete(url: String, body: String, headers: Map<String, String> = mapOf()): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .delete(
                RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(), body
                )
            )
        if (headers.isNotEmpty()) {
            headers.forEach { key, value ->
                requestBuilder.addHeader(key, value)
            }
        }
        val request = requestBuilder.build()
        val client = okHttpClient.newBuilder().build()
        client.newCall(request).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("request failed, message: ${response.message}")
                throw CodeCCException(CommonMessageCode.THIRD_PARTY_SYSTEM_FAIL)
            }
            return responseContent
        }
    }

    fun doHttpPut(url: String, body: String, headers: Map<String, String> = mapOf()): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .put(
                RequestBody.create(
                    "application/json; charset=utf-8".toMediaTypeOrNull(), body
                )
            )
        if (headers.isNotEmpty()) {
            headers.forEach { key, value ->
                requestBuilder.addHeader(key, value)
            }
        }
        val request = requestBuilder.build()
        val client = okHttpClient.newBuilder().build()
        client.newCall(request).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("request failed, message: ${response.message}")
                throw CodeCCException(CommonMessageCode.THIRD_PARTY_SYSTEM_FAIL)
            }
            return responseContent
        }
    }

    fun doFileStreamPut(url: String, file: File, headers: Map<String, String> = mapOf()): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .put(
                RequestBody.create(
                    "application/octet-stream; charset=utf-8".toMediaTypeOrNull(), file
                )
            )
        return doStreamPut(requestBuilder, headers)
    }

    fun doFileStreamPut(url: String, content: ByteArray, headers: Map<String, String> = mapOf()): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .put(
                RequestBody.create(
                    "application/octet-stream; charset=utf-8".toMediaTypeOrNull(), content
                )
            )
        return doStreamPut(requestBuilder, headers)
    }

    private fun doStreamPut(
        requestBuilder: Request.Builder,
        headers: Map<String, String> = mapOf()
    ): String {
        if (headers.isNotEmpty()) {
            headers.forEach { key, value ->
                requestBuilder.addHeader(key, value)
            }
        }
        val request = requestBuilder.build()
        val client = okHttpClient.newBuilder().build()
        client.newCall(request).execute().use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("request failed, message: ${response.message}")
                throw CodeCCException(CommonMessageCode.THIRD_PARTY_SYSTEM_FAIL)
            }
            return responseContent
        }
    }

    fun downloadFile(url: String, destPath: File) {
        downloadFile(url, destPath, mapOf())
    }

    // NOCC:NestedBlockDepth(设计如此:)
    fun downloadFile(url: String, destPath: File, headers: Map<String, String> = mapOf()) {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()

        if (headers.isNotEmpty()) {
            headers.forEach { key, value ->
                requestBuilder.addHeader(key, value)
            }
        }
        val request = requestBuilder.build()
        okHttpClient.newCall(request).execute().use { response ->
            if (response.code == HttpStatus.NOT_FOUND.value()) {
                logger.warn("The file $url is not exist")
                throw RuntimeException("文件不存在")
            }
            if (!response.isSuccessful) {
                logger.warn(
                    "fail to download the file from $url because of" +
                            " ${response.message} and code ${response.code}"
                )
                // NOCC:TooGenericExceptionThrown(设计如此:)
                throw RuntimeException("获取文件失败")
            }
            if (!destPath.parentFile.exists()) destPath.parentFile.mkdirs()
            // NOCC:MagicNumber(设计如此:)
            val buf = ByteArray(4096)
            response.body!!.byteStream().use { bs ->
                var len = bs.read(buf)
                FileOutputStream(destPath).use { fos ->
                    while (len != -1) {
                        fos.write(buf, 0, len)
                        len = bs.read(buf)
                    }
                }
            }
        }
    }

    fun download(url: String, headers: Map<String, String> = mapOf()): ByteArray {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
        if (headers.isNotEmpty()) {
            headers.forEach { key, value ->
                requestBuilder.addHeader(key, value)
            }
        }
        val request = requestBuilder.build()
        return okHttpClient.newCall(request).execute().use { response ->
            if (response.code == HttpStatus.NOT_FOUND.value()) {
                logger.warn("The file $url is not exist")
                throw RuntimeException("文件不存在")
            }
            if (!response.isSuccessful) {
                logger.warn(
                    "fail to download the file from $url " +
                            "because of ${response.message} and code ${response.code}"
                )
                // NOCC:TooGenericExceptionThrown(设计如此:)
                throw RuntimeException("获取文件失败")
            }
            val content = response.body!!.byteStream().use {
                IOUtils.toByteArray(it)
            }
            content
        }
    }

    fun downloadFile(response: Response, destPath: File) {
        if (response.code == HttpStatus.NOT_MODIFIED.value()) {
            logger.info("file is newest, do not download to $destPath")
            return
        }
        if (!response.isSuccessful) {
            logger.warn("fail to download the file because of ${response.message} and code ${response.code}")
            throw RuntimeException("获取文件失败")
        }
        if (!destPath.parentFile.exists()) destPath.parentFile.mkdirs()
        val buf = ByteArray(4096)
        response.body!!.byteStream().use { bs ->
            var len = bs.read(buf)
            FileOutputStream(destPath).use { fos ->
                while (len != -1) {
                    fos.write(buf, 0, len)
                    len = bs.read(buf)
                }
            }
        }
    }
}
