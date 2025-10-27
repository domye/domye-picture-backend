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
     * 创建Elasticsearch客户端
     */
    @Bean(name = "restHighLevelClient")
    public RestHighLevelClient restHighLevelClient() {
        try {
            // 此处为单节点es
            String host = hosts.split(":")[0];
            String port = hosts.split(":")[1];
            HttpHost httpHost = new HttpHost(host, Integer.parseInt(port), scheme);

            // 设置用户名、密码
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(userName, password));

            // 构建连接对象
            RestClientBuilder builder = RestClient.builder(httpHost)
                    .setHttpClientConfigCallback(httpClientBuilder -> {
                        httpClientBuilder.setMaxConnTotal(maxConnectNum);
                        httpClientBuilder.setMaxConnPerRoute(maxConnectNumPerRoute);
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                        return httpClientBuilder;
                    })
                    .setRequestConfigCallback(requestConfigBuilder -> {
                        requestConfigBuilder.setConnectTimeout(connectTimeOut);
                        requestConfigBuilder.setSocketTimeout(socketTimeOut);
                        requestConfigBuilder.setConnectionRequestTimeout(connectionRequestTimeOut);
                        return requestConfigBuilder;
                    });
            log.info("成功");
            // 创建高级客户端
            return new RestHighLevelClient(builder);
        } catch (Exception e) {
            log.error("Elasticsearch客户端创建失败", e);
            throw new RuntimeException("Elasticsearch客户端创建失败", e);
        }
    }
}