package com.tencent.bk.codecc.defect.utils;

import com.tencent.bk.codecc.defect.service.AbstractQueryWarningBizService;
import com.tencent.devops.common.api.enums.ScmType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * scm 相关工具类
 *
 * @date 2023/07/17
 */
@Component
public class SCMUtils {
    @Value("${git.hosts:#{null}}")
    private String gitHosts;
    @Value("${git.tHosts:#{null}}")
    private String tGitHosts;
    private static final String githubHost = "github.com";
    private static final String[] HEADERS = {"https://", "http://", "ssh://"};
    private static Logger logger = LoggerFactory.getLogger(SCMUtils.class);

    /**
     * 由 url 解析 scm 类型
     *
     * @param url
     * @return
     */
    public ScmType getScmType(String url) {
        String hostName = getHostNameFromUrl(url);
        if (StringUtils.isBlank(hostName)) {
            return null;
        }

        if (hostName.contains(githubHost)) {
            return ScmType.GITHUB;
        }

        List<String> gitHostsList = null;
        if (StringUtils.isNotBlank(gitHosts)) {
            gitHostsList = Arrays.stream(gitHosts.split(",")).map(String::trim).collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(gitHostsList)) {
            for (String gitHost : gitHostsList) {
                if (hostName.contains(gitHost)) {
                    return ScmType.CODE_GIT;
                }
            }
        }

        List<String> tgitHostsList = null;
        if (StringUtils.isNotBlank(tGitHosts)) {
            tgitHostsList = Arrays.stream(tGitHosts.split(",")).map(String::trim).collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(tgitHostsList)) {
            for (String tgitHost : tgitHostsList) {
                if (hostName.contains(tgitHost)) {
                    return ScmType.CODE_TGIT;
                }
            }
        }

        return null;
    }

    /**
     * 从 url 中获取 host name
     * @param url
     * @return
     */
    private String getHostNameFromUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }

        int startInd, endInd;
        for (String header : HEADERS) {
            if (url.startsWith(header)) {
                startInd = url.indexOf("://") + 3;
                endInd = url.indexOf("/", startInd);

                return url.substring(startInd, endInd);
            }
        }

        // 处理 "[<username>@]<server>:/path/to/repos/myrepo.git" 格式的 url
        startInd = url.indexOf("@");
        endInd = url.indexOf(":");

        if (startInd == -1 || startInd > endInd) {
            startInd = 0;
        } else {
            startInd += 1;
        }

        if (endInd < 0 || startInd > endInd) {
            return null;
        }

        return url.substring(startInd, endInd);
    }
}