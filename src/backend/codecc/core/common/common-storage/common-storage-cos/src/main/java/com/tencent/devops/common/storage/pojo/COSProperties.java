package com.tencent.devops.common.storage.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@ConfigurationProperties(prefix = "cos")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class COSProperties {

    private String secretId;

    private String secretKey;

    private String bucket;

}
