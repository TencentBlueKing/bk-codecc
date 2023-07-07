package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetHisRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetProjectRelationshipRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerPropsEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetHisEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetProjectRelationshipEntity;
import com.tencent.bk.codecc.defect.service.ICheckerSetIntegratedBizService;
import com.tencent.bk.codecc.defect.service.IV3CheckerSetBizService;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants.ToolIntegratedStatus;
import com.tencent.devops.common.util.GsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CheckerSetIntegratedBizServiceImpl implements ICheckerSetIntegratedBizService {

    @Autowired
    private CheckerSetRepository checkerSetRepository;

    @Autowired
    private CheckerSetHisRepository checkerSetHisRepository;

    @Autowired
    private CheckerSetProjectRelationshipRepository projectRelationshipRepository;

    @Autowired
    private IV3CheckerSetBizService v3CheckerSetBizService;

    @Autowired
    private Client client;

    @Override
    public String updateToStatus(
            String toolName,
            String buildId,
            ToolIntegratedStatus fromStatus,
            ToolIntegratedStatus toStatus,
            String user,
            Set<String> checkerSetIds,
            Set<String> changeCheckerIds
    ) {
        List<CheckerSetEntity> changedFromCheckerSetList =
                init(toolName, fromStatus, toStatus, checkerSetIds, changeCheckerIds);

        List<String> changedFromCheckerSetIdList =
                changedFromCheckerSetList.stream().map(CheckerSetEntity::getCheckerSetId).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(changedFromCheckerSetList)) {
            return "no change checker set for update, do nothing...";
        }

        if (toStatus == ToolIntegratedStatus.G || toStatus == ToolIntegratedStatus.PRE_PROD) {
            List<CheckerSetEntity> toCheckerSetList =
                    checkerSetRepository.findByCheckerSetIdInAndVersion(changedFromCheckerSetIdList, toStatus.value());

            if (CollectionUtils.isEmpty(toCheckerSetList)) {
                log.info("back up the max version checker set: {}", toolName);
                List<CheckerSetEntity> prodCheckerSet =
                    getMaxVersionMap(changedFromCheckerSetIdList).values().stream().map(it -> {
                        it.setVersion(toStatus.value());
                        return it;
                    }).collect(Collectors.toList());
                backup(toolName, toStatus, buildId, prodCheckerSet);
            } else {
                log.info("back up the checker set: {}", toolName);
                backup(toolName, toStatus, buildId, toCheckerSetList);
            }

            checkerSetRepository.deleteAll(toCheckerSetList);

            List<CheckerSetEntity> newCheckerSetList =
                    getNewCheckerSetForGrayOrPreProd(changedFromCheckerSetList, user, toStatus);
            checkerSetRepository.saveAll(newCheckerSetList);

            // 把旧规则集toCheckerSetList换成新规则newCheckerSetList，需要设置强制全量及告警状态
            Map<String, CheckerSetEntity> oldCheckerSetMap = toCheckerSetList.stream()
                    .collect(Collectors.toMap(CheckerSetEntity::getCheckerSetId, Function.identity(), (k, v) -> k));
            updateTaskAfterChangeCheckerSet(newCheckerSetList, oldCheckerSetMap, user, toStatus);

            if (toStatus == ToolIntegratedStatus.PRE_PROD) {
                List<CheckerSetVO> list = newCheckerSetList.stream().map(it -> {
                    CheckerSetVO checkerSetVO = new CheckerSetVO();
                    checkerSetVO.setCheckerSetId(it.getCheckerSetId());
                    checkerSetVO.setToolList(
                        it.getCheckerProps().stream().map(CheckerPropsEntity::getToolName).collect(Collectors.toSet()));
                    return checkerSetVO;
                }).collect(Collectors.toList());
                log.info("is pre prod and update lang pre prod checker set: {}", list);
                Set<String> updateLangPreProdCheckerSetId =
                    client.get(ServiceBaseDataResource.class).updateLangPreProdConfig(list).getData();
                log.info("finish update lang pre prod checker set: {}", updateLangPreProdCheckerSetId);
            }

            return String.format("batch update checker set successfully: %s, %s, %s",
                toolName, toStatus, newCheckerSetList);
        } else if (toStatus == ToolIntegratedStatus.P) {
            Map<String, CheckerSetEntity> checkerSetMap = getMaxVersionMap(changedFromCheckerSetIdList);

            backup(toolName, toStatus, buildId, checkerSetMap.values());

            List<CheckerSetEntity> newCheckerSetList
                    = getNewCheckerSetForProd(changedFromCheckerSetList, checkerSetMap, user);

            // 把旧规则集checkerSetMap换成新规则newCheckerSetList，需要设置强制全量及告警状态
            List<CheckerSetEntity> updateCheckerSetList = updateTaskAfterChangeCheckerSet(newCheckerSetList,
                checkerSetMap, user, toStatus);
            if (CollectionUtils.isNotEmpty(updateCheckerSetList)) {
                checkerSetRepository.saveAll(updateCheckerSetList);
            }

            return String.format("batch update checker set successfully: %s, %s, %s",
                toolName, toStatus, updateCheckerSetList);
        }

        return "";
    }

    private List<CheckerSetEntity> updateTaskAfterChangeCheckerSet(List<CheckerSetEntity> newCheckerSetList,
                                                                   Map<String, CheckerSetEntity> oldCheckerSetMap,
                                                                   String user, ToolIntegratedStatus toStatus) {

        log.info("====================================newCheckerSetList======================================\n{}",
                GsonUtils.toJson(newCheckerSetList));
        log.info("====================================oldCheckerSetMap======================================\n{}",
                GsonUtils.toJson(oldCheckerSetMap));

        List<CheckerSetEntity> updateCheckerSetList = new ArrayList<>();
        newCheckerSetList.forEach(newCheckerSet -> {
            String checkerSetId = newCheckerSet.getCheckerSetId();
            CheckerSetEntity oldCheckerSet = oldCheckerSetMap.get(checkerSetId);
            if (diffWithOld(oldCheckerSet, newCheckerSet)) {
                log.info("start to save checker set and update project");

                updateCheckerSetList.add(newCheckerSet);
                // 查询已关联此规则集，且选择了latest版本自动更新的项目数据
                if (oldCheckerSet != null) {
                    if (toStatus == ToolIntegratedStatus.PRE_PROD) {
                        v3CheckerSetBizService.updateTaskAfterChangeCheckerSet(newCheckerSet, oldCheckerSet,
                            ToolIntegratedStatus.PRE_PROD.value(), user);
                    } else {
                        List<CheckerSetProjectRelationshipEntity> projectRelationships = projectRelationshipRepository
                            .findByCheckerSetIdAndUselatestVersion(oldCheckerSet.getCheckerSetId(), true);
                        v3CheckerSetBizService.updateTaskAfterChangeCheckerSet(newCheckerSet, oldCheckerSet,
                            projectRelationships, user);
                    }
                }
            }
        });
        return updateCheckerSetList;
    }

    private boolean diffWithOld(CheckerSetEntity oldCheckerSet, CheckerSetEntity newCheckerSet) {
        // 比较规则
        if (oldCheckerSet == null) {
            return true;
        }
        Set<String> oldCheckerSetIds =
            oldCheckerSet.getCheckerProps().stream().map(CheckerPropsEntity::getCheckerKey).collect(Collectors.toSet());
        Set<String> newCheckerSetIds =
            newCheckerSet.getCheckerProps().stream().map(CheckerPropsEntity::getCheckerKey).collect(Collectors.toSet());

        if (!oldCheckerSetIds.equals(newCheckerSetIds)) {
            Set<String> diffSet = new HashSet<>(newCheckerSetIds);
            diffSet.removeAll(oldCheckerSetIds);
            log.info("diff checker set is : {}", diffSet);
            return true;
        }

        // 比较规则属性
        Map<String, CheckerPropsEntity> oldCheckerPropsMap =
            oldCheckerSet.getCheckerProps().stream().collect(Collectors.toMap(CheckerPropsEntity::getCheckerKey,
                Function.identity(), (k, v) -> v));
        Map<String, CheckerPropsEntity> newCheckerPropsMap =
            newCheckerSet.getCheckerProps().stream().collect(Collectors.toMap(CheckerPropsEntity::getCheckerKey,
                Function.identity(), (k, v) -> v));

        if (!oldCheckerPropsMap.equals(newCheckerPropsMap)) {
            log.info("diff with props of checker...");
            return true;
        }

        log.info("the new and old checker set is same");

        return false;
    }

    private List<CheckerSetEntity> getNewCheckerSetForGrayOrPreProd(
        List<CheckerSetEntity> changedFromCheckerSetList,
        String user,
        ToolIntegratedStatus toStatus) {
        log.info("get gray data and change to gray status");

        return changedFromCheckerSetList.stream().map(testCheckerSet -> {
            testCheckerSet.setVersion(toStatus.value());
            testCheckerSet.setEntityId(null);
            testCheckerSet.setLastUpdateTime(System.currentTimeMillis());
            testCheckerSet.setUpdatedBy(user);
            return testCheckerSet;
        }).collect(Collectors.toList());
    }

    private List<CheckerSetEntity> getNewCheckerSetForProd(List<CheckerSetEntity> changedFromCheckerSetList,
                                                           Map<String, CheckerSetEntity> checkerSetMap,
                                                           String user) {
        log.info("get gray data and change to prod status");

        return changedFromCheckerSetList.stream().map(it -> {
            CheckerSetEntity maxVersionCheckerSet = checkerSetMap.get(it.getCheckerSetId());
            if (maxVersionCheckerSet == null) {
                it.setVersion(1);
            } else {
                it.setEntityId(null);
                it.setVersion(maxVersionCheckerSet.getVersion() + 1);
            }
            it.setUpdatedBy(user);
            it.setLastUpdateTime(System.currentTimeMillis());
            it.setToolName(null);
            return it;
        }).collect(Collectors.toList());
    }


    private void backup(String toolName,
                        ToolIntegratedStatus toStatus,
                        String buildId,
                        Collection<CheckerSetEntity> toCheckerSetList) {
        List<CheckerSetHisEntity> hisCheckerSetEntities;
        hisCheckerSetEntities =
            checkerSetHisRepository.findByToolNameInAndVersion(toolName, toStatus.value());

        if (CollectionUtils.isNotEmpty(hisCheckerSetEntities)) {
            if (hisCheckerSetEntities.get(0).getBuildId().equals(buildId)) {
                log.info("is the same back up build id, do nothing: {}", toolName);
                return;
            }
        }

        log.info("back up checker set: {}, {}, {}, {}", toolName, toStatus, buildId, toCheckerSetList);

        List<CheckerSetHisEntity> bakCheckerSetList = toCheckerSetList.stream().map(it -> {
            CheckerSetHisEntity checkerSetHisEntity = new CheckerSetHisEntity();
            BeanUtils.copyProperties(it, checkerSetHisEntity);
            checkerSetHisEntity.setToolName(toolName);
            checkerSetHisEntity.setBuildId(buildId);

            return checkerSetHisEntity;
        }).collect(Collectors.toList());

        checkerSetHisRepository.deleteByToolNameAndVersion(toolName, toStatus.value());
        checkerSetHisRepository.saveAll(bakCheckerSetList);
    }

    private List<CheckerSetEntity> init(
            String toolName,
            ToolIntegratedStatus fromStatus,
            ToolIntegratedStatus toStatus,
            Set<String> checkerSetIds,
            Set<String> changeCheckerIds
    ) {
        if (toStatus == ToolIntegratedStatus.T) {
            log.info("do nothing for checker set status init: {}", toolName);
            return new ArrayList<>();
        }

        log.info("get from data and change to status");
        List<CheckerSetEntity> fromCheckerSetList =
                checkerSetRepository.findByCheckerSetIdInAndVersion(checkerSetIds, fromStatus.value());

        return fromCheckerSetList;
    }

    @Override
    public String revertStatus(String toolName, ToolIntegratedStatus status, String user, Set<String> checkerSetIds) {
        if (status == ToolIntegratedStatus.T) {
            return "do nothing";
        }

        log.info("get backup checker set: {}, {}", checkerSetIds, status);
        List<CheckerSetEntity> bakCheckerSetList = checkerSetHisRepository.findByToolNameInAndVersion(
            toolName, status.value()).stream().map(it -> {
            CheckerSetEntity checkerSetEntity = new CheckerSetEntity();
            BeanUtils.copyProperties(it, checkerSetEntity);
            return checkerSetEntity;
        }).collect(Collectors.toList());

        if (status == ToolIntegratedStatus.G || status == ToolIntegratedStatus.PRE_PROD) {
            List<CheckerSetEntity> bakGrayCheckerSetList = bakCheckerSetList.stream().map(it -> {
                CheckerSetEntity checkerSetEntity = new CheckerSetEntity();
                BeanUtils.copyProperties(it, checkerSetEntity);
                checkerSetEntity.setLastUpdateTime(System.currentTimeMillis());
                checkerSetEntity.setUpdatedBy(user);
                return checkerSetEntity;
            }).collect(Collectors.toList());

            List<CheckerSetEntity> oldCheckerSetList =
                    checkerSetRepository.findByCheckerSetIdInAndVersion(checkerSetIds, status.value());
            Map<String, CheckerSetEntity> oldCheckerSetMap = oldCheckerSetList.stream()
                    .collect(Collectors.toMap(CheckerSetEntity::getCheckerSetId, Function.identity(), (k, v) -> k));
            bakGrayCheckerSetList.forEach(it -> {
                CheckerSetEntity oldCheckerSet = oldCheckerSetMap.get(it.getCheckerSetId());
                if (oldCheckerSet != null) {
                    it.setEntityId(oldCheckerSet.getEntityId());
                }
            });
            checkerSetRepository.saveAll(bakGrayCheckerSetList);

            // 把旧规则集换成新规则，需要设置强制全量及告警状态
            updateTaskAfterChangeCheckerSet(bakGrayCheckerSetList, oldCheckerSetMap, user, status);
        } else if (status == ToolIntegratedStatus.P) {
            // use bak checker to cover the prod
            List<CheckerSetEntity> newProdCheckerSetList = new ArrayList<>();
            List<String> bakCheckerSetIdList =
                    bakCheckerSetList.stream().map(CheckerSetEntity::getCheckerSetId).collect(Collectors.toList());
            Map<String, CheckerSetEntity> maxVersionMap = getMaxVersionMap(bakCheckerSetIdList);
            bakCheckerSetList.forEach(checkerSetEntity -> {
                CheckerSetEntity latestCheckerSet = maxVersionMap.get(checkerSetEntity.getCheckerSetId());

                if (latestCheckerSet != null) {
                    CheckerSetEntity newCheckerSetEntity = new CheckerSetEntity();
                    BeanUtils.copyProperties(checkerSetEntity, newCheckerSetEntity);
                    newCheckerSetEntity.setVersion(latestCheckerSet.getVersion());
                    newCheckerSetEntity.setEntityId(latestCheckerSet.getEntityId());
                    newCheckerSetEntity.setLastUpdateTime(System.currentTimeMillis());
                    newCheckerSetEntity.setUpdatedBy(user);
                    newProdCheckerSetList.add(newCheckerSetEntity);
                }
            });

            checkerSetRepository.saveAll(newProdCheckerSetList);

            // 把旧规则集换成新规则，需要设置强制全量及告警状态
            updateTaskAfterChangeCheckerSet(newProdCheckerSetList, maxVersionMap, user, status);
        }

        return String.format("batch revert checker set successfully: %s, %s, %s", toolName, status, checkerSetIds);
    }

    private Map<String, CheckerSetEntity> getMaxVersionMap(List<String> checkerSetIdList) {
        Map<String, CheckerSetEntity> checkerSetMap = new HashMap<>(8);
        // get the max checker set version
        checkerSetRepository.findByCheckerSetIdIn(checkerSetIdList).forEach(it -> {
            if (it.getVersion() > 0) {
                CheckerSetEntity checkerSetEntity = checkerSetMap.get(it.getCheckerSetId());
                if (checkerSetEntity == null || checkerSetEntity.getVersion() < it.getVersion()) {
                    checkerSetMap.put(it.getCheckerSetId(), it);
                }
            }
        });

        return checkerSetMap;
    }
}
