package com.tencent.devops.common.util;

/**
 * 字符串工具类
 */
public class CodeccStringUtils {
    /**
     * 判断字符串 sub 是否字符串 ms 的子序列
     */
    public static boolean subsequence(String sub, String ms) {
        int lens = sub.length();
        int lenm = ms.length();
        if (lens > lenm || lens == 0) {
            return false;
        }

        int i = 0;
        int j = 0;
        while (i < lens && j < lenm) {
            if (sub.charAt(i) == ms.charAt(j)) {
                i++;
            }
            j++;

            if (lenm - j < lens - i) {
                // ms 剩余的字符数不够匹配 sub 的剩余字符数, 则说明 sub 不是 ms 的子序列
                return false;
            }
        }

        return i == lens;
    }

    /**
     * 计算两个字符串的 levenshtein 编辑距离
     * 要求字符串长度不超过1000
     */
    public static long levenshteinDistance(String s1, String s2, long inf) {
        int n1 = s1.length();
        int n2 = s2.length();
        if (n1 > 1000 || n2 > 1000) {
            // 字符串长度超限, 则返回编辑距离无限大
            return inf;
        }

        long[][] dp = new long[n1 + 1][n2 + 1];

        for (int i = 0; i <= n1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= n2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= n1; i++) {
            for (int j = 1; j <= n2; j++) {
                long v1 = dp[i - 1][j] + 1;
                long v2 = dp[i][j - 1] + 1;
                long v3 = dp[i - 1][j - 1];
                if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                    v3 += 1;
                }
                dp[i][j] = Math.min(Math.min(v1, v2), v3);
            }
        }

        return dp[n1][n2];
    }
}
