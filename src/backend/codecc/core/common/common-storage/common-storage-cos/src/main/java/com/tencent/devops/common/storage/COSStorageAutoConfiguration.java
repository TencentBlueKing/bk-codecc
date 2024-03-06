package com.tencent.devops.common.storage;

import com.tencent.devops.common.storage.pojo.COSProperties;
import com.tencent.devops.common.storage.sdk.COSApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(COSProperties.class)
@ConditionalOnProperty(prefix = "cos", name = "secretKey")
public class COSStorageAutoConfiguration {

    @Autowired
    private COSProperties cosProperties;

    @Bean
    public COSApi cosApi() {
        return new COSApi(cosProperties.getSecretId(), cosProperties.getSecretKey(), cosProperties.getBucket());
    }
}
