package com.tencent.bk.codecc.codeccjob.config;

import com.tencent.bk.codecc.defect.mapping.DefectConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EntityMappingConfig {

    @Bean
    public DefectConverter defectConverter() {
        return new DefectConverter();
    }
}
