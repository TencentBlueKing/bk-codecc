package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.model.defect.CCNDefectEntity;
import com.tencent.bk.codecc.defect.service.AbstractFilterPathBizService;
import com.tencent.bk.codecc.task.vo.FilterPathInputVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.ComConstants;
import java.util.Set;

import kotlin.Pair;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author liwei
 */
@Service
public class CCNFilterPathBizServiceImpl
        extends AbstractFilterPathBizService<FilterPathInputVO<CCNDefectEntity>> {

    /**
     * 路径黑/白名单判断，黑名单只对比相对路径，白名单两个路径都对比
     *
     */
    @Override
    public Result<Boolean> processBiz(FilterPathInputVO<CCNDefectEntity> filterPathInputVO) {
        CCNDefectEntity ccnDefectEntity = filterPathInputVO.getDefectEntity();
        Set<String> filterPaths = filterPathInputVO.getAllFilterPath();
        Set<String> whitePaths = filterPathInputVO.getWhitePath();
        String filePath = ccnDefectEntity.getFilePath();

        // 绝对路径为空不做操作
        if (StringUtils.isBlank(filePath)) {
            return new Result<>(false);
        }

        long curTime = filterPathInputVO.getExcludeTime();
        Pair<Boolean, String> pair;
        // 如果告警不是已经屏蔽，则在入库前检测一遍屏蔽路径
        if ((pair = shouldMask(ccnDefectEntity.getStatus(), filePath, filterPaths, whitePaths)).getFirst()) {
            ccnDefectEntity.setStatus(
                    ccnDefectEntity.getStatus() | ComConstants.DefectStatus.PATH_MASK.value());
            ccnDefectEntity.setMaskPath(pair.getSecond());
            ccnDefectEntity.setExcludeTime(curTime);
            return new Result<>(true);
        } else if (shouldUnmask(ccnDefectEntity.getStatus(), filePath, filterPaths, whitePaths)) {
            // 如果已经是被路径屏蔽的，但是实质没有被路径屏蔽，则要把屏蔽状态去掉
            ccnDefectEntity.setStatus(
                    ccnDefectEntity.getStatus() - ComConstants.DefectStatus.PATH_MASK.value());
            ccnDefectEntity.setMaskPath(null);
            return new Result<>(true);
        }
        return new Result<>(false);
    }
}
