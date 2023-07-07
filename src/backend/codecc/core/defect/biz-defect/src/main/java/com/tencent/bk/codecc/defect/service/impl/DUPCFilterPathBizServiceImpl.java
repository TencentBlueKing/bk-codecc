package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.model.defect.DUPCDefectEntity;
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
public class DUPCFilterPathBizServiceImpl
        extends AbstractFilterPathBizService<FilterPathInputVO<DUPCDefectEntity>> {
    /**
     * 路径黑/白名单判断，黑名单只对比相对路径，白名单两个路径都对比
     *
     * @param filterPathInputVO
     * @return
     */
    @Override
    public Result<Boolean> processBiz(FilterPathInputVO<DUPCDefectEntity> filterPathInputVO) {
        DUPCDefectEntity dupcDefectEntity = filterPathInputVO.getDefectEntity();
        Set<String> filterPaths = filterPathInputVO.getAllFilterPath();
        Set<String> whitePaths = filterPathInputVO.getWhitePath();
        String filePath = dupcDefectEntity.getFilePath();
        // 绝对路径为空不做操作
        if (StringUtils.isBlank(filePath)) {
            return new Result<>(false);
        }

        Pair<Boolean, String> pair;
        if ((pair = shouldMask(dupcDefectEntity.getStatus(),
                filePath,
                filterPaths,
                whitePaths)).getFirst()) {
            dupcDefectEntity.setStatus(
                    dupcDefectEntity.getStatus() | ComConstants.TaskFileStatus.PATH_MASK.value());
            dupcDefectEntity.setMaskPath(pair.getSecond());
            dupcDefectEntity.setExcludeTime(filterPathInputVO.getExcludeTime());
            return new Result<>(true);
            // 如果已经是被路径屏蔽的，但是实质没有被路径屏蔽，则要把屏蔽状态去掉
        } else if (shouldUnmask(dupcDefectEntity.getStatus(), filePath, filterPaths, whitePaths)) {
            dupcDefectEntity.setStatus(dupcDefectEntity.getStatus() - ComConstants.DefectStatus.PATH_MASK.value());
            dupcDefectEntity.setMaskPath(null);
            return new Result<>(true);
        }
        return new Result<>(false);
    }
}
