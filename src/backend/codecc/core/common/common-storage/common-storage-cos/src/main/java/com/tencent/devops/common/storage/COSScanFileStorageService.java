package com.tencent.devops.common.storage;

import java.io.FileNotFoundException;

public class COSScanFileStorageService implements ScanFileStorageService {

    @Override
    public String upload(String localFilePath, String subPath, String filename) throws FileNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String startChunk(String subPath, String filename) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean chunkUpload(String localFilePath, String subPath, String filename, Integer chunkNo, String uploadId)
            throws FileNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String finishChunk(String subPath, String filename, String uploadId) throws FileNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void download(String localFilePath, String storageType, String urlOrPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getStorageType() {
        return null;
    }

    @Override
    public Boolean ifNeedAndCanDownload(String storageType, String urlOrPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean ifNeedLocalMerge(String storageType) {
        throw new UnsupportedOperationException();
    }
}
