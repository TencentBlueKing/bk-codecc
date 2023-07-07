package com.tencent.bk.codecc.defect.service;

import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.IBizService;
import com.tencent.devops.common.util.PathUtils;
import java.util.Set;

import kotlin.Pair;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 各类告警路径黑/白名单屏蔽判断逻辑
 * 使用绝对路径屏蔽
 *
 * @author warmli
 */
public abstract class AbstractFilterPathBizService<T> implements IBizService<T> {
    /**
     * 判断告警状态是否应该转换为"屏蔽"状态
     *
     * @param status 告警状态，只有状态不为"屏蔽"和"修复"时才屏蔽
     * @param filePath 告警文件绝对路径
     * @param whitePaths 路径白名单
     * @param filterPaths 路径黑名单
     */
    protected Pair<Boolean, String> shouldMask(
            int status,
            String filePath,
            Set<String> filterPaths,
            Set<String> whitePaths) {
        String maskPath = null;
        Pair<Boolean, String> bPair = PathUtils.checkIfMaskByPath(filePath, filterPaths);
        Pair<Boolean, String> wPair = PathUtils.checkIfMaskByPath(filePath, whitePaths);
        if (bPair.getFirst()) {
            maskPath = bPair.getSecond();
        }
        if (!wPair.getFirst()) {
            StringBuilder builder = new StringBuilder();
            whitePaths.forEach(builder::append);
            maskPath = builder.toString();
        }

        boolean isMask = (status & ComConstants.DefectStatus.PATH_MASK.value()) == 0
                && (status & ComConstants.DefectStatus.FIXED.value()) == 0
                && (bPair.getFirst()
                || (CollectionUtils.isNotEmpty(whitePaths)
                && !wPair.getFirst()));
        return new Pair<>(isMask, maskPath);
    }

    /**
     * 判断告警状态是否应该解除"屏蔽"状态
     *
     * @param status 告警状态，只有状态为"屏蔽"时才生效
     * @param filePath 告警文件绝对路径
     * @param whitePaths 路径白名单
     * @param filterPaths 路径黑名单
     */
    protected boolean shouldUnmask(
            int status,
            String filePath,
            Set<String> filterPaths,
            Set<String> whitePaths) {
        return (CollectionUtils.isEmpty(whitePaths)
                || PathUtils.checkIfMaskByPath(filePath, whitePaths).getFirst())
                && !PathUtils.checkIfMaskByPath(filePath, filterPaths).getFirst()
                && (status & ComConstants.DefectStatus.PATH_MASK.value()) > 0;
    }
}
