package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.purging;

import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.CCNStatisticRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.CLOCStatisticRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.CcnClusterStatisticRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.CommonStatisticRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.DUPCStatisticRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.DefectClusterStatisticRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.LintStatisticRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.MetricsRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.SecurityClusterStatisticRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.StandardClusterStatisticRepository;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.StatStatisticRepository;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants.ColdDataPurgingType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 统计相关清理
 */
@Service
public class StatisticPurgingServiceImpl extends AbstractDefectPurgingTemplate {

    @Autowired
    private DUPCStatisticRepository dupcStatisticRepository;
    @Autowired
    private CLOCStatisticRepository clocStatisticRepository;
    @Autowired
    private CCNStatisticRepository ccnStatisticRepository;
    @Autowired
    private LintStatisticRepository lintStatisticRepository;
    @Autowired
    private CommonStatisticRepository commonStatisticRepository;
    @Autowired
    private StatStatisticRepository statStatisticRepository;
    @Autowired
    private SecurityClusterStatisticRepository securityClusterStatisticRepository;
    @Autowired
    private StandardClusterStatisticRepository standardClusterStatisticRepository;
    @Autowired
    private CcnClusterStatisticRepository ccnClusterStatisticRepository;
    @Autowired
    private DefectClusterStatisticRepository defectClusterStatisticRepository;
    @Autowired
    private MetricsRepository metricsRepository;
    @Autowired
    private Client client;

    @Override
    protected long purgeCore(long taskId) {
        // 保留最后一次构建的工具维度统计信息
        String latestBuildId = getLatestBuildId(taskId);

        return securityClusterStatisticRepository.deleteByTaskId(taskId)
                + standardClusterStatisticRepository.deleteByTaskId(taskId)
                + ccnClusterStatisticRepository.deleteByTaskId(taskId)
                + defectClusterStatisticRepository.deleteByTaskId(taskId)
                + metricsRepository.deleteByTaskId(taskId)
                + dupcStatisticRepository.deleteByTaskIdAndBuildIdIsNot(taskId, latestBuildId)
                + clocStatisticRepository.deleteByTaskIdAndBuildIdIsNot(taskId, latestBuildId)
                + ccnStatisticRepository.deleteByTaskIdAndBuildIdIsNot(taskId, latestBuildId)
                + lintStatisticRepository.deleteByTaskIdAndBuildIdIsNot(taskId, latestBuildId)
                + commonStatisticRepository.deleteByTaskIdAndBuildIdIsNot(taskId, latestBuildId)
                + statStatisticRepository.deleteByTaskIdAndBuildIdIsNot(taskId, latestBuildId);
    }

    @Override
    public ColdDataPurgingType coldDataPurgingType() {
        return ColdDataPurgingType.STATISTIC;
    }

    /**
     * 获取最后一次构建Id
     *
     * @param taskId
     * @return
     */
    private String getLatestBuildId(long taskId) {
        return client.get(ServiceTaskRestResource.class).getLatestBuildId(taskId).getData();
    }
}
