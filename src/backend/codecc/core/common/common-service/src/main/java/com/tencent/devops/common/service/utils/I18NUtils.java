package com.tencent.devops.common.service.utils;

import com.tencent.devops.common.service.aop.AbstractI18NResponseAspect;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

@Slf4j
public class I18NUtils {

    public static String getMessage(String resourceCode) {
        try {
            Locale locale = AbstractI18NResponseAspect.getLocale();
            ResourceBundle resourceBundle = ResourceBundle.getBundle("i18n/message", locale);

            return resourceBundle.getString(resourceCode);
        } catch (Throwable t) {
            log.error("i18n util get message error, code: {}", resourceCode, t);

            return "";
        }
    }

    public static String getMessage(String resourceCode, String[] params) {
        if (params == null) {
            params = new String[0];
        }

        String message = getMessage(resourceCode);
        if (ObjectUtils.isEmpty(message)) {
            return "";
        }

        try {
            return MessageFormat.format(message, params);
        } catch (Throwable t) {
            log.error("i18n util get message error, code: {}, params: {}", resourceCode, params, t);

            return "";
        }
    }
}
