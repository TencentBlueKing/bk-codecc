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
        return new Criteria().orOperator(createFroms.stream().map(TaskCreateFromUtils::buildCriteria).toArray(Criteria[]::new));
    }

    public static Criteria buildCriteria(String createFrom) {
        Criteria baseCriteria = Criteria.where("create_from");
        if (DefectStatType.CLOSED_SOURCE_SCAN.value().equals(createFrom)) {
            return baseCriteria.is(GONGFENG_SCAN.value()).and("project_id")
                    .regex(ComConstants.GONGFENG_PRIVATYE_PROJECT_PREFIX);
        } else if (DefectStatType.OPEN_SOURCE_SCAN.value().equals(createFrom)) {
            return baseCriteria.is(GONGFENG_SCAN.value()).and("project_id")
                    .regex(ComConstants.GONGFENG_PROJECT_ID_PREFIX);
        } else if (DefectStatType.USER.value().equals(createFrom)) {
            return baseCriteria.in(Lists.newArrayList(BS_CODECC.value(), BS_PIPELINE.value()));
        }
        return baseCriteria.is(createFrom);
    }
}
