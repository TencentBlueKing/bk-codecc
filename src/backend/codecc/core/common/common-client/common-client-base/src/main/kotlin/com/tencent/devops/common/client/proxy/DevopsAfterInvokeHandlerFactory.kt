package com.tencent.devops.common.client.proxy

import com.tencent.devops.common.service.utils.SpringContextUtil
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class DevopsAfterInvokeHandlerFactory : CommandLineRunner {

    companion object{
        var SINGLETON : DevopsAfterInvokeHandlerFactory? = null
        private val logger = LoggerFactory.getLogger(DevopsAfterInvokeHandlerFactory::class.java)
    }

    private var handles : List<DevopsAfterInvokeHandler>? = null

    fun getInvokeHandlers(): List<DevopsAfterInvokeHandler> {
        return handles ?: emptyList()
    }

    override fun run(vararg args: String?) {
        logger.info("Init DevopsAfterInvokeHandlerFactory Start")
        SINGLETON = this
        //获取 handles
        val handlerBeanMap = SpringContextUtil.getBeansOfType(DevopsAfterInvokeHandler::class.java)
        handles = if (handlerBeanMap.isNullOrEmpty()) {
            emptyList()
        } else {
            handlerBeanMap.values.toList()
        }
        logger.info("Init DevopsAfterInvokeHandlerFactory End handles:${handles!!.size}")
    }

}