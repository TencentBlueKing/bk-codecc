package com.tencent.codecc.common.db.utils;

import com.google.common.collect.Lists;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.DefectStatType;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Collection;

import static com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom.BS_CODECC;
import static com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom.BS_PIPELINE;
import static com.tencent.devops.common.constant.ComConstants.BsTaskCreateFrom.GONGFENG_SCAN;

public class TaskCreateFromUtils {

    /**
     * 根据创建来源获取查询条件
     * 原本只有系统创建(gongfeng_scan), 用户自主创建(bs_codecc, bs_pipeline)，user是自主创建的集合
     * 现在系统创建需【兼容】细分为开源扫描(open_source_scan), 闭源扫描(closed_source_scan)
     * @param createFroms 支持输入（user, open_source_scan, closed_source_scan, gongfeng_scan, bs_codecc, bs_pipeline）
     * @return criteria
     */
    @Nullable
    public static Criteria getCriteriaByCreateFrom(Collection<String> createFroms) {
        if (CollectionUtils.isEmpty(createFroms)) {
            return null;
        }
        return new Criteria().orOperator(createFroms.stream()
                .map(TaskCreateFromUtils::buildCriteria).toArray(Criteria[]::new));
    }

    public static Criteria buildCriteria(String createFrom) {
        Criteria baseCriteria = Criteria.where("create_from");
        // 闭源
        if (DefectStatType.CLOSED_SOURCE_SCAN.value().equals(createFrom)) {
            return baseCriteria.is(GONGFENG_SCAN.value()).and("project_id")
                    .regex(ComConstants.GONGFENG_PRIVATE_PROJECT_PREFIX);
            // 开源
        } else if (DefectStatType.OPEN_SOURCE_SCAN.value().equals(createFrom)) {
            return baseCriteria.is(GONGFENG_SCAN.value()).and("project_id")
                    .regex(ComConstants.GONGFENG_PROJECT_ID_PREFIX);
            // API创建
        } else if (DefectStatType.API_SCAN.value().equals(createFrom)) {
            return baseCriteria.is(GONGFENG_SCAN.value()).and("project_id")
                    .regex(ComConstants.CUSTOMPROJ_ID_PREFIX);
            // GitHub
        } else if (DefectStatType.GITHUB_SCAN.value().equals(createFrom)) {
            return baseCriteria.is(GONGFENG_SCAN.value()).and("project_id")
                    .regex(ComConstants.GITHUB_PROJECT_PREFIX);
            // Stream
        } else if (DefectStatType.STREAM_SCAN.value().equals(createFrom)) {
            return baseCriteria.is(BS_PIPELINE.value()).and("project_id")
                    .regex(ComConstants.GONGFENG_PROJECT_ID_PREFIX);
            // 用户自主创建（排除内部工具项目）
        } else if (DefectStatType.USER.value().equals(createFrom)) {
            return baseCriteria.in(Lists.newArrayList(BS_CODECC.value(), BS_PIPELINE.value())).and("project_id")
                    .regex(ComConstants.ONLY_USER_CREATE_FROM_REGEX);
        }
        return baseCriteria.is(createFrom);
    }
}
