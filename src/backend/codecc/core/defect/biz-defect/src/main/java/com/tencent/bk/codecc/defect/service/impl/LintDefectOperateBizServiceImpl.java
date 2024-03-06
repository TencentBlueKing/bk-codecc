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

import com.tencent.bk.codecc.defect.dao.defect.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.model.CodeCommentEntity;
import com.tencent.bk.codecc.defect.model.SingleCommentEntity;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.vo.SingleCommentVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.util.BeanUtils;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


/**
 * lint类告警操作服务实现
 *
 * @version V1.0
 * @date 2020/3/2
 */
@Service("LINTDefectOperateBizService")
@Slf4j
public class LintDefectOperateBizServiceImpl extends AbstractDefectOperateBizService {

    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;

    @Autowired
    private Client client;

    @Value("${codecc.public.url}")
    private String codeccGateWay;

    @Override
    public void addCodeComment(
            String defectId, String toolName, String commentId, String userName,
            SingleCommentVO singleCommentVO, String fileName, String nameCn,
            String checker, String projectId, String taskId
    ) {
        log.info("start to add code comment, defect id: {}, comment id: {}", defectId, commentId);

        if (!userName.equalsIgnoreCase(singleCommentVO.getUserName())) {
            log.info("permission denied for user name: {}", userName);
            throw new CodeCCException(CommonMessageCode.PERMISSION_DENIED, new String[]{userName}, null);
        }

        //如果comment_id为空，则表示是重新新建的评论系列
        if (StringUtils.isBlank(commentId)) {
            LintDefectV2Entity defectV2Entity = lintDefectV2Repository.findById(defectId).orElse(null);
            if (null != defectV2Entity) {
                CodeCommentEntity codeCommentEntity = new CodeCommentEntity();
                SingleCommentEntity singleCommentEntity = new SingleCommentEntity();
                BeanUtils.copyProperties(singleCommentVO, singleCommentEntity);
                singleCommentEntity.setSingleCommentId(new ObjectId().toString());
                Long currentTime = System.currentTimeMillis();
                singleCommentEntity.setCommentTime(currentTime / ComConstants.COMMON_NUM_1000L);
                codeCommentEntity.setCommentList(new ArrayList<SingleCommentEntity>() {{
                    add(singleCommentEntity);
                }});
                codeCommentEntity.setCreatedDate(currentTime);
                codeCommentEntity.setUpdatedDate(currentTime);
                codeCommentEntity.setCreatedBy(singleCommentVO.getUserName());
                codeCommentEntity.setUpdatedBy(singleCommentVO.getUserName());
                codeCommentEntity = codeCommentRepository.save(codeCommentEntity);
                defectV2Entity.setCodeComment(codeCommentEntity);
                lintDefectV2Repository.save(defectV2Entity);
            }
        }
        else {
            saveCodeComment(commentId, singleCommentVO);
        }

        // 查看评论有无 @开发者
        if (singleCommentVO.getComment().contains("@")) {
            // 发送评论给被@到的人
            codeCommentSendRtx(
                    singleCommentVO.getComment(), checker, projectId,
                    taskId, toolName, defectId, userName, nameCn, fileName
            );
        }
    }
}
