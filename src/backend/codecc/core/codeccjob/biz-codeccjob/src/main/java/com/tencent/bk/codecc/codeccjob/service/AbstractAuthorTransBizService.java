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

package com.tencent.bk.codecc.codeccjob.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.vo.common.AuthorTransferVO;
import com.tencent.devops.common.service.IBizService;
import java.util.HashSet;
import java.util.List;
import org.springframework.util.CollectionUtils;

/**
 * 告警处理人转换的抽象类
 *
 * @version V1.0
 * @date 2019/12/3
 */
public abstract class AbstractAuthorTransBizService implements IBizService<AuthorTransferVO> {

    protected String transferAuthor(
            List<AuthorTransferVO.TransferAuthorPair> transferAuthorList,
            String curAuthor
    ) {
        for (AuthorTransferVO.TransferAuthorPair transferAuthorPair : transferAuthorList) {
            String sourceAuthor = transferAuthorPair.getSourceAuthor();
            String targetAuthor = transferAuthorPair.getTargetAuthor();

            if (sourceAuthor.equals(curAuthor)) {
                return targetAuthor;
            }
        }

        return curAuthor;
    }

    protected List<String> transferAuthor(
            List<AuthorTransferVO.TransferAuthorPair> transferAuthorList,
            List<String> curAuthorList
    ) {
        if (CollectionUtils.isEmpty(transferAuthorList) || CollectionUtils.isEmpty(curAuthorList)) {
            return curAuthorList;
        }

        HashSet<String> curAuthorSet = Sets.newHashSet(curAuthorList);

        for (AuthorTransferVO.TransferAuthorPair transferAuthorPair : transferAuthorList) {
            String sourceAuthor = transferAuthorPair.getSourceAuthor();
            String targetAuthor = transferAuthorPair.getTargetAuthor();

            if (curAuthorSet.contains(sourceAuthor)) {
                return Lists.newArrayList(targetAuthor);
            }
        }

        return curAuthorList;
    }
}
