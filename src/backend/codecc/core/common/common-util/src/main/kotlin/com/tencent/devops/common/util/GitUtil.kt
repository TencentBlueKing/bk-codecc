package com.tencent.devops.common.util

import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

object GitUtil {

    fun urlDecode(s: String) = URLDecoder.decode(s, "UTF-8")

    fun urlEncode(s: String) = URLEncoder.encode(s, "UTF-8")

    /**
     * git拉取代码被终止会导致index被锁住，需要删除.git/index.lock文件
     */
    fun deleteLock(workspace: File) {
        val lockFile = File(workspace, ".git/index.lock")
        if (lockFile.exists()) {
            lockFile.delete()
        }
    }

    fun getProjectName(gitUrl: String): String {
        var urlFinal = gitUrl

        // 兼容处理错误的历史数据: devops-virtual-https://xxx.yyy/zzz.git
        val flag = "devops-virtual-"
        if (urlFinal.startsWith(flag)) {
            urlFinal = urlFinal.replaceFirst(flag, "")
        }

        return if (urlFinal.startsWith("http")) {
            URL(urlFinal).path.removeSuffix(".git").removePrefix("/")
        } else {
            urlFinal.split(":").last().removeSuffix(".git")
        }
    }
}
