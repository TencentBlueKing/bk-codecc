/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.defect.mongorepository.IgnoreDefectRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.IgnoreDefectDao;
import com.tencent.bk.codecc.defect.model.ignore.IgnoreCommentDefectModel;
import com.tencent.bk.codecc.defect.model.ignore.IgnoreCommentDefectSubModel;
import com.tencent.bk.codecc.defect.service.IIgnoreDefectService;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreCommentDefectSubVO;
import com.tencent.bk.codecc.defect.vo.ignore.IgnoreCommentDefectVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 注释忽略服务实现
 * 
 * @date 2021/6/30
 * @version V1.0
 */
@Service
public class IgnoreDefectServiceImpl implements IIgnoreDefectService {
    @Autowired
    private IgnoreDefectRepository ignoreDefectRepository;

    @Autowired
    private IgnoreDefectDao ignoreDefectDao;

    private static Logger logger = LoggerFactory.getLogger(IgnoreDefectServiceImpl.class);

    @Override
    public IgnoreCommentDefectVO findIgnoreDefectInfo(Long taskId) {
        IgnoreCommentDefectModel ignoreCommentDefectModel = ignoreDefectRepository.findFirstByTaskId(taskId);
        if (null == ignoreCommentDefectModel) {
            return null;
        }
        IgnoreCommentDefectVO ignoreCommentDefectVO = new IgnoreCommentDefectVO();
        ignoreCommentDefectVO.setTaskId(ignoreCommentDefectModel.getTaskId());
        if (MapUtils.isNotEmpty(ignoreCommentDefectModel.getIgnoreDefectMap())) {
            Map<String, List<IgnoreCommentDefectSubVO>> ignoreDefectVOMap = new HashMap<>();
            ignoreCommentDefectModel.getIgnoreDefectMap().forEach((k, v) -> {
                if (CollectionUtils.isNotEmpty(v)) {
                    List<IgnoreCommentDefectSubVO> ignoreCommentDefectSubVOList =
                            v.stream().map(ignoreCommentDefectSubModel -> {
                                IgnoreCommentDefectSubVO ignoreCommentDefectSubVO = new IgnoreCommentDefectSubVO();
                                ignoreCommentDefectSubVO.setLineNum(ignoreCommentDefectSubModel.getLineNum());
                                Map<String, String> ignoreRules =
                                        ignoreCommentDefectSubModel.getIgnoreRule().entrySet().stream()
                                                .collect(Collectors.toMap(
                                                        entry -> entry.getKey().replaceAll("#DOT#", "."),
                                                        Map.Entry::getValue));
                                ignoreCommentDefectSubVO.setIgnoreRule(ignoreRules);
                                return ignoreCommentDefectSubVO;
                            }).collect(Collectors.toList());
                    ignoreDefectVOMap.put(k.replaceAll("~", "."), ignoreCommentDefectSubVOList);
                } else {
                    ignoreDefectVOMap.put(k.replaceAll("~", "."), null);
                }
            });
            ignoreCommentDefectVO.setIgnoreDefectMap(ignoreDefectVOMap);
        }
        return ignoreCommentDefectVO;
    }

    @Override
    public Boolean deleteIgnoreDefectMap(Long taskId, Set<String> currentFileSet) {
        if (CollectionUtils.isNotEmpty(currentFileSet)) {
            IgnoreCommentDefectModel ignoreCommentDefectModel = new IgnoreCommentDefectModel();
            ignoreCommentDefectModel.setTaskId(taskId);
            Map<String, List<IgnoreCommentDefectSubModel>> ignoreDefectMap = new HashMap<>();
            currentFileSet.forEach(currentFile ->
                ignoreDefectMap.put(currentFile.replaceAll("\\.", "~"), null));
            ignoreCommentDefectModel.setIgnoreDefectMap(ignoreDefectMap);
            ignoreDefectDao.upsertIgnoreDefectInfo(ignoreCommentDefectModel);
        } else {
            ignoreDefectDao.deleteIgnoreDefectMap(taskId);
        }
        return true;
    }

    @Override
    public Boolean upsertIgnoreDefectInfo(IgnoreCommentDefectModel ignoreCommentDefectModel) {
        if (MapUtils.isNotEmpty(ignoreCommentDefectModel.getIgnoreDefectMap())) {
            Map<String, List<IgnoreCommentDefectSubModel>> ignoreDefectMap = new HashMap<>();
            ignoreCommentDefectModel.getIgnoreDefectMap().forEach((k, v) -> {
                if(CollectionUtils.isNotEmpty(v)) {
                    List<IgnoreCommentDefectSubModel> ignoreCommentDefectSubModelList =
                            v.stream().map(ignoreCommentDefectSubVO -> {
                                IgnoreCommentDefectSubModel ignoreCommentDefectSubModel
                                        = new IgnoreCommentDefectSubModel();
                                ignoreCommentDefectSubModel.setLineNum(ignoreCommentDefectSubVO.getLineNum());
                                Map<String, String> ignoreRules =
                                        ignoreCommentDefectSubVO.getIgnoreRule().entrySet().stream()
                                                .collect(Collectors.toMap(
                                                        entry -> entry.getKey().replaceAll("\\.", "#DOT#"),
                                                        Map.Entry::getValue));
                                ignoreCommentDefectSubModel.setIgnoreRule(ignoreRules);
                                return ignoreCommentDefectSubModel;
                            }).collect(Collectors.toList());
                    ignoreDefectMap.put(k.replaceAll("\\.", "~"), ignoreCommentDefectSubModelList);
                } else {
                    ignoreDefectMap.put(k.replaceAll("\\.", "~"), null);
                }
            });
            ignoreCommentDefectModel.setIgnoreDefectMap(ignoreDefectMap);
        }
        ignoreDefectDao.upsertIgnoreDefectInfo(ignoreCommentDefectModel);
        return true;
    }
}
