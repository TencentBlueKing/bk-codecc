package com.tencent.bk.codecc.defect.dao.defect.mongotemplate;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.SCAQueryWarningParams;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.sca.SCASbomPackageRepository;
import com.tencent.bk.codecc.defect.model.sca.SCASbomPackageEntity;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.sca.SCADefectGroupStatisticVO;
import com.tencent.bk.codecc.defect.vo.sca.SCADefectQueryReqVO;
import com.tencent.codecc.common.db.MongoPageHelper;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SCA工具组件持久层代码
 */
@Slf4j
@Repository
public class SCASbomPackageDao {
    public static final String COLLECTION_NAME = "t_sca_sbom_package";
    // 公共的分页大小
    private static final int PAGE_SIZE = 1000;

    @Autowired
    private MongoTemplate defectMongoTemplate;
    @Autowired
    private MongoPageHelper mongoPageHelper;
    @Autowired
    private SCASbomPackageRepository scaSbomPackageRepository;

    List<SCASbomPackageEntity> findDefectByCondition(SCAQueryWarningParams scaQueryWarningParams) {
        Criteria criteria = getQueryCriteria(scaQueryWarningParams);
        Query query = Query.query(criteria);
        return defectMongoTemplate.find(query, SCASbomPackageEntity.class);
    }

    /**
     * 根据条件分页查询SCA软件物料清单(SBOM)组件数据
     *
     * @param scaQueryWarningParams SCA组件查询参数对象，包含过滤条件
     * @param pageNum               当前页码（从1开始计数）
     * @param pageSize              每页数据量
     * @param sortField             排序字段（前端传入小驼峰格式，如"severityLevel"），默认按严重等级排序
     * @param sortType              排序方向（ASC/DESC），默认升序
     * @return 分页查询结果对象，包含组件列表和分页信息
     */
    public Page<SCASbomPackageEntity> findSCASbomPackagePageByCondition(
            SCAQueryWarningParams scaQueryWarningParams,
            Integer pageNum,
            Integer pageSize,
            String sortField,
            Sort.Direction sortType
    ) {
        // 默认按照严重等级排序
        if (StringUtils.isEmpty(sortField)) {
            sortField = "severity";
        }
        if (null == sortType) {
            sortType = Sort.Direction.ASC;
        }

        // 按照"首次发现"排序
        if ("createBuildNumber".equals(sortField)) {
            sortField = "createTime";
        }

        // 把前端传入的小驼峰排序字段转换为小写下划线的数据库字段名
        String sortFieldInDb = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, sortField);
        List<Sort.Order> sortList = Lists.newArrayList(new Sort.Order(sortType, sortFieldInDb));

        // 查询超时时间
        Duration timeout = Duration.ofSeconds(60L);
        Query query = Query.query(getQueryCriteria(scaQueryWarningParams));

        return mongoPageHelper.pageQuery(
                query, SCASbomPackageEntity.class,
                pageSize, pageNum, sortList, timeout
        );
    }

    /**
     * 按条件查询SCA组件列表
     * @param scaQueryWarningParams
     * @return
     */
    public List<SCASbomPackageEntity> findSCASbomPackageByCondition(
            SCAQueryWarningParams scaQueryWarningParams
    ) {
        Query query = Query.query(getQueryCriteria(scaQueryWarningParams));
        return defectMongoTemplate.find(query, SCASbomPackageEntity.class);
    }

    /**
     * 高效游标分页查询（基于MongoDB _id的连续分页）
     * @param scaQueryWarningParams 查询条件
     * @param lastObjectId 上一页最后记录的ObjectId（null表示第一页）
     * @param pageSize 分页大小（1-1000，默认100）
     * @return 当前页数据（按_id升序排列）
     */
    public List<SCASbomPackageEntity> findDefectByConditionWithEntityIdPage(
            SCAQueryWarningParams scaQueryWarningParams,
            String lastObjectId,
            Integer pageSize
    ) {
        final Query query = Query.query(getQueryCriteria(scaQueryWarningParams));

        // 1. 游标分页条件
        if (StringUtils.isNotBlank(lastObjectId)) {
            query.addCriteria(Criteria.where("_id").gt(new ObjectId(lastObjectId)));
        }

        // 2. 分页参数校验和设置
        int validatedPageSize = (pageSize == null || pageSize <= 0) ? 100 : Math.min(pageSize, 1000);
        query.with(Sort.by(Sort.Direction.ASC, "_id"))
                .limit(validatedPageSize);

        // 3. 执行查询
        return defectMongoTemplate.find(query, SCASbomPackageEntity.class);
    }

    /**
     * 批量更新告警忽略原因
     * @param taskId
     * @param defectList
     * @param ignoreReasonType
     * @param ignoreReason
     */
    public void batchUpdateIgnoreType(
            long taskId,
            List<SCASbomPackageEntity> defectList,
            int ignoreReasonType,
            String ignoreReason
    ) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED,
                    SCASbomPackageEntity.class);
            defectList.forEach(defectEntity -> {
                Query query = new Query();
                query.addCriteria(Criteria.where("_id").
                        is(new ObjectId(defectEntity.getEntityId())).and("task_id").is(taskId));
                Update update = new Update();
                update.set("ignore_reason_type", ignoreReasonType);
                update.set("ignore_reason", ignoreReason);
                ops.upsert(query, update);
            });
            ops.execute();
        }
    }

    /**
     * 根据条件查询SCA软件物料清单(SBOM)组件作者列表
     * @param scaQueryWarningParams
     * @return
     */
    public List<String> findAuthorsByCondition(
            SCAQueryWarningParams scaQueryWarningParams
    ) {
        // 1. 构建基础查询条件并排除空作者
        Criteria criteria = getQueryCriteria(scaQueryWarningParams)
                .and("author").ne(null);

        // 2. 直接使用distinct查询去重
        return defectMongoTemplate.findDistinct(
                Query.query(criteria),
                "author",
                COLLECTION_NAME,
                String.class
        );
    }

    /**
     * 分组查询添加筛选条件后的各状态等级告警
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
                        COLLECTION_NAME,
                        SCADefectGroupStatisticVO.class
                );

        return queryResult.getMappedResults();
    }

    /**
     * 分组查询添加筛选条件后的各风险等级告警
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
                        COLLECTION_NAME,
                        SCADefectGroupStatisticVO.class
                );

        return queryResult.getMappedResults();
    }

    /**
     * 获取查询的多条件
     * @param scaQueryWarningParams
     * @return
     */
    @NotNull
    private Criteria getQueryCriteria(
            SCAQueryWarningParams scaQueryWarningParams
    ) {
        Map<Long, List<String>> taskToolMap = scaQueryWarningParams.getTaskToolMap();
        SCADefectQueryReqVO requestProcessed = scaQueryWarningParams.getScaDefectQueryReqVO();

        String buildId = requestProcessed.getBuildId();
        Boolean direct = requestProcessed.getDirect();
        String author = requestProcessed.getAuthor();
        Set<String> status = requestProcessed.getStatus();
        String keyWord = requestProcessed.getKeyword();
        Set<Integer> severity =
                ParamUtils.convertStringSet2IntegerSet(requestProcessed.getSeverity());

        Criteria magicEmptyCriteria = Criteria.where("task_id").is(-1L);

        if (MapUtils.isEmpty(taskToolMap)) {
            log.info("taskToolMap empty, return magicEmptyCriteria, task id:{},buildId:{}",
                    requestProcessed.getTaskIdList(),buildId);
            return magicEmptyCriteria;
        }

        if (StringUtils.isNotEmpty(buildId)
                && CollectionUtils.isEmpty(scaQueryWarningParams.getScaDefectMongoIdSet())
        ) {
            log.info("scaDefectMongoIdSet empty, return magicEmptyCriteria, task id:{},buildId:{}",
                    requestProcessed.getTaskIdList(),buildId);
            return magicEmptyCriteria;
        }

        // 过滤条件: 任务id-工具
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

        // 过滤条件：快照buildId，对应的告警Id集合过滤
        Set<String> scaDefectMongoIdSet = scaQueryWarningParams.getScaDefectMongoIdSet();
        if (CollectionUtils.isNotEmpty(scaDefectMongoIdSet)) {
            andOpList.add(
                    Criteria.where("_id")
                            .in(scaDefectMongoIdSet.stream().map(ObjectId::new).collect(Collectors.toSet()))
            );
        }

        // 过滤条件：依赖方式direct
        if (direct != null) {
            if (direct) {
                andOpList.add(Criteria.where("depth").lte(1));
            } else {
                andOpList.add(Criteria.where("depth").gt(1));
            }
        }

        // 过滤条件：处理人author
        if (StringUtils.isNotBlank(author)) {
            andOpList.add(Criteria.where("author").is(author));
        }

        // 过滤条件：按创建日期范围过滤
        Criteria creatTimeCri = ParamUtils.getStartEndTimeStampCri("last_update_time",
                requestProcessed.getStartCreateTime(), requestProcessed.getEndCreateTime());
        if (creatTimeCri != null) {
            andOpList.add(creatTimeCri);
        }

        // 过滤条件：按修复日期范围过滤
        Criteria fixTimeCri = ParamUtils.getStartEndTimeStampCri("fixed_time",
                requestProcessed.getStartFixTime(), requestProcessed.getEndFixTime());
        if (fixTimeCri != null) {
            andOpList.add(fixTimeCri);
        }

        // 过滤条件：状态status
        if (CollectionUtils.isNotEmpty(status)) {
            Set<Integer> statusFilter = ParamUtils.convertStringSet2IntegerSet(status);
            boolean isSnapshotQuery = StringUtils.isNotEmpty(requestProcessed.getBuildId());
            Set<Integer> ignoreReasonTypes = requestProcessed.getIgnoreReasonTypes();
            Criteria statusCriteria =
                    ParamUtils.getStatusCriteria(statusFilter, isSnapshotQuery, ignoreReasonTypes);
            andOpList.add(statusCriteria);
        }

        // 过滤条件：按风险等级过滤
        if (CollectionUtils.isNotEmpty(severity)) {
            andOpList.add(Criteria.where("severity").in(severity));
        }

        // 过滤条件：组件名关键词keyWord
        if (StringUtils.isNotBlank(keyWord)) {
            andOpList.add(Criteria.where("name").regex(keyWord));
        }

        return new Criteria().andOperator(andOpList.toArray(new Criteria[0]));
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
            List<SCASbomPackageEntity> defectList,
            int ignoreReasonType,
            String ignoreReason,
            String ignoreAuthor
    ) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED,
                    SCASbomPackageEntity.class);
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
     * @param taskId
     * @param defectList
     * @param markFlag
     */
    public void batchMarkDefect(long taskId, List<SCASbomPackageEntity> defectList, Integer markFlag) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED,
                    SCASbomPackageEntity.class);
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

    /**
     * 批量更新告警的处理人
     * @param taskId
     * @param defectList
     * @param newAuthor
     */
    public void batchUpdateDefectAuthor(long taskId, List<SCASbomPackageEntity> defectList, Set<String> newAuthor) {
        if (CollectionUtils.isNotEmpty(defectList)) {
            BulkOperations ops = defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED,
                    SCASbomPackageEntity.class);
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