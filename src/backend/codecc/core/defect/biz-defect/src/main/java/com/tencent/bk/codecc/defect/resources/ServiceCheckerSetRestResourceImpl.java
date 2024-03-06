package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.ServiceCheckerSetRestResource;
import com.tencent.bk.codecc.defect.service.ICheckerSetManageBizService;
import com.tencent.bk.codecc.defect.service.ICheckerSetQueryBizService;
import com.tencent.bk.codecc.defect.vo.CheckerSetListQueryReq;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.devops.common.api.checkerset.CheckerSetRelationshipVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.web.RestResource;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

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
}
