package com.tencent.bk.codecc.defect.bkpass;

import com.google.common.collect.ImmutableMap;
import com.tencent.bk.codecc.defect.vo.ToolMemberInfoVO;
import com.tencent.devops.common.codecc.util.JsonUtil;
import com.tencent.devops.common.util.OkhttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BkPassClientApi {

    private static final Logger logger = LoggerFactory.getLogger(BkPassClientApi.class);

    @Value("${bkpass.codecc.appcode:#{null}}")
    private String appCode;

    @Value("${bkpass.codecc.appsecret:#{null}}")
    private String appSecret;

    @Value("${bkpass.codecc.appPdId:#{null}}")
    private String appPdId;

    @Value("${bkpass.rootpath:#{null}}")
    private String rootPath;

    /**
     * 同步开发者中心工具成员
     * @param pluginId 插件ID
     * @param toolMemberInfoList 成员列表
     */
    public void syncToolMember(String pluginId, List<ToolMemberInfoVO> toolMemberInfoList) {

        String url = String.format("%s/sys/shim/plugins_center/bk_plugins/%s/plugins/%s/members/",
                rootPath, appPdId, pluginId);

        Map<String, String> authParams = ImmutableMap.of(
                "bk_app_code", appCode,
                "bk_app_secret", appSecret
        );
        // 紧凑JSON格式，不包含换行符和多余空格
        String authHeaderValue = JsonUtil.INSTANCE.toJson(authParams).replaceAll("\\s+", "");

        Map<String, String> headers = ImmutableMap.of(
                "Content-Type", "application/json",
                "X-Bkapi-Authorization", authHeaderValue
        );

        String body = JsonUtil.INSTANCE.toJson(toolMemberInfoList);

        logger.info("#bkPassClientApi syncToolMember doHttp - url: {}", url);

        try {
            OkhttpUtils.INSTANCE.doHttpPost(url, body, headers);
            logger.info("#bkPassClientApi syncToolMember request success!");
        } catch (Exception e) {
            logger.error("#bkPassClientApi syncToolMember request fail. error message: {}[{}]"
                    , e.getMessage(), e);
        }
    }

}
