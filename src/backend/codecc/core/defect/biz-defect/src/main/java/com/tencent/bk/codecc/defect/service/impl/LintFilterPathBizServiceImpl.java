package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
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
public class LintFilterPathBizServiceImpl
        extends AbstractFilterPathBizService<FilterPathInputVO<LintDefectV2Entity>> {

    /**
     * 路径黑/白名单判断，黑名单只对比相对路径，白名单两个路径都对比
     *
     * @param filterPathInputVO
     * @return
     */
    @Override
    public Result<Boolean> processBiz(FilterPathInputVO<LintDefectV2Entity> filterPathInputVO) {
        LintDefectV2Entity lintDefectV2Entity = filterPathInputVO.getDefectEntity();
        Set<String> filterPaths = filterPathInputVO.getAllFilterPath();
        Set<String> whitePaths = filterPathInputVO.getWhitePath();
        String filePath = lintDefectV2Entity.getFilePath();
        long curTime = filterPathInputVO.getExcludeTime();
        // 绝对路径为空不做操作
        if (StringUtils.isBlank(filePath)) {
            return new Result<>(false);
        }

        // 如果告警不是已经屏蔽，则在入库前检测一遍屏蔽路径
        Pair<Boolean, String> pair;
        if ((pair = shouldMask(lintDefectV2Entity.getStatus(), filePath, filterPaths, whitePaths)).getFirst()) {
            lintDefectV2Entity.setStatus(
                    lintDefectV2Entity.getStatus() | ComConstants.TaskFileStatus.PATH_MASK.value());
            lintDefectV2Entity.setExcludeTime(curTime);
            lintDefectV2Entity.setMaskPath(pair.getSecond());
            return new Result<>(true);
            // 如果已经是被路径屏蔽的，但是实质没有被路径屏蔽，则要把屏蔽状态去掉
        } else if (shouldUnmask(lintDefectV2Entity.getStatus(), filePath, filterPaths, whitePaths)) {
            lintDefectV2Entity.setStatus(
                    lintDefectV2Entity.getStatus() - ComConstants.TaskFileStatus.PATH_MASK.value());
            lintDefectV2Entity.setMaskPath(null);
            return new Result<>(true);
        }
        return new Result<>(false);
    }
}
