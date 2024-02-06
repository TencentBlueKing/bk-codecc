package com.tencent.devops.common.storage;

import com.tencent.devops.common.storage.service.AbstractScanFileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.File;
import java.io.FileNotFoundException;


/**
 * NFS存储
 * NFS 挂载到本地盘，不需要其他操作
 */
@Slf4j
@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "nfs")
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class NFSScanFileStorageService extends AbstractScanFileStorageService {


    @Override
    public String doUpload(String subPath, String filename, File file)  {
        return null;
    }

    @Override
    public Boolean chunkUpload(String uploadFilePath, String filename, File file, Integer chunkNo, String uploadId) {
        return true;
    }

    @Override
    public String startChunk(String subPath, String filename) throws Exception {
        return null;
    }

    @Override
    public String finishChunk(String subPath, String filename, String uploadId) throws FileNotFoundException {
        return null;
    }

    @Override
    public void download(String localFilePath, String storageType, String urlOrPath) {
    }
}
