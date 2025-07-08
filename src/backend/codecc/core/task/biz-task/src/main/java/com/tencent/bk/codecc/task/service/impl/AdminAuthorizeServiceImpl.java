/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 Tencent. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.task.service.impl;

import com.google.common.base.Joiner;
import com.tencent.bk.codecc.task.dao.mongorepository.AdminAuthorizeInfoRepository;
import com.tencent.bk.codecc.task.model.AdminAuthorizeInfoEntity;
import com.tencent.bk.codecc.task.service.AdminAuthorizeService;
import com.tencent.bk.codecc.task.vo.AdminAuthorizeInfoVO;
import com.tencent.bk.codecc.task.vo.OpToolAdminInfoVO;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.RedisKeyConstants;
import jodd.util.StringUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理员授权接口实现
 *
 * @version V1.0
 * @date 2021/4/13
 */

@Service
public class AdminAuthorizeServiceImpl implements AdminAuthorizeService {
    private static final Logger logger = LoggerFactory.getLogger(AdminAuthorizeServiceImpl.class);

    @Autowired
    private AdminAuthorizeInfoRepository adminAuthorizeInfoRepository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    /**
     * 新增、编辑BG管理员授权信息
     *
     * @param reqVO req
     * @return boolean
     */
    @Override
    public Boolean updateAdminAuthorizeInfo(@NotNull AdminAuthorizeInfoVO reqVO) {
        List<String> userIdList = reqVO.getUserIdList();
        if (CollectionUtils.isNotEmpty(userIdList)) {
            List<AdminAuthorizeInfoEntity> adminAuthorizeInfoEntityList =
                    adminAuthorizeInfoRepository.findByUserIdIn(userIdList);

            List<AdminAuthorizeInfoEntity> adminAuthorizeInfoEntitys = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(adminAuthorizeInfoEntityList)) {
                Map<String, AdminAuthorizeInfoEntity> adminAuthorizeInfoEntityMap =
                        adminAuthorizeInfoEntityList.stream().collect(Collectors
                                .toMap(AdminAuthorizeInfoEntity::getUserId, Function.identity(), (k, v) -> v));
                for (String userId : userIdList) {
                    if (adminAuthorizeInfoEntityMap.get(userId) != null) {
                        // 说明数据库中有该user,进行更新操作
                        AdminAuthorizeInfoEntity adminAuthorizeInfoEntity = adminAuthorizeInfoEntityMap.get(userId);

                        setAdminAuthorizeInfoEntity(reqVO, adminAuthorizeInfoEntitys, adminAuthorizeInfoEntity);
                    } else {
                        // Map中不存在改userId对应的数据,说明数据库中不存在该用户-新增
                        AdminAuthorizeInfoEntity adminAuthorizeInfoEntity = new AdminAuthorizeInfoEntity();
                        adminAuthorizeInfoEntity.setUserId(userId);

                        setAdminAuthorizeInfoEntity(reqVO, adminAuthorizeInfoEntitys, adminAuthorizeInfoEntity);
                    }
                }
            } else {
                // userIdList中的用户都是数据库中没有存在的-新增
                for (String userId : userIdList) {
                    AdminAuthorizeInfoEntity adminAuthorizeInfoEntity = new AdminAuthorizeInfoEntity();
                    adminAuthorizeInfoEntity.setUserId(userId);

                    setAdminAuthorizeInfoEntity(reqVO, adminAuthorizeInfoEntitys, adminAuthorizeInfoEntity);
                }
            }
            adminAuthorizeInfoRepository.saveAll(adminAuthorizeInfoEntitys);
            batchExecute(adminAuthorizeInfoEntitys);
            return true;
        }
        return false;
    }

    /**
     * 公共方法-编辑BG管理员授权信息
     *
     * @param reqVO                    请求体
     * @param adminAuthorizeInfoEntity 需要编辑的Entity
     */
    private void setAdminAuthorizeInfoEntity(@NotNull AdminAuthorizeInfoVO reqVO,
            List<AdminAuthorizeInfoEntity> adminAuthorizeInfoEntitys,
            AdminAuthorizeInfoEntity adminAuthorizeInfoEntity) {

        adminAuthorizeInfoEntity.setBgIdList(ListUtils.emptyIfNull(reqVO.getBgIdList()));
        adminAuthorizeInfoEntity.setCreateFroms(ListUtils.emptyIfNull(reqVO.getCreateFroms()));
        adminAuthorizeInfoEntity.setRemarks(StringUtils.defaultString(reqVO.getRemarks()));
        adminAuthorizeInfoEntitys.add(adminAuthorizeInfoEntity);
    }

    /**
     * 删除BG管理员
     * @param userId 用户名id
     * @return boolean
     */
    @Override
    public Boolean delete(String userId) {
        AdminAuthorizeInfoEntity authorizeInfoEntity = adminAuthorizeInfoRepository.findFirstByUserId(userId);
        if (null != authorizeInfoEntity) {
            adminAuthorizeInfoRepository.delete(authorizeInfoEntity);
            // 同时清理缓存数据
            redisTemplate.executePipelined((RedisCallback<Long>) conn -> {
                for (ComConstants.DefectStatType defectStatType : ComConstants.DefectStatType.values()) {
                    String formatKey =
                            String.format("%s%s:%s", RedisKeyConstants.PREFIX_BG_ADMIN, defectStatType.value(), userId);
                    byte[] key;
                    try {
                        key = formatKey.getBytes(StandardCharsets.UTF_8.name());
                    } catch (UnsupportedEncodingException e) {
                        logger.error(e.getMessage(), e);
                        continue;
                    }
                    conn.del(key);
                }
                return null;
            });
            return true;
        }
        return false;
    }

    /**
     * 服务启动初始化BG管理员
     * @return boolean
     */
    @Override
    public Boolean initializationBgAdminMember() {
        List<AdminAuthorizeInfoEntity> authorizeInfoEntities = adminAuthorizeInfoRepository.findAll();
        if (CollectionUtils.isEmpty(authorizeInfoEntities)) {
            return false;
        }
        batchExecute(authorizeInfoEntities);
        return true;
    }

    /**
     * 批处理BG管理员信息
     * @param authorizeInfoEntities entities
     */
    private void batchExecute(List<AdminAuthorizeInfoEntity> authorizeInfoEntities) {
        redisTemplate.executePipelined((RedisCallback<Long>) conn -> {
            for (AdminAuthorizeInfoEntity entity : authorizeInfoEntities) {
                List<String> createFroms = entity.getCreateFroms();
                List<Integer> bgIdList = entity.getBgIdList();
                if (CollectionUtils.isEmpty(createFroms) || CollectionUtils.isEmpty(bgIdList)) {
                    continue;
                }

                String userId = entity.getUserId();
                for (ComConstants.DefectStatType defectStatType : ComConstants.DefectStatType.values()) {
                    String createFrom = defectStatType.value();
                    String formatKey = String.format("%s%s:%s", RedisKeyConstants.PREFIX_BG_ADMIN, createFrom, userId);
                    byte[] key = new byte[0];
                    try {
                        key = formatKey.getBytes(StandardCharsets.UTF_8.name());
                    } catch (UnsupportedEncodingException e) {
                        logger.error(e.getMessage(), e);
                        continue;
                    }
                    conn.del(key);
                    if (!createFroms.contains(createFrom)) {
                        continue;
                    }
                    for (Integer bgId : bgIdList) {
                        try {
                            conn.sAdd(key, bgId.toString().getBytes(StandardCharsets.UTF_8.name()));
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }
            return null;
        });
    }

}
