package com.tencent.bk.codecc.defect.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

/**
 * 用户从TaskLog中获取仓库信息
 */
@Slf4j
public class TaskLogRepoInfoUtils {

    public static boolean hasRepoInfo(String msg){
        return StringUtils.isNotBlank(msg) && (msg.contains("代码库：") || msg.contains("repository: "));
    }


    public static String getRepoUrl(String msg) {
        if (!hasRepoInfo(msg)) {
            return null;
        }
        if (msg.contains("代码库：")) {
            return msg.substring(msg.indexOf("代码库：") + 4, msg.indexOf("，版本号：")).trim();
        } else if (msg.contains("repository: ")) {
            return msg.substring(msg.indexOf("repository: ") + 12, msg.indexOf("，version: ")).trim();
        }
        return null;
    }


    public static String getRevision(String msg) {
        if (!hasRepoInfo(msg)) {
            return null;
        }
        if (msg.contains("版本号：")) {
            return msg.substring(msg.indexOf("版本号：") + 4, msg.indexOf("，提交时间")).trim();
        } else if (msg.contains("version: ")) {
            return msg.substring(msg.indexOf("version: ") + 9, msg.indexOf("，commit time: ")).trim();
        }
        return null;
    }

    public static String getCommitTime(String msg) {
        if (!hasRepoInfo(msg)) {
            return null;
        }
        if (msg.contains("提交时间：")) {
            return msg.substring(msg.indexOf("提交时间：") + 5, msg.indexOf("，提交人")).trim();
        } else if (msg.contains("commit time: ")) {
            return msg.substring(msg.indexOf("commit time: ") + 9, msg.indexOf("，author: ")).trim();
        }
        return null;
    }

    public static String getCommitUser(String msg) {
        if (!hasRepoInfo(msg)) {
            return null;
        }
        if (msg.contains("提交人：")) {
            return msg.substring(msg.indexOf("提交人：") + 4, msg.indexOf("，分支")).trim();
        } else if (msg.contains("commit time: ")) {
            return msg.substring(msg.indexOf("author: ") + 8, msg.indexOf("，branch: ")).trim();
        }
        return null;
    }

    public static String getBranch(String msg) {
        if (!hasRepoInfo(msg)) {
            return null;
        }
        if (msg.contains("分支：")) {
            return msg.substring(msg.indexOf("分支：") + 3).trim();
        } else if (msg.contains("branch: ")) {
            return msg.substring(msg.indexOf("branch: ") + 8).trim();
        }
        return null;
    }
}
