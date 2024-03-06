package com.tencent.devops.common.storage;

import com.tencent.devops.common.storage.service.AbstractScanFileStorageService;
import java.io.File;
import java.io.FileNotFoundException;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "bkrepo-csi")
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class BkRepoCSIScanFileStorageService extends AbstractScanFileStorageService {

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
