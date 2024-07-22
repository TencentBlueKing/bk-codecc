package com.tencent.bk.codecc.defect.consumer;

import static java.util.stream.Collectors.toSet;

import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerSetTaskRelationshipRepository;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetPackageEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetTaskRelationshipEntity;
import com.tencent.bk.codecc.defect.model.common.OrgInfoEntity;
import com.tencent.bk.codecc.defect.service.AbstractCodeScoringService;
import com.tencent.bk.codecc.defect.service.CheckerSetPackageService;
import com.tencent.bk.codecc.defect.utils.ThirdPartySystemCaller;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import com.tencent.devops.common.constant.ComConstants.CheckerSetEnvType;
import com.tencent.devops.common.constant.ComConstants.CheckerSetPackageType;
import com.tencent.devops.common.constant.ComConstants.CodeLang;
import com.tencent.devops.common.service.BaseDataCacheService;
import com.tencent.devops.common.service.IConsumer;
import com.tencent.devops.common.util.TaskCreateFromUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CodeScoringConsumer implements IConsumer<CommitDefectVO> {

    @Autowired
    private Client client;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BaseDataCacheService baseDataCacheService;

    @Autowired
    private CheckerSetTaskRelationshipRepository checkerSetTaskRelationshipRepository;

    @Autowired
    private CheckerSetPackageService checkerSetPackageService;

    @Autowired
    private ThirdPartySystemCaller thirdPartySystemCaller;

    @Override
    public void consumer(CommitDefectVO commitDefectVO) {
        try {
            long taskId = commitDefectVO.getTaskId();
            String buildId = commitDefectVO.getBuildId();
            Result<TaskDetailVO> response =
                    client.get(ServiceTaskRestResource.class).getTaskInfoById(commitDefectVO.getTaskId());

            if (response.isNotOk() || response.getData() == null) {
                log.error("fail to get task info: {} {}", taskId, buildId);
                return;
            }

            TaskDetailVO taskDetailVO = response.getData();
            AbstractCodeScoringService codeScoringService =
                    applicationContext.getBean(getScoringServiceName(taskDetailVO),
                            AbstractCodeScoringService.class);
            codeScoringService.scoring(taskDetailVO, buildId, commitDefectVO.getToolName());
        } catch (Throwable e) {
            log.info("CodeScoringConsumer error, mq obj: {}", commitDefectVO, e);
        }
    }

    private String getScoringServiceName(TaskDetailVO taskDetailVO) {
        boolean isOpenScan = isOpenScan(taskDetailVO.getTaskId(), taskDetailVO.getCodeLang());
        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()
                .equalsIgnoreCase(taskDetailVO.getCreateFrom()) || isOpenScan
        ) {
            return "TStandard";
        } else {
            return "Custom";
        }
    }

    /**
     * 判断当前度量计算的环境是否符合开源扫描的场景
     * 规则集是否符合开源扫描规则集要求
     * 从缓存中根据当前项目语言获取相应的全量规则集信息与当前 Task 的规则集比对
     *
     * @param taskId
     * @param codeLang
     */
    private boolean isOpenScan(long taskId, long codeLang) {
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList =
                checkerSetTaskRelationshipRepository.findByTaskId(taskId);
        List<BaseDataVO> baseDataVOList = baseDataCacheService.getLanguageBaseDataFromCache(codeLang);
        TaskDetailVO task = thirdPartySystemCaller.geTaskInfoTaskId(taskId);
        List<CheckerSetPackageEntity> packages = checkerSetPackageService.getByTypeAndEnvTypeAndOrgInfoAndCreateFrom(
                CheckerSetPackageType.OPEN_SCAN.value(), CheckerSetEnvType.PROD.getKey(),
                task == null ? null : new OrgInfoEntity(task.getBgId(), task.getDeptId(),
                        task.getCenterId(), task.getGroupId()),
                task == null ? BsTaskCreateFrom.BS_PIPELINE : TaskCreateFromUtils.INSTANCE.getTaskRealCreateFrom(
                        task.getProjectId(), task.getCreateFrom()));
        Map<Long, List<CheckerSetPackageEntity>> langToPackagesMap =
                packages.stream().collect(Collectors.groupingBy(CheckerSetPackageEntity::getLangValue));
        // 过滤 OTHERS 的开源规则集
        Set<String> openSourceCheckerSet = baseDataVOList.stream()
                .filter(baseDataVO ->
                        !(CodeLang.OTHERS.langName().equals(baseDataVO.getLangFullKey())))
                .map(baseDataVO -> langToPackagesMap.getOrDefault(Long.valueOf(baseDataVO.getParamCode()),
                        Collections.emptyList()))
                .flatMap(langPackages ->
                        langPackages.stream().filter(checkerSetPackage ->
                                checkerSetPackage.getCheckerSetType() == null
                                        || "FULL".equals(checkerSetPackage.getCheckerSetType())
                        ).map(CheckerSetPackageEntity::getCheckerSetId)
                ).collect(toSet());
        Set<String> checkerSetIdSet = checkerSetTaskRelationshipEntityList.stream()
                .map(CheckerSetTaskRelationshipEntity::getCheckerSetId)
                .collect(toSet());
        return checkerSetIdSet.containsAll(openSourceCheckerSet);
    }
}
