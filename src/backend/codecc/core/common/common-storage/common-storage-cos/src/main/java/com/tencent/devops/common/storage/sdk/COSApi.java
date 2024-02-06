package com.tencent.devops.common.storage.sdk;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ListObjectsRequest;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.TransferManagerConfiguration;
import com.qcloud.cos.utils.IOUtils;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.springframework.util.ObjectUtils;

@Slf4j
public class COSApi implements AutoCloseable {

    private static final String COS_REGION = "ap-guangzhou";
    private final TransferManager transferManager;
    private final COSClient cosClient;
    private final String bucket;

    public COSApi(String secretId, String secretKey, String bucket) {
        cosClient = createCOSClient(secretId, secretKey);
        transferManager = createTransferManager(cosClient);
        this.bucket = bucket;
    }

    /**
     * 移动对象
     * <a href="https://cloud.tencent.com/document/product/436/65936">文档</a>
     */
    public boolean moveObject(String sourceObjName, String destinationObjName) {
        try {
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(
                    new Region(COS_REGION),
                    bucket,
                    sourceObjName,
                    bucket,
                    destinationObjName
            );
            transferManager.copy(copyObjectRequest).waitForCopyResult();
            cosClient.deleteObject(bucket, sourceObjName);

            return true;
        } catch (Throwable t) {
            log.error("move object fail, source: {}, destination: {}", sourceObjName, destinationObjName, t);

            return false;
        }
    }

    /**
     * 对象上传
     * <a href="https://cloud.tencent.com/document/product/436/65935">文档</a>
     *
     * @param objName
     * @param inputStream
     * @return
     */
    public boolean uploadObject(String objName, final InputStream inputStream) {
        try {
            if (ObjectUtils.isEmpty(objName)) {
                return false;
            }

            if (inputStream == null) {
                log.warn("input stream cannot be null, obj name: {}", objName);
                return false;
            }

            ObjectMetadata objectMetadata = new ObjectMetadata();
            // NOCC:LineLength(设计如此:)
            // No content length specified for stream data.  Stream contents will be buffered in memory and could result in out of memory errors.
            objectMetadata.setContentLength(inputStream.available());
            PutObjectRequest request = new PutObjectRequest(bucket, objName, inputStream, new ObjectMetadata());
            transferManager.upload(request).waitForUploadResult();

            return true;
        } catch (Throwable t) {
            log.error("cos upload fail, obj name: {}", objName, t);

            return false;
        }
    }

    /**
     * 对象下载
     * <a href="https://cloud.tencent.com/document/product/436/65937">文档</a>
     *
     * @param objName
     * @return
     */
    public byte[] downloadObjectByte(String objName) {
        try {
            InputStream inputStream = downloadObjectStream(objName);

            return IOUtils.toByteArray(inputStream);
        } catch (Throwable t) {
            log.error("cos down fail, obj name: {}", objName, t);

            return new byte[0];
        }
    }

    /**
     * 对象下载
     * <a href="https://cloud.tencent.com/document/product/436/65937">文档</a>
     *
     * @param objName
     * @return
     */
    public InputStream downloadObjectStream(String objName) {
        GetObjectRequest request = new GetObjectRequest(bucket, objName);
        // 设置分 range 多线程同时下载的并发数，不设置默认为1（此参数在5.6.168及以上版本支持）
        request.setDownloadPartsThreads(4);

        return cosClient.getObject(request).getObjectContent();
    }

    /**
     * 列出对象列表
     * <a href="https://cloud.tencent.com/document/product/436/65938">文档</a>
     *
     * @param prefix
     * @return
     */
    public List<String> listObjects(String prefix) {
        ListObjectsRequest request = new ListObjectsRequest();
        request.setBucketName(bucket);
        request.setPrefix(prefix);
        // 最大返回的成员个数
        request.setMaxKeys(1000);

        ObjectListing objectListing = null;
        List<String> objectNameList = Lists.newArrayList();

        do {
            objectListing = cosClient.listObjects(request);
            objectNameList.addAll(
                    objectListing.getObjectSummaries().stream()
                            .map(COSObjectSummary::getKey)
                            .collect(Collectors.toList())
            );

            // 标记下一次开始的位置
            request.setMarker(objectListing.getNextMarker());
        } while (objectListing.isTruncated());

        return objectNameList;
    }

    @Override
    public void close() {
        if (transferManager != null) {
            // 指定参数为 true, 则同时会关闭 transferManager 内部的 COSClient 实例。
            // 指定参数为 false, 则不会关闭 transferManager 内部的 COSClient 实例。
            transferManager.shutdownNow(true);
        }
    }

    private COSClient createCOSClient(String secretId, String secretKey) {
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(COS_REGION));
        // 走内网通道，不支持https
        String endpoint = String.format("cos-internal.%s.tencentcos.cn", COS_REGION);
        clientConfig.setEndpointBuilder(new COSEndpointBuilder(endpoint));
        clientConfig.setHttpProtocol(HttpProtocol.http);

        return new COSClient(cred, clientConfig);
    }

    private TransferManager createTransferManager(COSClient cosClient) {
        // 自定义线程池大小，建议在客户端与 COS 网络充足（例如使用腾讯云的 CVM，同地域上传 COS）的情况下，设置成16或32即可，可较充分的利用网络资源
        // 对于使用公网传输且网络带宽质量不高的情况，建议减小该值，避免因网速过慢，造成请求超时。
        ExecutorService threadPool = Executors.newFixedThreadPool(32);
        TransferManager transferManager = new TransferManager(cosClient, threadPool);

        // 设置高级接口的配置项
        // 分块上传阈值和分块大小分别为 10MB 和 2MB
        TransferManagerConfiguration transferManagerConfiguration = new TransferManagerConfiguration();
        transferManagerConfiguration.setMultipartUploadThreshold(10 * 1024 * 1024);
        transferManagerConfiguration.setMinimumUploadPartSize(2 * 1024 * 1024);
        transferManager.setConfiguration(transferManagerConfiguration);

        return transferManager;
    }
}
