package com.tencent.devops.common.storage.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.devops.common.api.codecc.util.JsonUtil;
import com.tencent.devops.common.storage.vo.BkRepoResult;
import com.tencent.devops.common.storage.vo.BkRepoStartChunkVo;
import com.tencent.devops.common.util.OkhttpUtils;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 封装BkRepo的接口调用
 */
@AllArgsConstructor
public class BkRepoApi {

    private String username;

    private String password;

    private String project;

    private String repo;

    private String bkrepoHost;


    public String genericSimpleUpload(String filepath, File file) {
        String url = String.format("%s/generic/%s/%s/%s", bkrepoHost, project, repo, trimFilePath(filepath));
        OkhttpUtils.INSTANCE.doFileStreamPut(url, file, getUploadHeaders());
        return url + "?download=true";
    }


    public String genericSimpleUpload(String filepath, byte[] content) {
        String url = String.format("%s/generic/%s/%s/%s", bkrepoHost, project, repo, trimFilePath(filepath));
        OkhttpUtils.INSTANCE.doFileStreamPut(url, content, getUploadHeaders());
        return url + "?download=true";
    }

    public String startChunk(String filepath) throws Exception {
        String url = String.format("%s/generic/block/%s/%s/%s", bkrepoHost, project, repo, trimFilePath(filepath));
        String resp = OkhttpUtils.INSTANCE.doHttpPost(url, "{}", getUploadHeaders());
        BkRepoResult<BkRepoStartChunkVo> result =
                JsonUtil.INSTANCE.to(resp, new TypeReference<BkRepoResult<BkRepoStartChunkVo>>() {
                });
        if (result == null || !result.isOk() || result.getData() == null) {
            throw new Exception("startChunk : " + filepath + " return "
                    + JsonUtil.INSTANCE.toJson(result) + " cause error.");
        }
        return result.getData().getUploadId();
    }


    public Boolean genericChunkUpload(String filepath, File file, Integer chunkNo, String uploadId) {
        String url = String.format("%s/generic/%s/%s/%s", bkrepoHost, project, repo, trimFilePath(filepath));
        Map<String, String> headers = getUploadHeaders();
        if (chunkNo != null) {
            headers.put("X-BKREPO-SEQUENCE", chunkNo.toString());
        }
        if (StringUtils.hasLength(uploadId)) {
            headers.put("X-BKREPO-UPLOAD-ID", uploadId);
        }
        OkhttpUtils.INSTANCE.doFileStreamPut(url, file, headers);
        return true;
    }

    public String genericFinishChunk(String filepath, String uploadId) {
        String url = String.format("%s/generic/block/%s/%s/%s", bkrepoHost, project, repo, trimFilePath(filepath));
        Map<String, String> headers = getUploadHeaders();
        if (StringUtils.hasLength(uploadId)) {
            headers.put("X-BKREPO-UPLOAD-ID", uploadId);
        }
        OkhttpUtils.INSTANCE.doHttpPut(url, "{}", headers);
        return String.format("%s/generic/%s/%s/%s", bkrepoHost, project, repo, trimFilePath(filepath))
                + "?download=true";
    }

    private Map<String, String> getUploadHeaders() {
        Map<String, String> headers = new HashMap<>(getAuthHeaders());
        headers.put("X-BKREPO-OVERWRITE", "true");
        return headers;
    }

    /**
     * 认证头信息
     *
     * @return
     */
    public Map<String, String> getAuthHeaders() {
        String base64Src = username + ":" + password;
        String base64 = Base64.getEncoder().encodeToString(base64Src.getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + base64);
        return headers;
    }

    /**
     * 设置上传文件的超时信息
     *
     * @param headers
     * @param expires
     * @return
     */
    public Map<String, String> setExpires(Map<String, String> headers, Long expires) {
        Map<String, String> newHeaders = CollectionUtils.isEmpty(headers) ? new HashMap<>() : headers;
        if (expires == null || expires < 0) {
            expires = 0L;
        }
        newHeaders.put("X-BKREPO-EXPIRES", expires.toString());
        return headers;
    }

    public void download(String filePath, String localFilePath) {
        Map<String, String> headers = getAuthHeaders();
        String url = getDownloadUrl(filePath);
        OkhttpUtils.INSTANCE.downloadFile(url, new File(localFilePath), headers);
    }

    public byte[] download(String filePath) {
        Map<String, String> headers = getAuthHeaders();
        String url = getDownloadUrl(filePath);
        return OkhttpUtils.INSTANCE.download(url, headers);
    }

    public String delete(String filePath) {
        String url = String.format("%s/generic/%s/%s/%s", bkrepoHost, project, repo, trimFilePath(filePath));
        return OkhttpUtils.INSTANCE.doHttpDelete(url, "{}", getAuthHeaders());
    }

    private String getDownloadUrl(String filepath) {
        return String.format("%s/generic/%s/%s/%s?download=true", bkrepoHost, project, repo,
                trimFilePath(filepath));
    }

    private String trimFilePath(String filepath) {
        if (filepath.startsWith("/")) {
            return filepath.substring(1).trim();
        }
        return filepath.trim();
    }

}
