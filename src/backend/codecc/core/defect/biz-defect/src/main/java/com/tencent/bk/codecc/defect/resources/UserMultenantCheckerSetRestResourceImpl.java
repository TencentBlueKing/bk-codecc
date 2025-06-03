package com.tencent.bk.codecc.defect.resources;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.vo.OtherCheckerSetListQueryReq;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.checkerset.CreateCheckerSetReqVO;
import com.tencent.devops.common.api.constant.CommonMessageCode;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.MultenantConstants;
import com.tencent.devops.common.util.MultenantUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 规则集接口实现类（多租户封装版）
 *
 * @date 2025/06/24
 */
@ConditionalOnProperty(name = "codecc.enableMultiTenant", havingValue = "true")
@Service
@Slf4j
public class UserMultenantCheckerSetRestResourceImpl extends UserCheckerSetRestResourceImpl {

    private String getTenantIdFromProjectId(String projectId) {
        Pair<String, String> tp = MultenantUtils.splitProjectId(projectId);
        if (tp == null) {
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID,
                    new String[]{"多租户场景下, projectId 格式错误."});
        }

        return tp.getLeft();
    }

    @Override
    public Result<Boolean> createCheckerSet(String user, String projectId,
            CreateCheckerSetReqVO createCheckerSetReqVO) {
        String tenantId = getTenantIdFromProjectId(projectId);
        createCheckerSetReqVO.setTenantId(tenantId);

        checkerSetManageBizService.createCheckerSet(user, projectId, createCheckerSetReqVO);
        return new Result<>(true);
    }

    @Override
    public Result<Page<CheckerSetVO>> getOtherCheckerSets(String projectId,
            OtherCheckerSetListQueryReq queryCheckerSetReq) {
        String tenantId = getTenantIdFromProjectId(projectId);
        int pageNum = Math.max(queryCheckerSetReq.getPageNum() - 1, 0);
        int pageSize = queryCheckerSetReq.getPageSize() <= 0 ? 100 : queryCheckerSetReq.getPageSize();
        // 用于请求返回的分页属性
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        List<CheckerSetVO> checkerSets = checkerSetQueryBizService.getOtherCheckerSets(projectId, queryCheckerSetReq);
        if (CollectionUtils.isEmpty(checkerSets)) {
            Page<CheckerSetVO> page = new PageImpl<>(Lists.newArrayList(), pageable, 0);
            return new Result<>(page);
        }

        List<CheckerSetVO> result = checkerSets.stream()
                .filter(it -> it.getTenantId() == null || it.getTenantId().equals(tenantId)
                        || it.getTenantId().equals(MultenantConstants.SYSTEM_TENANT))
                .sorted((o1, o2) -> compareCheckerSets(o1, o2, queryCheckerSetReq.getSortField(),
                        queryCheckerSetReq.getSortType()))
                .skip((long) pageNum * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());

        return new Result<>(new PageImpl<>(result, pageable, checkerSets.size()));
    }
}
