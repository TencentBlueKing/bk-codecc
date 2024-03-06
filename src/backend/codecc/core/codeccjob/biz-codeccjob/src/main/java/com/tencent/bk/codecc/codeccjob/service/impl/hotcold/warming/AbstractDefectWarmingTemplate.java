package com.tencent.bk.codecc.codeccjob.service.impl.hotcold.warming;

import com.google.common.collect.Lists;
import com.tencent.bk.codecc.codeccjob.service.ColdDataWarmingService;
import com.tencent.bk.codecc.codeccjob.service.impl.hotcold.pojo.HotColdConstants;
import com.tencent.codecc.common.db.CommonEntity;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.storage.sdk.COSApi;
import com.tencent.devops.common.util.ThreadUtils;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.tukaani.xz.XZInputStream;

/**
 * 数据加热模板方法
 *
 * @param <T>
 */
@Slf4j
public abstract class AbstractDefectWarmingTemplate<T extends CommonEntity> implements ColdDataWarmingService {

    @Autowired
    protected MongoTemplate defectMongoTemplate;
    @Autowired
    private COSApi cosApi;
    @Autowired
    private Client client;

    @Override
    public void warm(long taskId) {
        List<String> cosObjectNameList = cosApi.listObjects(
                String.format(
                        HotColdConstants.COS_PREFIX_FORMATTER,
                        taskId,
                        coldDataArchivingType().name().toLowerCase(Locale.ENGLISH)
                )
        );

        if (CollectionUtils.isEmpty(cosObjectNameList)) {
            log.error("defect warming, cannot find archive file on cos, task id: {}, type: {}", taskId,
                    coldDataArchivingType());
            return;
        }

        preHandle(taskId);

        for (String cosObjectName : cosObjectNameList) {
            String json = decompressJson(taskId, cosObjectName);
            if (ObjectUtils.isEmpty(json)) {
                continue;
            }

            List<T> defectList = getDefectList(json);
            if (CollectionUtils.isEmpty(defectList)) {
                continue;
            }

            // 分批插入
            List<List<T>> partitionList = Lists.partition(defectList, getBatchPageSize());
            for (List<T> partition : partitionList) {
                defectMongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, getCollectionName())
                        .insert(partition)
                        .execute();
            }

            // 每份cos文件最多会有10万告警
            ThreadUtils.sleep(50);
        }

        postHandle(taskId, cosObjectNameList);
    }

    protected abstract List<T> getDefectList(String json);

    protected abstract String getCollectionName();

    protected int getBatchPageSize() {
        return HotColdConstants.BATCH_SIZE_FOR_WARMING;
    }

    /**
     * 数据加热前的前置处理，主要执行清理操作；
     * 有可能上次加热失败，由于主键的唯一性，导致下一次幂等写入报错
     *
     * @param taskId
     */
    protected abstract void preHandle(long taskId);

    protected void postHandle(long taskId, List<String> cosObjectNameList) {
        log.info("defect warming, success, task id: {}, type: {}, cos object: {}", taskId, coldDataArchivingType(),
                cosObjectNameList);

        long now = System.currentTimeMillis();

        // 完成加热的数据统一移动到backup文件夹，方便日后一次回收释放
        for (String cosObjectName : cosObjectNameList) {
            // format: /backup/{原对象}.{时间戳}
            String destinationObjName = String.format("/backup/%s.bak%d", StringUtils.strip(cosObjectName, "/"), now);
            cosApi.moveObject(cosObjectName, destinationObjName);
        }
    }

    protected String decompressJson(long taskId, String cosObjectName) {
        try (InputStream inputStream = cosApi.downloadObjectStream(cosObjectName);
                XZInputStream xzInput = new XZInputStream(inputStream);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = xzInput.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }

            return outputStream.toString(StandardCharsets.UTF_8.name());
        } catch (Throwable t) {
            log.error("decompress json fail, task id: {},  cos object name: {}", taskId, cosObjectName, t);

            return "";
        }
    }
}
