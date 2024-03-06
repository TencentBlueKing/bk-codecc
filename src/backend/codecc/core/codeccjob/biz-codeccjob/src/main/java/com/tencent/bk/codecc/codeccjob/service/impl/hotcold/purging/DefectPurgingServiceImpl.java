package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.purging;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.CCNDefectRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.CLOCDefectRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.DUPCDefectRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.StatDefectRepository;
import com.tencent.devops.common.constant.ComConstants.ColdDataPurgingType;
import com.tencent.devops.common.constant.ComConstants.ToolPattern;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 原始告警清理
 */
@Service
@Slf4j
public class DefectPurgingServiceImpl extends AbstractDefectPurgingTemplate {

    private static final Map<Long, List<KeyCountPair>> REMARK_DETAIL_MAP = Maps.newConcurrentMap();

    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;
    @Autowired
    private CCNDefectRepository ccnDefectRepository;
    @Autowired
    private CLOCDefectRepository clocDefectRepository;
    @Autowired
    private DUPCDefectRepository dupcDefectRepository;
    @Autowired
    private StatDefectRepository statDefectRepository;

    @Override
    protected long purgeCore(long taskId) {
        long lintDefectCount = lintDefectV2Repository.deleteByTaskId(taskId);
        long ccnDefectCount = ccnDefectRepository.deleteByTaskId(taskId);
        long clocDefectCount = clocDefectRepository.deleteByTaskId(taskId);
        long dupcDefectCount = dupcDefectRepository.deleteByTaskId(taskId);
        long statDefectCount = statDefectRepository.deleteByTaskId(taskId);

        List<KeyCountPair> detailList = Stream.of(
                new KeyCountPair(ToolPattern.LINT.name(), lintDefectCount),
                new KeyCountPair(ToolPattern.CCN.name(), ccnDefectCount),
                new KeyCountPair(ToolPattern.CLOC.name(), clocDefectCount),
                new KeyCountPair(ToolPattern.DUPC.name(), dupcDefectCount),
                new KeyCountPair(ToolPattern.STAT.name(), statDefectCount)
        ).collect(Collectors.toList());
        REMARK_DETAIL_MAP.put(taskId, detailList);

        return lintDefectCount + ccnDefectCount + clocDefectCount + dupcDefectCount + statDefectCount;
    }

    @Override
    public ColdDataPurgingType coldDataPurgingType() {
        return ColdDataPurgingType.DEFECT;
    }

    @Override
    protected String getRemark(long taskId) {
        List<KeyCountPair> list = REMARK_DETAIL_MAP.get(taskId);

        if (list != null) {
            try {
                return JSON.toJSONString(list, false);
            } catch (Throwable t) {
                log.error("getRemark fail, task id: {}", taskId, t);
                return "";
            } finally {
                REMARK_DETAIL_MAP.remove(taskId);
            }
        }

        return super.getRemark(taskId);
    }

    @AllArgsConstructor
    @Data
    public static class KeyCountPair {

        private String key;
        private long count;
    }
}
