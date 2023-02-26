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

import com.tencent.bk.codecc.defect.dao.mongorepository.CodeCommentRepository;
import com.tencent.bk.codecc.defect.model.CodeCommentEntity;
import com.tencent.bk.codecc.defect.model.SingleCommentEntity;
import com.tencent.bk.codecc.defect.service.IDefectOperateBizService;
import com.tencent.bk.codecc.defect.vo.SingleCommentVO;
import com.tencent.bk.codecc.task.api.UserTaskRestResource;
import com.tencent.devops.common.api.RtxNotifyVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.bson.types.ObjectId;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 告警操作服务抽象类
 *
 * @version V1.0
 * @date 2020/3/2
 */
@Slf4j
public abstract class AbstractDefectOperateBizService implements IDefectOperateBizService {
    @Autowired
    protected CodeCommentRepository codeCommentRepository;

    @Autowired
    protected AuthExPermissionApi authExPermissionApi;

    @Autowired
    private Client client;

    @Value("${codecc.gateway.host}")
    private String codeccGateWay;

    @Override
    public void updateCodeComment(String commentId, String userName, SingleCommentVO singleCommentVO) {
        log.info("start to update code comment, comment id: {}", commentId);
        Optional<CodeCommentEntity> optional = codeCommentRepository.findById(commentId);
        if (!optional.isPresent()) {
            return;
        }

        CodeCommentEntity codeCommentEntity = optional.get();
        List<SingleCommentEntity> commentEntityList = codeCommentEntity.getCommentList();

        if (CollectionUtils.isNotEmpty(commentEntityList)) {
            SingleCommentEntity singleCommentEntity = commentEntityList.stream().filter(commentEntity ->
                    commentEntity.getSingleCommentId().equalsIgnoreCase(singleCommentVO.getSingleCommentId())
            ).findFirst().orElseThrow(() -> new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID));
            if (!userName.equalsIgnoreCase(singleCommentEntity.getUserName())) {
                log.info("permission denied for user name: {}", userName);
                throw new CodeCCException(CommonMessageCode.PERMISSION_DENIED, new String[]{userName}, null);
            }
            singleCommentEntity.setComment(singleCommentVO.getComment());
            codeCommentEntity.setUpdatedDate(System.currentTimeMillis());
            codeCommentRepository.save(codeCommentEntity);
        }
    }

    @Override
    public void deleteCodeComment(String commentId, String singleCommentId, String userName) {
        Boolean isAdmin = authExPermissionApi.isAdminMember(userName);
        Optional<CodeCommentEntity> optional = codeCommentRepository.findById(commentId);
        if(!optional.isPresent()){
            return;
        }
        CodeCommentEntity codeCommentEntity = optional.get();
        List<SingleCommentEntity> commentEntityList = codeCommentEntity.getCommentList();
        if (CollectionUtils.isNotEmpty(commentEntityList)) {
            commentEntityList.removeIf(singleCommentEntity -> {
                if (singleCommentEntity.getSingleCommentId().equalsIgnoreCase(singleCommentId)) {
                    if (isAdmin || userName.equalsIgnoreCase(singleCommentEntity.getUserName())) {
                        return true;
                    } else {
                        throw new CodeCCException(CommonMessageCode.PERMISSION_DENIED, new String[]{userName}, null);
                    }
                } else {
                    return false;
                }
            });
            codeCommentRepository.save(codeCommentEntity);
        }
    }


    protected void saveCodeComment(String commentId, SingleCommentVO singleCommentVO) {
        Optional<CodeCommentEntity> optional = codeCommentRepository.findById(commentId);
        if(!optional.isPresent()){
            return;
        }
        CodeCommentEntity codeCommentEntity = optional.get();
        SingleCommentEntity singleCommentEntity = new SingleCommentEntity();
        BeanUtils.copyProperties(singleCommentVO, singleCommentEntity);
        singleCommentEntity.setSingleCommentId(new ObjectId().toString());
        singleCommentEntity.setCommentTime(System.currentTimeMillis() / ComConstants.COMMON_NUM_1000L);
        List<SingleCommentEntity> commentEntityList = codeCommentEntity.getCommentList();
        if (CollectionUtils.isNotEmpty(commentEntityList)) {
            if (commentEntityList.size() >= 10) {
                log.error("comment list larger than 10, comment id: {}", commentId);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID);
            }
            commentEntityList.add(singleCommentEntity);
        } else {
            codeCommentEntity.setCommentList(new ArrayList<SingleCommentEntity>() {{
                add(singleCommentEntity);
            }});
        }
        codeCommentRepository.save(codeCommentEntity);
    }

    /**
     * 组装评论内容,发送评论给被@到的人
     */
    public void codeCommentSendRtx(String comment, String checker, String projectId, String taskId, String toolName,
            String defectId, String userName, String nameCn, String fileName) {
        log.info("code comment send rtx, tool name: {} file name: {} name cn:{}  project id: {} task id: {}", toolName,
                fileName, nameCn, projectId, taskId);
        // 企业微信通知评论中@到的人
        // 正则表达式: 获取评论中@到的用户名
        Pattern pattern = Pattern.compile("(?<=[@])\\w+");
        Matcher matcher = pattern.matcher(comment);
        if (matcher.find()) {
            // 企业微信通知对象
            Set<String> receivers = new HashSet<>();
            receivers.add(matcher.group());

            // 发送内容
            String substance =
                    String.format("规则：%s\n查看详情：http://%s/codecc/%s/task/%s/defect/lint/%s/list?entityId=%s", checker,
                            codeccGateWay, projectId, taskId, toolName, defectId);

            String title = String.format("%s在 %s 的%s中@了你：%s", userName, nameCn, fileName, comment);

            RtxNotifyVO rtxNotifyVO = new RtxNotifyVO();
            rtxNotifyVO.setReceivers(receivers);
            rtxNotifyVO.setTitle(title);
            rtxNotifyVO.setSubstance(substance);
            log.info("rtxNotifyVO:[{}]", rtxNotifyVO);

            client.get(UserTaskRestResource.class).codeCommentSendRtx(rtxNotifyVO);
        } else {
            log.error("format mismatch!");
        }
    }
}
