
package com.domye.picture.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信公众号配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat")
public class WeChatConfig {

    /**
     * 微信公众号appId
     */
    private String appId;

    /**
     * 微信公众号appSecret
     */
    private String appSecret;

    /**
     * 微信服务器token
     */
    private String token;

    /**
     * 微信消息加密密钥EncodingAESKey
     */
    private String encodingAesKey;

    /**
     * 微信服务器地址
     */
    private String serverUrl;
}
