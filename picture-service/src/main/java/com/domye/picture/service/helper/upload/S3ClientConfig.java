package com.domye.picture.service.helper.upload;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@ConfigurationProperties(prefix = "s3.client")
@Data
public class S3ClientConfig {
    /**
     * S3服务端点地址 (如: http://localhost:9000)
     */
    private String endpoint;

    /**
     * 访问密钥ID
     */
    private String accessKey;

    /**
     * 访问密钥
     */
    private String secretKey;

    /**
     * 区域
     */
    private String region = "us-east-1";

    /**
     * 存储桶名称
     */
    private String bucket;

    /**
     * 访问域名 (用于返回文件URL)
     */
    private String host;

    /**
     * 是否使用PathStyle访问 (MinIO/RustFS等兼容S3服务通常需要true)
     */
    private Boolean pathStyleAccess = true;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccess)
                .build();

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region))
                .serviceConfiguration(s3Config)
                .build();
    }
}
