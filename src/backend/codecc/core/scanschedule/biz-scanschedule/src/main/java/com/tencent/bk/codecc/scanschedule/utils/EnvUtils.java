package com.tencent.bk.codecc.scanschedule.utils;

import com.tencent.devops.project.pojo.enums.OS;

import java.util.*;

public class EnvUtils {
    private static OS os = null;

    /**
     * 判断系统类型
     *
     * @return
     */
    public static OS getOS() {
        if (Objects.isNull(os)) {
            String osType = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
            if (osType.indexOf("mac") >= 0 || osType.indexOf("darwin") >= 0) {
                os = OS.MACOS;
            } else {
                if (osType.indexOf("win") >= 0) {
                    os = OS.WINDOWS;
                } else {
                    if (osType.indexOf("nux") >= 0) {
                        os = OS.LINUX;
                    }
                }
            }
        }
        return os;
    }
}
