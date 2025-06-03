package com.tencent.bk.codecc.task.service.specialparam;

import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.task.service.ToolMetaService;
import com.tencent.bk.codecc.task.service.ToolService;
import com.tencent.devops.common.api.ToolOption;
import com.tencent.devops.common.constant.ToolConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 特殊参数工具类
 *
 * @version V4.0
 * @date 2019/3/12
 */
@Slf4j
@Service
public class SpecialParamUtil
{
    private static final String BUSINESS_TYPE = "SpecialParam";

    @Autowired
    private BizServiceFactory<ISpecialParamService> specialParamServiceBizServiceFactory;
    @Autowired
    private ToolMetaService toolMetaService;

    /**
     * 检查 oldParamJson 和 newParamJson 的所有被设置 fullScanOnChange 的字段的值是否相同, 若不是, 需要触发该工具的全量扫描
     * 关于 fullScanOnChange 的字段说明, 详见 CodeCC 工具开发规范 ToolOption 的字段说明
     */
    private boolean isSameToolParamsV2(String toolName, String oldParamJsonStr, String newParamJsonStr) {
        List<ToolOption> toolOptions = toolMetaService.findToolOptionsByToolName(toolName);
        if (CollectionUtils.isEmpty(toolOptions)) {
            return true;
        }

        // 处理出所有 fullScanOnChange=true 的字段名
        Set<String> varNames = toolOptions.stream()
                .filter(ToolOption::isFullScanOnChange)
                .map(ToolOption::getVarName)
                .collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(varNames)) {
            return true;
        }

        JSONObject oldParamJson, newParamJson;
        try {
            oldParamJson = new JSONObject(oldParamJsonStr);
            newParamJson = new JSONObject(newParamJsonStr);
        } catch (JSONException e) {
            log.error("create param json object error: {}", e.getMessage());
            return false;
        }

        // 只有当 varNames 中的所有都没有变化时, 返回 true
        return varNames.stream().allMatch(key -> {
           if (!oldParamJson.has(key) && !newParamJson.has(key)) {
               // 如果新老的 paramJson 都没有该字段, 则判断在该字段上没有变化
               return true;
           } else if ((!oldParamJson.has(key) && newParamJson.has(key))
                   || (oldParamJson.has(key) && !newParamJson.has(key))) {
               // 如果某个字段, 只有新老的 paramJson 中的一个有, 则代表在该字段上有变化
               return false;
           } else {
               // 新老 paramJson 都有的字段, 就比较 value 值是否有变化
               return StringUtils.equals(oldParamJson.getString(key), newParamJson.getString(key));
           }
        });
    }

    /**
     * 特殊参数是否相同
     *
     * @param toolName
     * @param paramJson1
     * @param paramJson2
     */
    public boolean isSameParam(String toolName, String paramJson1, String paramJson2, String devopsToolParamVersion) {
        if (ToolConstants.ToolParamsVersion.V2.getValue().equals(devopsToolParamVersion)) {
            return isSameToolParamsV2(toolName, paramJson1, paramJson2);
        }

        if (StringUtils.isEmpty(paramJson1)) {
            paramJson1 = new JSONObject().toString();
        }
        if (StringUtils.isEmpty(paramJson2)) {
            paramJson2 = new JSONObject().toString();
        }
        ISpecialParamService processor = specialParamServiceBizServiceFactory.createBizService(toolName, BUSINESS_TYPE, ISpecialParamService.class);
        return processor.isSameParam(toolName, paramJson1, paramJson2);
    }

    /**
     * 规则与参数是否相符
     *
     * @param toolName
     * @param checkerModel
     * @param paramJson
     * @return
     */
    public boolean checkerMatchParam(String toolName, CheckerDetailVO checkerModel, String paramJson)
    {
        if (StringUtils.isEmpty(paramJson))
        {
            paramJson = new JSONObject().toString();
        }
        ISpecialParamService processor = specialParamServiceBizServiceFactory.createBizService(toolName, BUSINESS_TYPE, ISpecialParamService.class);
        return processor.checkerMatchParam(toolName, checkerModel, paramJson);
    }
}
