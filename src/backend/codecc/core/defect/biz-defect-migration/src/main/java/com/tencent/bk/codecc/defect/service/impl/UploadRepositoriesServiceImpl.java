package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.CodeRepoInfoRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.ToolBuildInfoRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongorepository.ToolBuildStackRepository;
import com.tencent.bk.codecc.defect.dao.defect.mongotemplate.ToolBuildStackDao;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoEntity;
import com.tencent.bk.codecc.defect.model.incremental.CodeRepoInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildInfoEntity;
import com.tencent.bk.codecc.defect.model.incremental.ToolBuildStackEntity;
import com.tencent.bk.codecc.defect.service.UploadRepositoriesService;
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO;
import com.tencent.devops.common.api.CodeRepoVO;
import com.tencent.bk.codecc.defect.vo.coderepository.UploadRepositoriesVO;
import com.tencent.devops.common.api.pojo.codecc.Result;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.redis.lock.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.tencent.devops.common.util.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_CODE_REPO_STAT;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_CODE_REPO_STAT;

/**
 * 上报仓库信息服务实现类
 *
 * @version V1.0
 * @date 2019/11/15
 */
@Slf4j
@Service("uploadRepositoriesService")
public class UploadRepositoriesServiceImpl implements UploadRepositoriesService {
    @Autowired
    private CodeRepoInfoRepository codeRepoRepository;
    @Autowired
    private ToolBuildStackRepository toolBuildStackRepository;
    @Autowired
    private ToolBuildInfoRepository toolBuildInfoRepository;
    @Autowired
    private ToolBuildStackDao toolBuildStackDao;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    /**
     * 上报仓库信息
     *
     * @param uploadRepositoriesVO
     * @return
     */
    @SuppressWarnings("all")
    @Override
    public Result uploadRepositories(UploadRepositoriesVO uploadRepositoriesVO) {
        log.info("upload repo info, task id: {}, tool name: {}", uploadRepositoriesVO.getTaskId(),
                uploadRepositoriesVO.getToolName());
        long taskId = uploadRepositoriesVO.getTaskId();
        String toolName = uploadRepositoriesVO.getToolName();
        String buildId = uploadRepositoriesVO.getBuildId();
        List<CodeRepoVO> codeRepoList = uploadRepositoriesVO.getRepoList();
        List<String> deleteFiles = uploadRepositoriesVO.getDeleteFiles();
        Set<String> rootPaths = uploadRepositoriesVO.getRootPaths();
        List<String> repoWhiteList = uploadRepositoriesVO.getRepoWhiteList();
        List<String> repoRelativePathList = uploadRepositoriesVO.getRepoRelativePathList();
        log.info("upload repo info, task id: {}, repoRelativePathList: {}", uploadRepositoriesVO.getTaskId(),
                repoRelativePathList);
        // 更新构建运行时栈表
        ToolBuildStackEntity toolBuildStackEntity =
                toolBuildStackRepository.findFirstByTaskIdAndToolNameAndBuildId(taskId, toolName, buildId);
        if (toolBuildStackEntity == null) {
            ToolBuildInfoEntity toolBuildInfoEntity = toolBuildInfoRepository.findFirstByTaskIdAndToolName(taskId, toolName);
            toolBuildStackEntity = new ToolBuildStackEntity();
            toolBuildStackEntity.setTaskId(taskId);
            toolBuildStackEntity.setToolName(toolName);
            toolBuildStackEntity.setBuildId(buildId);
            toolBuildStackEntity
                    .setBaseBuildId(toolBuildInfoEntity != null ? toolBuildInfoEntity.getDefectBaseBuildId() : "");
            toolBuildStackEntity.setFullScan(true);
        }
        toolBuildStackEntity.setDeleteFiles(deleteFiles);
        toolBuildStackEntity.setRootPaths(rootPaths);
        toolBuildStackDao.upsert(toolBuildStackEntity);

        // 校验构建号对应的仓库信息是否已存在
        final String lockKey = new StringBuffer(RedisKeyConstants.LOCK_T_CODE_REPO_INFO).append(":").
                append(taskId).append(":").
                append(buildId).toString().trim();
        final long lockTime = 15L;
        RedisLock locker = null;
        try {
            locker = new RedisLock(redisTemplate, lockKey, lockTime);
            boolean lockSuccess = locker.tryLock();

            if (!lockSuccess) {
                return new Result(0, CommonMessageCode.SUCCESS, "upload repo info, the lock already exists.");
            }
            log.info("upload repo info, redis lock: {}", lockKey);
            CodeRepoInfoEntity codeRepoInfo = codeRepoRepository.findFirstByTaskIdAndBuildId(taskId, buildId);
            if (codeRepoInfo != null) {
                return new Result(0, CommonMessageCode.SUCCESS, "repo info of this build id is already exist.");
            }

            // 更新仓库列表和构建ID
            List<CodeRepoEntity> codeRepoEntities = Lists.newArrayList();
            if (CollectionUtils.isNotEmpty(codeRepoList)) {
                log.info("upload repo info, codeRepoList: {}", codeRepoList);
                for (CodeRepoVO codeRepoVO : codeRepoList) {
                    CodeRepoEntity codeRepoEntity = new CodeRepoEntity();
                    BeanUtils.copyProperties(codeRepoVO, codeRepoEntity);
                    codeRepoEntities.add(codeRepoEntity);
                }
            }
            codeRepoInfo = new CodeRepoInfoEntity(taskId, buildId, codeRepoEntities,
                    repoWhiteList, deleteFiles, repoRelativePathList);
            Long currentTime = System.currentTimeMillis();
            codeRepoInfo.setUpdatedDate(currentTime);
            codeRepoInfo.setCreatedDate(currentTime);
            codeRepoRepository.save(codeRepoInfo);

            // 代码库/分支统计
            UploadTaskLogStepVO stepVO = new UploadTaskLogStepVO();
            stepVO.setTaskId(taskId);
            stepVO.setPipelineBuildId(buildId);
            rabbitTemplate.convertAndSend(EXCHANGE_CODE_REPO_STAT, ROUTE_CODE_REPO_STAT, stepVO);

            return new Result(0, CommonMessageCode.SUCCESS, "upload repo info success.");
        } finally {
            if (locker != null && locker.isLocked()) {
                locker.unlock();
            }
            log.info("upload repo info, unlock : {}", lockKey);
        }
    }
}
