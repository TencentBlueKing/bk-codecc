package com.tencent.bk.codecc.scanschedule.utils;

import com.tencent.devops.common.api.enums.OSType;
import java.util.Objects;
import java.util.Locale;

/**
 * 环境操作公共类
 * @author jimxzcai
 */
public class EnvUtils {
    private static OSType os = null;

    /**
     * 判断系统类型
     */
    public static OSType getOS() {
        if (Objects.isNull(os)) {
            String osType = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
            if (osType.indexOf("mac") >= 0 || osType.indexOf("darwin") >= 0) {
                os = OSType.MAC_OS;
            } else {
                if (osType.indexOf("win") >= 0) {
                    os = OSType.WINDOWS;
                } else {
                    if (osType.indexOf("nux") >= 0) {
                        os = OSType.LINUX;
                    }
                }
            }
        }
        return os;
    }
}
