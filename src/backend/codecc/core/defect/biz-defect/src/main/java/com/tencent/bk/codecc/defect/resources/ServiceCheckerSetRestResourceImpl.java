package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceCheckerSetRestResource;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetPackageEntity;
import com.tencent.bk.codecc.defect.service.CheckerSetPackageService;
import com.tencent.bk.codecc.defect.service.ICheckerSetManageBizService;
import com.tencent.bk.codecc.defect.service.ICheckerSetQueryBizService;
import com.tencent.bk.codecc.defect.vo.CheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.checkerset.CheckerSetPackageVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.devops.common.api.checkerset.CheckerSetRelationshipVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.util.BeanUtils;
import com.tencent.devops.common.web.RestResource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 规则包接口实现类
 *
 * @version V1.0
 * @date 2020/1/2
 */
@RestResource
public class ServiceCheckerSetRestResourceImpl implements ServiceCheckerSetRestResource {

    @Autowired
    private ICheckerSetManageBizService checkerSetManageBizService;
    @Autowired
    private ICheckerSetQueryBizService checkerSetQueryBizService;
    @Autowired
    private CheckerSetPackageService checkerSetPackageService;

    @Override
    public Result<Boolean> batchRelateTaskAndCheckerSet(String user, String projectId, Long taskId,
            List<CheckerSetVO> checkerSetList, Boolean isOpenSource) {
        return new Result<>(
                checkerSetManageBizService.batchRelateTaskAndCheckerSet(projectId, taskId, checkerSetList, user,
                        isOpenSource));
    }

    @Override
    public Result<List<CheckerSetVO>> queryCheckerSets(Set<String> checkerSetList, String projectId) {
        return new Result<>(checkerSetQueryBizService.queryCheckerSets(checkerSetList, projectId));
    }

    @Override
    public Result<List<CheckerSetVO>> getCheckerSets(Long taskId) {
        return new Result<>(checkerSetQueryBizService.getCheckerSetsByTaskId(taskId));
    }

    @Override
    public Result<Boolean> updateCheckerSetAndTaskRelation(Long taskId, Long codeLang, String user) {
        return new Result<>(checkerSetManageBizService.updateCheckerSetAndTaskRelation(taskId, codeLang, user));
    }

    @Override
    public Result<TaskBaseVO> getCheckerAndCheckerSetCount(Long taskId, String projectId) {
        return new Result<>(checkerSetQueryBizService.getCheckerAndCheckerSetCount(taskId, projectId));
    }

    @Override
    public Result<Boolean> setRelationships(String checkerSetId, String user,
            CheckerSetRelationshipVO checkerSetRelationshipVO) {
        checkerSetManageBizService.setRelationships(checkerSetId, user, checkerSetRelationshipVO);
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> batchSetRelationships(String user, CheckerSetRelationshipVO checkerSetRelationshipVO) {
        checkerSetRelationshipVO.getCheckerSetIds().forEach(checkerSetId ->
                checkerSetManageBizService.setRelationships(checkerSetId, user, checkerSetRelationshipVO)
        );

        return new Result<>(true);
    }

    @Override
    public Result<List<CheckerSetVO>> queryCheckerSetsForOpenScan(Set<CheckerSetVO> checkerSetList) {
        return new Result<>(checkerSetQueryBizService.queryCheckerSetsForOpenScan(checkerSetList));
    }

    @Override
    public Result<List<CheckerSetVO>> getCheckerSets(CheckerSetListQueryReq queryCheckerSetReq) {
        if (queryCheckerSetReq.getTaskId() != null) {
            return new Result<>(checkerSetQueryBizService.getCheckerSetsOfTask(queryCheckerSetReq));
        } else {
            return new Result<>(checkerSetQueryBizService.getCheckerSetsOfProject(queryCheckerSetReq));
        }
    }

    @Override
    public Result<List<CheckerSetVO>> getCheckerSetsForContent(List<String> checkerSetIdList) {
        return new Result<>(checkerSetQueryBizService.queryCheckerDetailForContent(checkerSetIdList));
    }

    @Override
    public Result<List<CheckerSetPackageVO>> getPackageByLangValue(Long langValue) {
        List<CheckerSetPackageEntity> packages = checkerSetPackageService.getByLangValue(langValue);
        if (CollectionUtils.isEmpty(packages)) {
            return new Result<>(Collections.emptyList());
        }
        List<CheckerSetPackageVO> packageVOs = new ArrayList<>();
        for (CheckerSetPackageEntity checkerSetPackage : packages) {
            CheckerSetPackageVO packageVO = new CheckerSetPackageVO();
            BeanUtils.copyProperties(checkerSetPackage, packageVO);
            packageVOs.add(packageVO);
        }
        return new Result<>(packageVOs);
    }
}
