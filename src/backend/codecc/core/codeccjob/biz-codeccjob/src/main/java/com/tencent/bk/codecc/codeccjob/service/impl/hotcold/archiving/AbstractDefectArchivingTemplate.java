package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.archiving;

import com.alibaba.fastjson.JSON;
import com.tencent.bk.codecc.codeccjob.dao.defect.mongorepository.ColdDataArchivingLogRepository;
import com.tencent.bk.codecc.codeccjob.service.ColdDataArchivingService;
import com.tencent.bk.codecc.codeccjob.service.impl.hotcold.pojo.ArchivingFileModel;
import com.tencent.bk.codecc.codeccjob.service.impl.hotcold.pojo.HotColdConstants;
import com.tencent.bk.codecc.codeccjob.service.impl.hotcold.pojo.PageFetchResult;
import com.tencent.bk.codecc.defect.model.ColdDataArchivingLogEntity;
import com.tencent.codecc.common.db.CommonEntity;
import com.tencent.devops.common.storage.sdk.COSApi;
import com.tencent.devops.common.util.ThreadUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.CollectionUtils;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

/**
 * 告警数据归档模板方法
 *
 * @param <T>
 */
@Slf4j
public abstract class AbstractDefectArchivingTemplate<T extends CommonEntity> implements ColdDataArchivingService {

    @Autowired
    protected COSApi cosApi;
    @Autowired
    protected ColdDataArchivingLogRepository coldDataArchivingLogRepository;

    @Override
    public boolean archive(long taskId) {
        ColdDataArchivingLogEntity archivingLog =
                coldDataArchivingLogRepository.findFirstByTaskIdAndType(taskId, coldDataArchivingType().name());
        if (archivingLog != null && archivingLog.isSuccess()) {
            return true;
        }

        boolean finalRet = true;
        int cosShardIndex = 0;
        int defectCount = 0;
        PageFetchResult<T> lastPageRet = null;
        long beginTime = System.currentTimeMillis();

        try {
            while (true) {
                PageFetchResult<T> curPageRet = getDefectList(taskId, lastPageRet);
                List<T> defectList = curPageRet.getData();
                if (!CollectionUtils.isEmpty(defectList)) {
                    defectCount += defectList.size();
                    String json = defectToJson(new ArchivingFileModel<>(coldDataArchivingType(), defectList));
                    InputStream inputStream = compressJson(taskId, json);
                    boolean b = uploadToCOS(taskId, inputStream, cosShardIndex++);
                    if (!b) {
                        finalRet = false;
                        // 有1个片上传失败就没必要继续
                        break;
                    }
                }

                if (!curPageRet.getHasNext()) {
                    break;
                }

                curPageRet.close();
                lastPageRet = curPageRet;
                ThreadUtils.sleep(50);
            }
        } catch (Throwable t) {
            log.error("archive cold data fail, task id: {}, type: {}", taskId, coldDataArchivingType().name(), t);
            finalRet = false;
        } finally {
            long cost = System.currentTimeMillis() - beginTime;
            logResult(taskId, finalRet, defectCount, archivingLog, cost);
        }

        return finalRet;
    }

    /**
     * 获取原始告警
     *
     * @param taskId
     * @param lastResult
     * @return
     */
    protected PageFetchResult<T> getDefectList(long taskId, @Nullable PageFetchResult<T> lastResult) {
        int pageSize = HotColdConstants.BATCH_SIZE_FOR_ARCHIVING;
        Pageable pageable = lastResult != null && lastResult.getNextPageable() != null
                ? lastResult.getNextPageable() : Pageable.ofSize(pageSize);
        List<T> defectList = getDefectListCore(taskId, pageable);

        if (!CollectionUtils.isEmpty(defectList) && defectList.size() == pageSize) {
            return new PageFetchResult<>(
                    true,
                    defectList,
                    PageRequest.of(pageable.getPageNumber() + 1, pageSize),
                    null
            );
        } else {
            return new PageFetchResult<>(false, defectList, null, null);
        }
    }

    protected abstract List<T> getDefectListCore(long taskId, Pageable pageable);


    /**
     * 告警实体转json
     *
     * @param model
     * @return
     */
    protected String defectToJson(ArchivingFileModel<T> model) {
        return JSON.toJSONString(model);
    }

    /**
     * 压缩json数据
     *
     * @param taskId
     * @param data
     * @return
     */
    protected InputStream compressJson(long taskId, String data) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (XZOutputStream xzOutputStream = new XZOutputStream(outputStream, new LZMA2Options())) {
                xzOutputStream.write(data.getBytes(StandardCharsets.UTF_8));
            }

            return new ByteArrayInputStream(outputStream.toByteArray());
        } catch (Throwable t) {
            log.error("compress json fail, task id: {}", taskId, t);

            return null;
        }
    }

    /**
     * 文件流上传到腾讯云COS
     *
     * @param taskId
     * @param inputStream
     * @param shardIndex
     * @return
     */
    protected boolean uploadToCOS(long taskId, InputStream inputStream, int shardIndex) {
        String objName = String.format(
                HotColdConstants.COS_PREFIX_FORMATTER_WITH_SHARD,
                taskId,
                coldDataArchivingType().name().toLowerCase(Locale.ENGLISH),
                shardIndex
        );

        return cosApi.uploadObject(objName, inputStream);
    }

    /**
     * 保存归档结果
     *
     * @param taskId
     */
    protected void logResult(
            long taskId, boolean finalResult, int defectCount,
            ColdDataArchivingLogEntity archivingLog, long cost
    ) {
        if (archivingLog == null) {
            archivingLog = new ColdDataArchivingLogEntity(
                    taskId,
                    coldDataArchivingType().name(),
                    finalResult,
                    defectCount,
                    cost
            );
            archivingLog.applyAuditInfoOnCreate();
        } else {
            archivingLog.setDataCount(defectCount);
            archivingLog.setSuccess(finalResult);
            archivingLog.setCost(cost);
            archivingLog.applyAuditInfoOnUpdate();
        }

        coldDataArchivingLogRepository.save(archivingLog);
    }
}
