package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.core.mongorepository.CheckerSetPackageRepository;
import com.tencent.bk.codecc.defect.dao.core.mongotemplate.CheckerSetPackageDao;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetPackageEntity;
import com.tencent.bk.codecc.defect.model.common.OrgInfoEntity;
import com.tencent.bk.codecc.defect.service.CheckerSetPackageService;
import com.tencent.bk.codecc.task.vo.checkerset.OpenSourceCheckerSetVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom;
import com.tencent.devops.common.constant.ComConstants.CheckerSetEnvType;
import com.tencent.devops.common.constant.ComConstants.CheckerSetPackageType;
import com.tencent.devops.common.service.BaseDataCacheService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CheckerSetPackageServiceImpl implements CheckerSetPackageService {

    @Autowired
    private BaseDataCacheService baseDataCacheService;

    @Autowired
    private CheckerSetPackageDao checkerSetPackageDao;

    @Autowired
    private CheckerSetPackageRepository checkerSetPackageRepository;


    @Override
    public boolean updateByLang(OpenSourceCheckerSetVO reqVO, String userName) {
        log.info("updateByLang reqVO:{}, userName:{}", reqVO, userName);
        String lang = reqVO.getLang();
        String checkerSetId = reqVO.getCheckerSetId();
        if (StringUtils.isBlank(lang) || StringUtils.isBlank(checkerSetId)) {
            log.warn("param is blank, abort update!");
            return false;
        }
        BaseDataVO baseDataVO = baseDataCacheService.getByTypeAndCode(ComConstants.KEY_CODE_LANG, lang);
        if (Objects.isNull(baseDataVO) || !StringUtils.isNumeric(baseDataVO.getParamCode())) {
            log.warn("baseDataEntity param code not found:{}", lang);
            return false;
        }
        String manageType = reqVO.getManageType();
        Set<String> toolList = reqVO.getToolList();
        Integer versionReq = reqVO.getVersion();
        Long langValue = Long.valueOf(baseDataVO.getParamCode());
        // 获取当前的规则集配置
        List<CheckerSetPackageEntity> checkerSetPackages =
                checkerSetPackageRepository.findByTypeAndLangValueAndCheckerSetId(manageType, langValue, checkerSetId);
        if (CollectionUtils.isNotEmpty(checkerSetPackages)) {
            // 存在则更新
            for (CheckerSetPackageEntity checkerSetPackage : checkerSetPackages) {
                Integer currVersion = checkerSetPackage.getVersion();
                boolean prodUpdate = checkerSetPackage.getEnvType().equals(CheckerSetEnvType.PROD.getKey())
                        && !CheckerSetEnvType.PRE_PROD.name().equals(reqVO.getVersionType());
                if (prodUpdate || !checkerSetPackage.getEnvType().equals(CheckerSetEnvType.PROD.getKey())) {
                    checkerSetPackage.setLastVersion(currVersion);
                    checkerSetPackage.setVersion(versionReq);
                }
                if (CollectionUtils.isNotEmpty(toolList)) {
                    checkerSetPackage.setToolList(toolList);
                }
                //更新组织架构可见范围
                List<OrgInfoEntity> orgBaseEntities = CollectionUtils.isNotEmpty(reqVO.getScopes())
                        ? reqVO.getScopes().stream().map(it -> new OrgInfoEntity(it.getBgId(), it.getBusinessLineId(),
                        it.getDeptId(), it.getCenterId(), it.getGroupId())).collect(Collectors.toList()) : null;
                checkerSetPackage.setScopes(orgBaseEntities);
                //更新创建来源可见范围
                List<String> taskCreateFromScopes = CollectionUtils.isNotEmpty(reqVO.getTaskCreateFromScopes())
                        ? reqVO.getTaskCreateFromScopes().stream().filter(it -> BsTaskCreateFrom.getByValue(it) != null)
                        .collect(Collectors.toList()) : null;
                checkerSetPackage.setTaskCreateFromScopes(taskCreateFromScopes);
            }
            log.info("updateByLang {} checkersetId:{} checkerSetPackages size: {}", lang, checkerSetId,
                    checkerSetPackages.size());
        } else {
            // 不存在则新增
            checkerSetPackages = createNewCheckerSetPackageForAllEnv(reqVO, baseDataVO.getParamName(), langValue);
            log.info("createByLang {} checkersetId:{}  checkerSetPackages size: {}", lang, checkerSetId,
                    checkerSetPackages.size());
        }
        checkerSetPackageRepository.saveAll(checkerSetPackages);
        return true;
    }

    @Override
    public boolean deleteByLang(OpenSourceCheckerSetVO reqVO, String userName) {
        log.info("deleteByLang reqVO:{}, userName:{}", reqVO, userName);
        String lang = reqVO.getLang();
        String checkerSetId = reqVO.getCheckerSetId();
        String manageType = reqVO.getManageType();
        if (StringUtils.isEmpty(lang) || StringUtils.isEmpty(checkerSetId) || StringUtils.isEmpty(manageType)) {
            log.warn("param is blank, abort delete!");
            return false;
        }
        BaseDataVO baseDataVO = baseDataCacheService.getByTypeAndCode(ComConstants.KEY_CODE_LANG, lang);
        if (Objects.isNull(baseDataVO) || !StringUtils.isNumeric(baseDataVO.getParamCode())) {
            log.warn("baseDataEntity param code not found:{}", lang);
            return false;
        }
        Long langValue = Long.valueOf(baseDataVO.getParamCode());
        checkerSetPackageDao.removeByTypeAndLangValueAndCheckerSetId(manageType, langValue, checkerSetId);
        return true;
    }

    @Override
    public List<CheckerSetPackageEntity> getByLangValueAndTypeAndEnvTypeAndOrgInfoAndCreateFrom(Long langValue,
            String type, String envType, OrgInfoEntity orgInfo, BsTaskCreateFrom createFrom) {
        List<CheckerSetPackageEntity> packages = checkerSetPackageRepository.findByTypeAndLangValueAndEnvType(type,
                langValue, envType);
        return CollectionUtils.isEmpty(packages) ? Collections.emptyList() : packages.stream()
                .filter(checkerSetPackage -> CollectionUtils.isEmpty(checkerSetPackage.getScopes())
                        || checkerSetPackage.getScopes().stream().anyMatch(it -> it.contains(orgInfo))
                        || CollectionUtils.isEmpty(checkerSetPackage.getTaskCreateFromScopes())
                        || checkerSetPackage.getTaskCreateFromScopes().contains(createFrom.value())
                ).collect(Collectors.toList());
    }

    @Override
    public List<CheckerSetPackageEntity> getByTypeAndEnvTypeAndOrgInfoAndCreateFrom(String type, String envType,
            OrgInfoEntity orgInfo, BsTaskCreateFrom createFrom) {
        List<CheckerSetPackageEntity> packages = checkerSetPackageRepository.findByTypeAndEnvType(type, envType);
        return CollectionUtils.isEmpty(packages) ? Collections.emptyList() : packages.stream()
                .filter(checkerSetPackage -> CollectionUtils.isEmpty(checkerSetPackage.getScopes())
                        || checkerSetPackage.getScopes().stream().anyMatch(it -> it.contains(orgInfo))
                        || CollectionUtils.isEmpty(checkerSetPackage.getTaskCreateFromScopes())
                        || checkerSetPackage.getTaskCreateFromScopes().contains(createFrom.value())
                ).collect(Collectors.toList());
    }


    @Override
    public List<CheckerSetPackageEntity> getByLangValue(Long langValue) {
        return checkerSetPackageRepository.findByLangValue(langValue);
    }

    @Override
    public Set<String> updateLangPreProdConfig(List<CheckerSetVO> checkerSetVOS) {
        Map<String, CheckerSetVO> preProdCheckerSetMap = checkerSetVOS.stream().collect(
                Collectors.toMap(CheckerSetVO::getCheckerSetId, Function.identity(), (k, v) -> v));
        Set<String> updateCheckerSetIds = new HashSet<>();
        List<CheckerSetPackageEntity> packages = checkerSetPackageRepository.findByEnvType(
                CheckerSetEnvType.PRE_PROD.getKey());
        List<CheckerSetPackageEntity> updatePackages = new ArrayList<>();
        for (CheckerSetPackageEntity checkerSetPackage : packages) {
            if (checkerSetPackage.getVersion() != ComConstants.ToolIntegratedStatus.PRE_PROD.value()
                    && preProdCheckerSetMap.containsKey(checkerSetPackage.getCheckerSetId())) {
                CheckerSetVO preProdCheckerSet = preProdCheckerSetMap.get(checkerSetPackage.getCheckerSetId());
                checkerSetPackage.setVersion(ComConstants.ToolIntegratedStatus.PRE_PROD.value());
                checkerSetPackage.setToolList(preProdCheckerSet.getToolList());
                updateCheckerSetIds.add(checkerSetPackage.getCheckerSetId());
                updatePackages.add(checkerSetPackage);
            }
        }
        log.info("start to update pre pro checker set size: {}", updatePackages.size());
        // 有更新的package需要保存
        if (CollectionUtils.isNotEmpty(updatePackages)) {
            checkerSetPackageRepository.saveAll(updatePackages);
        }
        return updateCheckerSetIds;
    }


    private List<CheckerSetPackageEntity> createNewCheckerSetPackageForAllEnv(OpenSourceCheckerSetVO reqVO,
            String langName, Long langValue) {
        String manageType = reqVO.getManageType();
        List<CheckerSetPackageEntity> allEnvCheckerSetPackages = new ArrayList<>();
        List<OrgInfoEntity> orgBaseEntities = CollectionUtils.isNotEmpty(reqVO.getScopes())
                ? reqVO.getScopes().stream().map(it -> new OrgInfoEntity(it.getBgId(), it.getBusinessLineId(),
                it.getDeptId(), it.getCenterId(), it.getGroupId())).collect(Collectors.toList()) : null;
        List<String> taskCreateFromScopes = CollectionUtils.isNotEmpty(reqVO.getTaskCreateFromScopes())
                ? reqVO.getTaskCreateFromScopes().stream().filter(it -> BsTaskCreateFrom.getByValue(it) != null)
                .collect(Collectors.toList()) : null;
        for (CheckerSetEnvType value : CheckerSetEnvType.values()) {
            allEnvCheckerSetPackages.add(new CheckerSetPackageEntity(langName, langValue,
                    StringUtils.isEmpty(manageType) ? CheckerSetPackageType.OPEN_SCAN.value() : manageType,
                    value.getKey(), reqVO.getCheckerSetId(), reqVO.getCheckerSetType(), orgBaseEntities,
                    taskCreateFromScopes, reqVO.getVersion(), reqVO.getLastVersion(), reqVO.getToolList()
            ));
        }
        return allEnvCheckerSetPackages;
    }
}
