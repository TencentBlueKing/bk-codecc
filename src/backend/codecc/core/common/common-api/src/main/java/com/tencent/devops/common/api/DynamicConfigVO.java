package com.tencent.devops.common.api;

import lombok.Data;

/**
 * 动态配置
 *
 * @version V1.0
 * @date 2019/4/25
 */
@Data
public class DynamicConfigVO {

    /**
     * 键值（唯一），标识这个配置
     */
    private String key;

    /**
     * 服务名称
     */
    private String service;
    /**
     * 描述，方便理解变量含义
     */
    private String decs;

    /**
     * 对应的值，字符串，复杂类型可使用JSON字符串，再解析使用
     */
    private String value;
}
