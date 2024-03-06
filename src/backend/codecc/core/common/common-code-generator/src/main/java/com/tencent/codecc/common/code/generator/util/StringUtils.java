package com.tencent.codecc.common.code.generator.util;

import java.util.Locale;

public class StringUtils {

    public static String toUpperCaseFirstLetter(String word) {
        if (word == null || word.length() < 1) {
            return word;
        }

        return word.substring(0, 1).toUpperCase(Locale.ENGLISH) + word.substring(1);
    }
}
