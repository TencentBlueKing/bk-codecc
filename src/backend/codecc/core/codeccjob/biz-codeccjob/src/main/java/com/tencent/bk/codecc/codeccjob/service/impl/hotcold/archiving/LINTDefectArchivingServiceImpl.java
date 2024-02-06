package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.archiving;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.codeccjob.service.ColdDataArchivingService;
import com.tencent.bk.codecc.codeccjob.service.impl.hotcold.pojo.HotColdConstants;
import com.tencent.bk.codecc.codeccjob.service.impl.hotcold.pojo.PageFetchResult;
import com.tencent.bk.codecc.codeccjob.service.impl.hotcold.pojo.PageFetchResult.LintPageable;
import com.tencent.bk.codecc.defect.model.defect.LintDefectV2Entity;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants.ColdDataArchivingType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
public class LINTDefectArchivingServiceImpl
        extends AbstractDefectArchivingTemplate<LintDefectV2Entity>
        implements ColdDataArchivingService {

    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;
    @Autowired
    private Client client;

    @Override
    public ColdDataArchivingType coldDataArchivingType() {
        return ColdDataArchivingType.LINT;
    }

    @Override
    protected PageFetchResult<LintDefectV2Entity> getDefectList(
            long taskId,
            @Nullable PageFetchResult<LintDefectV2Entity> lastResult
    ) {

        List<String> toolNameList;
        String marker;
        int skip;
        int limit;

        if (lastResult == null || lastResult.getNextPageableForLint() == null) {
            toolNameList = client.get(ServiceTaskRestResource.class).getTaskToolNameList(taskId).getData();
            if (CollectionUtils.isEmpty(toolNameList)) {
                return new PageFetchResult<>(false, Lists.newArrayList(), null, null);
            }

            skip = 0;
            limit = HotColdConstants.BATCH_SIZE_FOR_ARCHIVING;
            marker = null;
        } else {
            LintPageable nextPageableForLint = lastResult.getNextPageableForLint();
            skip = nextPageableForLint.getSkip();
            limit = nextPageableForLint.getLimit();
            marker = nextPageableForLint.getMarker();
            toolNameList = nextPageableForLint.getToolNameList();
            if (toolNameList == null || toolNameList.size() == 0) {
                return new PageFetchResult<>(false, Lists.newArrayList(), null, null);
            }
        }

        List<LintDefectV2Entity> defectList = lintDefectV2Dao.fetchLintDefectEfficiently(
                taskId,
                toolNameList,
                marker,
                skip,
                limit
        );

        boolean hasNext = !CollectionUtils.isEmpty(defectList) && defectList.size() == limit;
        if (!hasNext) {
            return new PageFetchResult<>(false, defectList, null, null);
        }

        LintDefectV2Entity last = defectList.get(defectList.size() - 1);
        long sameCount = defectList.stream().filter(x -> checkRelPathEquals(x.getRelPath(), last.getRelPath())).count();
        if (checkRelPathEquals(last.getRelPath(), marker)) {
            skip += sameCount;
        } else {
            marker = last.getRelPath();
            skip = (int) sameCount;
        }

        LintPageable nextLintPageable = new LintPageable(marker, skip, limit, toolNameList);

        return new PageFetchResult<>(true, defectList, null, nextLintPageable);
    }

    @Override
    protected List<LintDefectV2Entity> getDefectListCore(long taskId, Pageable pageable) {
        throw new UnsupportedOperationException("no reference");
    }

    private boolean checkRelPathEquals(String source, String target) {
        if (source == null && target == null) {
            return true;
        }
        if (source == null || target == null) {
            return false;
        }

        return source.equals(target);
    }
}
