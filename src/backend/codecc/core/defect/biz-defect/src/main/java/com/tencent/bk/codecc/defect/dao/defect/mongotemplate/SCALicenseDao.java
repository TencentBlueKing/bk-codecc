package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.SCAQueryWarningParams;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca.SCALicenseRepository;
import com.tencent.bk.codecc.defect.model.sca.SCALicenseEntity;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.sca.SCADefectGroupStatisticVO;
import com.tencent.bk.codecc.defect.vo.sca.SCADefectQueryReqVO;
import com.tencent.codecc.common.db.MongoPageHelper;
import com.tencent.devops.common.api.pojo.Page;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * SCA工具许可证持久层代码
 */
@Slf4j
@Repository
public class SCALicenseDao {

    @Autowired
    private MongoTemplate defectMongoTemplate;
    @Autowired
    private MongoPageHelper mongoPageHelper;
    @Autowired
    private SCALicenseRepository scaLicenseRepository;

    /**
     * 根据条件分页查询SCA许可证数据
     *
     * @param scaQueryWarningParams SCA许可证查询参数
     * @param pageNum               当前页码
     * @param pageSize              每页数据量
     * @param sortField             排序字段（前端传入小驼峰格式）
     * @param sortType              排序方向
     * @return 分页数据结果
     */
    public Page<SCALicenseEntity> findLicensePageByCondition(
            SCAQueryWarningParams scaQueryWarningParams,
            Integer pageNum,
            Integer pageSize,
            String sortField,
            Sort.Direction sortType
    ) {
        // 获取db查询条件
        Criteria criteria = getQueryCriteria(scaQueryWarningParams);
        Query query = Query.query(criteria);

        // 默认按照严重等级排序
        if (StringUtils.isEmpty(sortField)) {
            sortField = "severity";
        }
        if (null == sortType) {
            sortType = Sort.Direction.ASC;
        }
        // 把前端传入的小驼峰排序字段转换为小写下划线的数据库字段名（如：severityLevel -> severity_level）
        String sortFieldInDb = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, sortField);
        List<Order> sortList = Lists.newArrayList(new Sort.Order(sortType, sortFieldInDb));

        // 设置查询超时时间为60秒
        Duration timeout = Duration.ofSeconds(60L);

        // 执行分页查询
        return mongoPageHelper.pageQuery(
                query, SCALicenseEntity.class, pageSize,
                pageNum, sortList, timeout
        );
    }

    @NotNull
    private Criteria getQueryCriteria(
            SCAQueryWarningParams scaQueryWarningParams
    ) {
        Map<Long, List<String>> taskToolMap = scaQueryWarningParams.getTaskToolMap();
        // 任务id-工具过滤条件
        Criteria taskToolCriteria = null;
        if (taskToolMap.size() == 1) {
            Map.Entry<Long, List<String>> kv = taskToolMap.entrySet().stream().findFirst().get();
            taskToolCriteria = Criteria.where("task_id").is(kv.getKey()).and("tool_name").in(kv.getValue());
        } else {
            // 若任务对应的工具均是一致的
            boolean isEqual = true;
            Set<String> toolNameSet = Sets.newHashSet(taskToolMap.values().iterator().next());
            for (List<String> toolNameList : taskToolMap.values()) {
                if (toolNameList.size() != toolNameSet.size() || !toolNameSet.containsAll(toolNameList)) {
                    isEqual = false;
                    break;
                }
            }
            if (isEqual) {
                taskToolCriteria = Criteria.where("task_id").in(taskToolMap.keySet())
                        .and("tool_name").in(toolNameSet);
            }
        }
        if (taskToolCriteria == null) {
            taskToolCriteria = new Criteria();
            List<Criteria> innerOrOpList = Lists.newArrayList();
            for (Map.Entry<Long, List<String>> entry : taskToolMap.entrySet()) {
                Long taskId = entry.getKey();
                List<String> toolNameList = entry.getValue();
                innerOrOpList.add(
                        Criteria.where("task_id").is(taskId)
                                .and("tool_name").in(toolNameList)
                );
            }
            taskToolCriteria.orOperator(innerOrOpList.toArray(new Criteria[]{}));
        }

        List<Criteria> andOpList = Lists.newArrayList();
        andOpList.add(taskToolCriteria);

        // 过滤条件：状态status
        SCADefectQueryReqVO scaDefectQueryReqVO = scaQueryWarningParams.getScaDefectQueryReqVO();
        Set<String> status = scaDefectQueryReqVO.getStatus();
        if (CollectionUtils.isNotEmpty(status)) {
            Set<Integer> statusFilter = ParamUtils.convertStringSet2IntegerSet(status);
            boolean isSnapshotQuery = StringUtils.isNotEmpty(scaDefectQueryReqVO.getBuildId());
            Set<Integer> ignoreReasonTypes = scaDefectQueryReqVO.getIgnoreReasonTypes();
            Criteria statusCriteria =
                    ParamUtils.getStatusCriteria(statusFilter, isSnapshotQuery, ignoreReasonTypes);
            andOpList.add(statusCriteria);
        }

        // todo：许可证快照

        // 许可证被组件的引用状态过滤条件
        andOpList.add(Criteria.where("has_enabled_package").is(true));

        return new Criteria().andOperator(andOpList.toArray(new Criteria[0]));
    }

    /**
     * 分组查询添加筛选条件后的各状态等级告警
     *
     * @param scaQueryWarningParams
     * @return
     */
    public List<SCADefectGroupStatisticVO> statisticByStatus(
            SCAQueryWarningParams scaQueryWarningParams
    ) {
        SCADefectQueryReqVO request = scaQueryWarningParams.getScaDefectQueryReqVO();
        request.setStatus(null);
        scaQueryWarningParams.setScaDefectQueryReqVO(request);
        // 获取db查询条件
        Criteria criteria = getQueryCriteria(scaQueryWarningParams);

        MatchOperation match = Aggregation.match(criteria);
        // 以status进行分组
        GroupOperation group = Aggregation.group("task_id", "tool_name", "status")
                .last("status").as("status")
                .count().as("defectCount");
        AggregationOptions options = Aggregation.newAggregationOptions().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, group).withOptions(options);
        AggregationResults<SCADefectGroupStatisticVO> queryResult =
                defectMongoTemplate.aggregate(
                        agg,
                        "t_sca_license",
                        SCADefectGroupStatisticVO.class
                );

        return queryResult.getMappedResults();
    }

    /**
     * 分组查询添加筛选条件后的各风险等级告警
     *
     * @param scaQueryWarningParams
     * @return
     */
    public List<SCADefectGroupStatisticVO> statisticBySeverity(
            SCAQueryWarningParams scaQueryWarningParams
    ) {
        // 获取db查询条件
        Criteria criteria = getQueryCriteria(scaQueryWarningParams);

        MatchOperation match = Aggregation.match(criteria);

        // 以severity进行分组
        GroupOperation group = Aggregation.group("task_id", "tool_name", "severity")
                .last("severity").as("severity")
                .count().as("defectCount");
        AggregationOptions options = Aggregation.newAggregationOptions().allowDiskUse(true).build();
        Aggregation agg = Aggregation.newAggregation(match, group).withOptions(options);
        AggregationResults<SCADefectGroupStatisticVO> queryResult =
                defectMongoTemplate.aggregate(
                        agg,
                        "t_sca_license",
                        SCADefectGroupStatisticVO.class
                );

        return queryResult.getMappedResults();
    }

    /**
     * 批量更新告警状态的ignore位
     *
     * @param defectList
     * @param ignoreReasonType
     * @param ignoreReason
     * @param ignoreAuthor
     */
    public void batchUpdateDefectStatusIgnoreBit(
            long taskId,
            List<SCALicenseEntity> defectList,
            int ignoreReasonType,
            String ignoreReason,
            String ignoreAuthor
    ) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED,
                    SCALicenseEntity.class);
            long currTime = System.currentTimeMillis();
            defectList.forEach(defectEntity -> {
                Update update = new Update();
                update.set("status", defectEntity.getStatus());
                update.set("ignore_time", currTime);
                update.set("ignore_reason_type", ignoreReasonType);
                update.set("ignore_reason", ignoreReason);
                update.set("ignore_author", ignoreAuthor);

                Query query = new Query(
                        Criteria.where("task_id").is(taskId)
                                .and("_id").is(new ObjectId(defectEntity.getEntityId()))
                );

                ops.upsert(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 批量更新告警的处理标记
     *
     * @param taskId
     * @param defectList
     * @param markFlag
     */
    public void batchMarkDefect(long taskId, List<SCALicenseEntity> defectList, Integer markFlag) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED,
                    SCALicenseEntity.class);
            long currTime = System.currentTimeMillis();
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(
                        Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId())).and("task_id").is(taskId));
                Update update = new Update();
                update.set("mark", markFlag);
                update.set("mark_time", currTime);
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }

    public void batchUpdateDefectAuthor(long taskId, List<SCALicenseEntity> defectList, String newAuthor) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED,
                    SCALicenseEntity.class);
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(
                        Criteria.where("_id").is(new ObjectId(defectEntity.getEntityId())).and("task_id").is(taskId));
                Update update = new Update();
                update.set("author", newAuthor);
                ops.updateOne(query, update);
            });
            ops.execute();
        }
    }
}
