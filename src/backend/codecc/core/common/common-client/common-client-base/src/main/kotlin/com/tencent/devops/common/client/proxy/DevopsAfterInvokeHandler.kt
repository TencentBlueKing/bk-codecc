package com.tencent.devops.common.client.proxy

import java.lang.reflect.Method

interface DevopsAfterInvokeHandler {

    fun handleAfterInvoke(method: Method, args: Array<out Any>, result : Any?)
}