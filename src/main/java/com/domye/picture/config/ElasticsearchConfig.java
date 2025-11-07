package com.domye.picture.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * es配置类
 * @author ygl
 * @since 2023-05-25
 **/
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "elasticsearch")
@ConditionalOnProperty(name = "elasticsearch.open", havingValue = "true")
public class ElasticsearchConfig {

    // 是否开启ES
    private Boolean open;

    // es host ip 地址（集群）
    private String hosts;

    // es用户名
    private String userName;

    // es密码
    private String password;

    // es 请求方式
    private String scheme;

    // es集群名称
    private String clusterName;

    // es 连接超时时间
    private int connectTimeOut;

    // es socket 连接超时时间
    private int socketTimeOut;

    // es 请求超时时间
    private int connectionRequestTimeOut;

    // es 最大连接数
    private int maxConnectNum;

    // es 每个路由的最大连接数
    private int maxConnectNumPerRoute;


    /**
     * 如果@Bean没有指定bean的名称，那么这个bean的名称就是方法名
     */
    @Bean(name = "restHighLevelClient")
    public RestHighLevelClient restHighLevelClient() {
        // 检查是否开启ES
        if (open == null || !open) {
            log.warn("Elasticsearch is disabled, skipping RestHighLevelClient creation");
            return null;
        }
        
        // 检查必要的配置参数
        if (hosts == null || hosts.trim().isEmpty()) {
            log.error("Elasticsearch hosts configuration is missing");
            return null;
        }

        try {
            // 此处为单节点es
            String host = hosts.split(":")[0];
            String port = hosts.split(":")[1];
            HttpHost httpHost = new HttpHost(host, Integer.parseInt(port));

            // 构建连接对象
            RestClientBuilder builder = RestClient.builder(httpHost);

            // 设置用户名、密码（如果配置了）
            if (userName != null && password != null) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));
                
                // 连接数配置
                builder.setHttpClientConfigCallback(httpClientBuilder -> {
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    if (maxConnectNum > 0) {
                        httpClientBuilder.setMaxConnTotal(maxConnectNum);
                    }
                    if (maxConnectNumPerRoute > 0) {
                        httpClientBuilder.setMaxConnPerRoute(maxConnectNumPerRoute);
                    }
                    return httpClientBuilder;
                });
            } else {
                // 没有配置认证信息
                builder.setHttpClientConfigCallback(httpClientBuilder -> {
                    if (maxConnectNum > 0) {
                        httpClientBuilder.setMaxConnTotal(maxConnectNum);
                    }
                    if (maxConnectNumPerRoute > 0) {
                        httpClientBuilder.setMaxConnPerRoute(maxConnectNumPerRoute);
                    }
                    return httpClientBuilder;
                });
            }

            // 连接延时配置（如果配置了）
            builder.setRequestConfigCallback(requestConfigBuilder -> {
                if (connectTimeOut > 0) {
                    requestConfigBuilder.setConnectTimeout(connectTimeOut);
                }
                if (socketTimeOut > 0) {
                    requestConfigBuilder.setSocketTimeout(socketTimeOut);
                }
                if (connectionRequestTimeOut > 0) {
                    requestConfigBuilder.setConnectionRequestTimeout(connectionRequestTimeOut);
                }
                return requestConfigBuilder;
            });

            return new RestHighLevelClient(builder);
        } catch (Exception e) {
            log.error("Failed to create RestHighLevelClient: {}", e.getMessage(), e);
            return null;
        }
    }
}