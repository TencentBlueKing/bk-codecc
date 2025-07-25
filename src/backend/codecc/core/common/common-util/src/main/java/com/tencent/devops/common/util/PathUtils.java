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

package com.tencent.devops.common.util;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import kotlin.Pair;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PathUtils {

    public static final int DIR = 1;
    public static final int FILE = 2;
    public static final String REGEX_GITHUB_SSH = "git@github\\.com:(.+)\\.git$";
    public static final String REGEX_GITLAB_SSH = "git@gitlab\\.com:(.+)\\.git$";
    public static final String REGEX_GIT_SSH = "git@git\\.(.+?):(.+)\\.git$";
    public static final String REGEX_GITHUB_HTTP_WITH_CERT = "https://(.+)@github(.+)\\.git$";
    public static final String REGEX_GITLAB_HTTP_WITH_CERT = "https://(.+)@gitlab(.+)\\.git$";
    public static final String REGEX_GIT_HTTP_WITH_CERT = "http://(.+)@git\\.(.+)\\.git$";
    // 没有.git后缀的github地址需要拼上.git
    public static final String REGEX_GITHUB_HTTP_WITHOUT_GIT_SUFFIX = "http.*://github(.+)(?<!\\.git)$";
    // 没有.git后缀的gitlab地址需要拼上.git
    public static final String REGEX_GITLAB_HTTP_WITHOUT_GIT_SUFFIX = "http.*://gitlab(.+)(?<!\\.git)$";
    // 没有.git后缀的git地址需要拼上.git
    public static final String REGEX_GIT_HTTP_WITHOUT_GIT_SUFFIX = "http.*://git\\.(.+)(?<!\\.git)$";
    // http的github路径需要把s加上
    public static final String REGEX_GITHUB_HTTP = "http://github(.+)\\.git$";
    // http的gitlab路径需要把s加上
    public static final String REGEX_GITLAB_HTTP = "http://gitlab(.+)\\.git$";
    // https的git路径需要把s去掉
    public static final String REGEX_GIT_HTTPS = "https://git\\.(.+)\\.git$";
    public static final String REGEX_SVN_SSH = "svn\\+ssh\\:\\/\\/\\S+@(.+)";
    public static final String REGEX_WIN_PATH_PREFIX = "^[a-zA-Z]:/(.+)";
    public static final String REGEX_WIN_PATH_PREFIX_2 = "^[a-zA-Z]:\\\\(.+)";
    public static final String REGEX_KLOCWORK_WIN_PATH_PREFIX = "^/[a-zA-Z]/(.+)";
    public static final String REGEX_GITHUB_SSH_2 = "git@github\\.com:(.+)\\.git(.+)";
    public static final String REGEX_GITLAB_SSH_2 = "git@gitlab\\.com:(.+)\\.git(.+)";
    public static final String REGEX_GIT_SSH_2 = "git@git\\.(.+?):(.+)\\.git(.+)";
    public static final String REGEX_GITHUB_HTTPS_2 = "https://(.+)@github(.+)\\.git(.+)";
    public static final String REGEX_GITLAB_HTTPS_2 = "https://(.+)@gitlab(.+)\\.git(.+)";
    public static final String REGEX_GIT_HTTP_2 = "http://(.+)@git\\.(.+)\\.git(.+)";
    public static final String REGEX_GITHUB_HTTP_2 = "http://github(.+)\\.git(.+)";
    public static final String REGEX_GITLAB_HTTP_2 = "http://gitlab(.+)\\.git(.+)";
    public static final String REGEX_GIT_HTTPS_2 = "https://git\\.(.+)\\.git(.+)";
    private static final int MIN_LENGTH = 6;
    private static Logger logger = LoggerFactory.getLogger(PathUtils.class);


    /**
     * 路径转换
     *
     * @param paths
     * @param storeArray
     * @param type
     */

    public static void convertPaths(List<String> paths, ArrayList<String> storeArray, int type) {
        if (storeArray == null || paths == null) {
            return;
        }

        String postfix = "";
        if (type == DIR) {
            postfix = ".*";
        }

        for (String path : paths) {
            int wordCount = getWordCount(path, "/");
            int startWord = 0;
            int startIndex = 0;
            if (wordCount >= MIN_LENGTH) {
                startWord = wordCount - (wordCount * 2 / 3) + 1;
                char[] words = path.toCharArray();
                startIndex = getStartIndex(startWord, words, '/');
            }
            String regexPath = ".*" + path.substring(startIndex) + postfix;
            if (regexPath.contains("(")) {
                regexPath = regexPath.replace("(", "\\(").replace(")", "\\)");
            }
            logger.debug("will add new ignore path:%s", regexPath);
            storeArray.add(regexPath);
        }
    }

    private static int getWordCount(String string, String splitChar) {
        return string.split(splitChar).length;
    }

    private static int getStartIndex(int startWord, char[] words, char splitChar) {
        if (words == null || words.length <= 0) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < words.length; i++) {
            if (words[i] == splitChar) {
                if (++count == startWord) {
                    return i;
                }
            }
        }
        return 0;
    }

    /**
     * 将文件的绝对路径转换成相对路径
     *
     * @param streamName
     * @param toolName
     * @param defectPaths
     * @return
     */
    public static Set<String> convertAbsolutePath2RelativePath(String streamName, String toolName,
            Set<String> defectPaths) {
        Set<String> relativePaths = new HashSet<String>(defectPaths.size());

        String splitRegex = streamName + "_" + toolName.toLowerCase();

        for (String absolutePath : defectPaths) {
            int i = absolutePath.lastIndexOf(splitRegex) + splitRegex.length();

            String relativePath = absolutePath.substring(i);
            relativePaths.add(relativePath);
        }

        return relativePaths;
    }

    /**
     * 将文件的绝对路径转换成相对路径
     *
     * @param streamName
     * @param toolName
     * @return
     */
    public static String convertAbsolutePath2RelativePath(String streamName, String toolName, String defectPath) {
        String splitRegex = streamName + "_" + toolName.toLowerCase();
        int i = defectPath.lastIndexOf(splitRegex) + splitRegex.length();

        String relativePath = defectPath.substring(i, defectPath.length());

        return relativePath;
    }

    /**
     * 根据工具侧上报的url和rel_path，截取url得到前面的值，然后拿最后一个/下的值
     *
     * @param url
     * @param relPath
     * @return
     */
    public static String getRelativePath(String url, String relPath) {
        if (StringUtils.isBlank(url) || StringUtils.isBlank(relPath)) {
            return "";
        }

        if (!relPath.startsWith("/")) {
            relPath = "/" + relPath;
        }

        String root;
        int index = url.lastIndexOf(relPath);
        if (index < 0 && url.contains("/")) {
            root = url.substring(url.lastIndexOf("/"));
        } else if (index < 0 && !url.contains("/")) {
            return "";
        } else {
            String tmpPath = url.substring(0, index);
            if (StringUtils.isBlank(tmpPath)) {
                // 如果tmpPath为空。则不使用tmpPath
                root = url.substring(url.lastIndexOf("/"));
            } else {
                root = tmpPath.substring(tmpPath.lastIndexOf("/"));
            }
        }
        return root + relPath;
    }

    /**
     * 获取绝对路径与相对路径
     *
     * @param sourceUrl
     * @param sourceRelPath
     * @param sourceFilePath
     * @param traceFilePath
     * @return
     */
    public static Pair<String, String> getRelativePathAndRelPath(String sourceUrl, String sourceRelPath,
            String sourceFilePath, String traceFilePath) {

        if (StringUtils.isBlank(sourceUrl) || StringUtils.isBlank(sourceRelPath)
                || StringUtils.isBlank(sourceFilePath) || StringUtils.isBlank(traceFilePath)) {
            return new Pair<>(sourceFilePath, sourceRelPath);
        }

        if (!sourceRelPath.startsWith("/")) {
            sourceRelPath = "/" + sourceRelPath;
        }

        int tmpInd;
        String relPath = "";
        if (traceFilePath.equals(sourceFilePath)) {
            relPath = sourceRelPath;
        } else {
            tmpInd = sourceFilePath.lastIndexOf(sourceRelPath);
            relPath = traceFilePath.substring(tmpInd, traceFilePath.length());
        }

        String root;
        tmpInd = sourceUrl.lastIndexOf(sourceRelPath);
        if (tmpInd < 0) {
            root = sourceUrl.substring(sourceUrl.lastIndexOf("/"));
        } else {
            String tmpPath = sourceUrl.substring(0, tmpInd);
            root = tmpPath.substring(tmpPath.lastIndexOf("/"));
        }

        return new Pair<>(root + relPath, relPath);
    }

    /**
     * 根据工具侧上报的url和rel_path，获取文件的完整url
     * 返回格式：http://github.com/xxx/website/blob/branch/xxx/xxx.java
     *
     * @param url
     * @param branch
     * @param relPath
     * @return
     */
    public static String getFileUrl(String url, String branch, String relPath) {
        return getFileUrl(url, branch, null, relPath);
    }

    /**
     * 根据工具侧上报的url和rel_path，获取文件的完整url
     * 返回格式：http://github.com/xxx/website/blob/xxx/xxx/xxx.java
     *
     * @param url
     * @param branch
     * @param revision
     * @param relPath
     * @return
     */
    public static String getFileUrl(String url, String branch, String revision, String relPath) {
        if (StringUtils.isBlank(url) || StringUtils.isBlank(branch) || StringUtils.isBlank(relPath)) {
            return "";
        }

        if (url.endsWith(".git")) {
            if (!relPath.startsWith("/")) {
                relPath = "/" + relPath;
            }

            url = url + relPath;
        }
        return formatFileRepoUrlToHttp(url).replace(".git", "/blob/" +
                (branch.startsWith("origin/") && StringUtils.isNotBlank(revision) ? revision : branch));
    }

    /**
     * 将路径转成全小写
     *
     * @param paths
     */
    public static Set<String> convertPathsToLowerCase(Set<String> paths) {
        Set<String> lowerCasePathSet = new HashSet<>(paths.size());
        paths.forEach(
                path -> lowerCasePathSet.add(path.toLowerCase())
        );
        return lowerCasePathSet;
    }

    /**
     * 检测路径是否匹配某个过滤路径
     *
     * @param path
     * @param filterPaths
     * @return
     */
    public static Pair<Boolean, String> checkIfMaskByPath(String path, Set<String> filterPaths) {
        if (StringUtils.isEmpty(path)) {
            return new Pair<>(false, null);
        }

        for (String regrex : filterPaths) {
            try {
                if (path.contains(regrex) || path.matches(regrex)) {
                    return new Pair<>(true, regrex);
                }
                //处理白名单中window会出现F:/ 与 /F 不匹配的问题
                if (regrex.matches("^[A-Z]:.*")) {
                    String subRegex = "/" + regrex.replaceFirst(":", "");
                    if (path.contains(subRegex) || path.matches(subRegex)) {
                        return new Pair<>(true, regrex);
                    }
                }
            } catch (Exception e) {
                logger.info("invalid regex expression: {}, {}", path, regrex);
            }
        }
        return new Pair<>(false, null);
    }

    /**
     * 将代码仓库地址转换成http规范地址
     *
     * @param url
     * @return
     */
    public static String formatRepoUrlToHttp(String url) {
        if (StringUtils.isNotEmpty(url)) {
            if (Pattern.matches(REGEX_GITHUB_SSH, url)) {
                url = url.replaceAll(REGEX_GITHUB_SSH, "https://github.com/$1.git");
            } else if (Pattern.matches(REGEX_GITLAB_SSH, url)) {
                url = url.replaceAll(REGEX_GITLAB_SSH, "https://gitlab.com/$1.git");
            } else if (Pattern.matches(REGEX_GIT_SSH, url)) {
                url = url.replaceAll(REGEX_GIT_SSH, "http://git\\.$1/$2.git");
            } else if (Pattern.matches(REGEX_GITHUB_HTTP_WITH_CERT, url)) {
                url = url.replaceAll(REGEX_GITHUB_HTTP_WITH_CERT, "https://github$2.git");
            } else if (Pattern.matches(REGEX_GITLAB_HTTP_WITH_CERT, url)) {
                url = url.replaceAll(REGEX_GITLAB_HTTP_WITH_CERT, "https://gitlab$2.git");
            } else if (Pattern.matches(REGEX_GIT_HTTP_WITH_CERT, url)) {
                url = url.replaceAll(REGEX_GIT_HTTP_WITH_CERT, "http://git\\.$2.git");
            } else if (Pattern.matches(REGEX_GITHUB_HTTP_WITHOUT_GIT_SUFFIX, url)) {
                url = url.replaceAll(REGEX_GITHUB_HTTP_WITHOUT_GIT_SUFFIX, "https://github$1.git");
            } else if (Pattern.matches(REGEX_GITLAB_HTTP_WITHOUT_GIT_SUFFIX, url)) {
                url = url.replaceAll(REGEX_GITLAB_HTTP_WITHOUT_GIT_SUFFIX, "https://gitlab$1.git");
            } else if (Pattern.matches(REGEX_GIT_HTTP_WITHOUT_GIT_SUFFIX, url)) {
                url = url.replaceAll(REGEX_GIT_HTTP_WITHOUT_GIT_SUFFIX, "http://git\\.$1.git");
            } else if (Pattern.matches(REGEX_GITHUB_HTTP, url)) {
                url = url.replaceAll("http://github", "https://github");
            } else if (Pattern.matches(REGEX_GITLAB_HTTP, url)) {
                url = url.replaceAll("http://gitlab", "https://gitlab");
            } else if (Pattern.matches(REGEX_GIT_HTTPS, url)) {
                url = url.replaceAll(REGEX_GIT_HTTPS, "http://git\\.$1.git");
            } else if (Pattern.matches(REGEX_SVN_SSH, url)) {
                url = url.replaceAll(REGEX_SVN_SSH, "http://$1");
            }
        }
        return url;
    }

    /**
     * 将代码文件的url地址转换成http规范地址
     *
     * @param fileUrl
     * @return
     */
    public static String formatFileRepoUrlToHttp(String fileUrl) {
        if (StringUtils.isNotEmpty(fileUrl)) {
            if (Pattern.matches(REGEX_GITHUB_SSH_2, fileUrl)) {
                fileUrl = fileUrl.replaceAll(REGEX_GITHUB_SSH_2, "https://github.com/$1.git$2");
            } else if (Pattern.matches(REGEX_GITLAB_SSH_2, fileUrl)) {
                fileUrl = fileUrl.replaceAll(REGEX_GITLAB_SSH_2, "https://gitlab.com/$1.git$2");
            } else if (Pattern.matches(REGEX_GIT_SSH_2, fileUrl)) {
                fileUrl = fileUrl.replaceAll(REGEX_GIT_SSH_2, "http://git\\.$1/$2.git$3");
            } else if (Pattern.matches(REGEX_GITHUB_HTTPS_2, fileUrl)) {
                fileUrl = fileUrl.replaceAll(REGEX_GITHUB_HTTPS_2, "https://github$2.git$3");
            } else if (Pattern.matches(REGEX_GITLAB_HTTPS_2, fileUrl)) {
                fileUrl = fileUrl.replaceAll(REGEX_GITLAB_HTTPS_2, "https://gitlab$2.git$3");
            } else if (Pattern.matches(REGEX_GIT_HTTP_2, fileUrl)) {
                fileUrl = fileUrl.replaceAll(REGEX_GIT_HTTP_2, "http://git\\.$2.git$3");
            } else if (Pattern.matches(REGEX_GITHUB_HTTP_2, fileUrl)) {
                fileUrl = fileUrl.replaceAll(REGEX_GITHUB_HTTP_2, "http://github\\.$1.git$2");
            } else if (Pattern.matches(REGEX_GITLAB_HTTP_2, fileUrl)) {
                fileUrl = fileUrl.replaceAll(REGEX_GITLAB_HTTP_2, "http://gitlab\\.$1.git$2");
            } else if (Pattern.matches(REGEX_GIT_HTTPS_2, fileUrl)) {
                fileUrl = fileUrl.replaceAll(REGEX_GIT_HTTPS_2, "http://git\\.$1.git$2");
            } else if (Pattern.matches(REGEX_SVN_SSH, fileUrl)) {
                fileUrl = fileUrl.replaceAll(REGEX_SVN_SSH, "http://$1");
            }
        }
        return fileUrl;
    }

    /**
     * 去掉windows路径的盘号和冒号
     * 比如：D:/workspace/svnauth_svr/app/exception/CodeCCException.java
     * 或者 D:\\workspace\\svnauth_svr\\app\\exception\\CodeCCException.java
     * 转换为：/workspace/svnauth_svr/app/exception/CodeCCException.java
     *
     * @param filePath
     * @return
     */
    public static String trimWinPathPrefix(String filePath) {
        if (StringUtils.isNotEmpty(filePath)) {
            if (Pattern.matches(REGEX_WIN_PATH_PREFIX, filePath)) {
                filePath = filePath.replaceAll(REGEX_WIN_PATH_PREFIX, "/$1");
            } else if (Pattern.matches(REGEX_WIN_PATH_PREFIX_2, filePath)) {
                filePath = filePath.replaceAll(REGEX_WIN_PATH_PREFIX_2, "/$1").replaceAll("\\\\", "/");
            }
        }
        return filePath;
    }

    /**
     * 去掉klocwork告警的路径的windows盘号
     * 比如：/d/workspace/svnauth_svr/app/controllers/application.java
     * 转换为：/workspace/svnauth_svr/app/controllers/application.java
     *
     * @param filePath
     * @return
     */
    public static String trimKlocworkWinPathPrefix(String filePath) {
        if (StringUtils.isNotEmpty(filePath)) {
            if (Pattern.matches(REGEX_KLOCWORK_WIN_PATH_PREFIX, filePath)) {
                filePath = filePath.replaceAll(REGEX_KLOCWORK_WIN_PATH_PREFIX, "/$1");
            }
        }
        return filePath;
    }

    public static String trimWinDifferentPath(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return filePath;
        }
        // d:/xxxx这种路径
        if (filePath.length() > 1 && filePath.charAt(1) == ':') {
            return trimWinPathPrefix(filePath);
        } else {
            return trimKlocworkWinPathPrefix(filePath);
        }
    }
}
