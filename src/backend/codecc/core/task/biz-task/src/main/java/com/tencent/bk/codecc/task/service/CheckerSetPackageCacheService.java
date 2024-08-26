package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.defect.vo.checkerset.CheckerSetPackageVO;
import com.tencent.devops.common.api.OrgInfoVO;
import java.util.List;
import java.util.Map;

/**
 * CheckerSetPackage缓存类
 */
public interface CheckerSetPackageCacheService {

    /**
     * 根据langValue获取CheckerSetPackage
     *
     * @param langValue
     * @return
     */
    List<CheckerSetPackageVO> getPackageByLangValueFromCache(Long langValue);

    /**
     * 根据多个langValue获取CheckerSetPackage
     *
     * @param langValues
     * @return
     */
    Map<Long, List<CheckerSetPackageVO>> getPackageByLangValueFromCache(List<Long> langValues);

    /**
     * 根据langValue获取CheckerSetPackage
     *
     * @param langValue
     * @param type
     * @return
     */
    List<CheckerSetPackageVO> getPackageByLangValueAndTypeFromCache(Long langValue, String type);

    /**
     * 根据多个langValue与type获取CheckerSetPackage
     *
     * @param langValues
     * @param type
     * @return
     */
    Map<Long, List<CheckerSetPackageVO>> getPackageByLangValueAndTypeFromCache(List<Long> langValues, String type);

    /**
     * 根据type获取CheckerSetPackage
     *
     * @param type
     * @return
     */
    List<CheckerSetPackageVO> getPackageByTypeFromCache(String type);


    /**
     * 根据langValue与其他信息获取CheckerSetPackage，过滤组织
     *
     * @param langValue
     * @param type
     * @param envType
     * @param orgInfo
     * @return
     */
    List<CheckerSetPackageVO> getPackageByLangValueAndTypeAndEnvTypeAndOrgInfoFromCache(Long langValue,
            String type, String envType, OrgInfoVO orgInfo);

    /**
     * 根据langValue与其他信息获取CheckerSetPackage
     *
     * @param langValue
     * @param type
     * @param envType
     * @return
     */
    List<CheckerSetPackageVO> getPackageByLangValueAndTypeAndEnvTypeFromCache(Long langValue,
            String type, String envType);

}
