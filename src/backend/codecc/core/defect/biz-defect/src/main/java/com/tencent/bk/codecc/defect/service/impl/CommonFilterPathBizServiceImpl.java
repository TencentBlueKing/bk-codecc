package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.model.defect.CommonDefectEntity;
import com.tencent.bk.codecc.defect.service.AbstractFilterPathBizService;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants;
import java.util.Set;

import kotlin.Pair;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author warmli
 */
@Service
public class CommonFilterPathBizServiceImpl
        extends AbstractFilterPathBizService<FilterPathInputVO<CommonDefectEntity>> {

    /**
     * 路径黑/白名单判断，黑名单只对比相对路径，白名单两个路径都对比
     *
     * @param filterPathInputVO
     * @return result of defect should be mask
     */
    @Override
    public Result<Boolean> processBiz(FilterPathInputVO<CommonDefectEntity> filterPathInputVO) {
        String toolName = filterPathInputVO.getToolName();
        CommonDefectEntity commonDefectEntity = filterPathInputVO.getDefectEntity();
        Set<String> filterPaths = filterPathInputVO.getAllFilterPath();
        Set<String> whitePaths = filterPathInputVO.getWhitePath();
        String filePath = filterPathInputVO.getDefectEntity().getFilePath();
        // 绝对路径为空不做操作
        if (StringUtils.isBlank(filePath)) {
            return new Result<>(false);
        }

        if (toolName.equals(ComConstants.Tool.KLOCWORK.name())
                || toolName.equals(ComConstants.Tool.PINPOINT.name())) {
            filePath = StringUtils.isEmpty(filePath) ? null : filePath.toLowerCase();
        }

        Pair<Boolean, String> pair;
        if ((pair = shouldMask(commonDefectEntity.getStatus(), filePath, filterPaths, whitePaths)).getFirst()) {
            commonDefectEntity.setStatus(
                    commonDefectEntity.getStatus() | ComConstants.DefectStatus.PATH_MASK.value());
            commonDefectEntity.setExcludeTime(filterPathInputVO.getExcludeTime());
            commonDefectEntity.setMaskPath(pair.getSecond());
            return new Result<>(true);
        }

        if (shouldUnmask(commonDefectEntity.getStatus(), filePath, filterPaths, whitePaths)) {
            commonDefectEntity.setStatus(
                    commonDefectEntity.getStatus() - ComConstants.DefectStatus.PATH_MASK.value());
            commonDefectEntity.setMaskPath(null);
            return new Result<>(true);
        }
        return new Result<>(false);
    }

    /**
     * 判断当前告警是否应该解除屏蔽状态
     *
     * @param filterPathInputVO 屏蔽入参
     */
    public boolean shouldMask(FilterPathInputVO<CommonDefectEntity> filterPathInputVO) {
        String filePath = filterPathInputVO.getDefectEntity().getFilePath();
        // 绝对路径为空不做操作
        if (StringUtils.isBlank(filePath)) {
            return false;
        }

        if (filterPathInputVO.getToolName().equals(ComConstants.Tool.KLOCWORK.name())
                || filterPathInputVO.getToolName().equals(ComConstants.Tool.PINPOINT.name())) {
            filePath = StringUtils.isEmpty(filePath) ? null : filePath.toLowerCase();
        }

        Pair<Boolean, String> pair = shouldMask(
                filterPathInputVO.getDefectEntity().getStatus(),
                filePath,
                filterPathInputVO.getAllFilterPath(),
                filterPathInputVO.getWhitePath()
        );

        if (pair.getFirst()) {
            filterPathInputVO.getDefectEntity().setMaskPath(pair.getSecond());
        }
        return pair.getFirst();
    }

    /**
     * 判断当前告警是否应该解除屏蔽状态
     *
     * @param filterPathInputVO 屏蔽路径信息实体类
     */
    public boolean shouldUnMask(FilterPathInputVO<CommonDefectEntity> filterPathInputVO) {
        String filePath = filterPathInputVO.getDefectEntity().getFilePath();
        // 绝对路径为空不做操作
        if (StringUtils.isBlank(filePath)) {
            return false;
        }

        if (filterPathInputVO.getToolName().equals(ComConstants.Tool.KLOCWORK.name())
                || filterPathInputVO.getToolName().equals(ComConstants.Tool.PINPOINT.name())) {
            filePath = StringUtils.isEmpty(filePath) ? null : filePath.toLowerCase();
        }
        boolean isUnmask = shouldUnmask(
                filterPathInputVO.getDefectEntity().getStatus(),
                filePath,
                filterPathInputVO.getAllFilterPath(),
                filterPathInputVO.getWhitePath()
        );

        if (isUnmask) {
            filterPathInputVO.getDefectEntity().setMaskPath(null);
        }

        return isUnmask;
    }
}
