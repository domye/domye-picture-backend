package com.domye.picture.api.config.ai;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * PostgreSQL PgVector 向量存储配置
 * 用于 RAG (检索增强生成) 功能
 *
 * 注意：此数据源仅用于 PgVector，不影响主数据源 (MySQL)
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "postgres.vector")
public class PgVectorConfig {

    /**
     * PostgreSQL 数据库连接 URL
     */
    private String url;

    /**
     * 数据库用户名
     */
    private String username;

    /**
     * 数据库密码
     */
    private String password;

    /**
     * 表名前缀
     */
    private String tablePrefix = "picture";

    /**
     * 创建 PostgreSQL 数据源（用于向量存储）
     * 与 MySQL 主数据源并存，仅用于 PgVector
     */
    @Bean(name = "pgVectorDataSource")
    @ConditionalOnProperty(prefix = "postgres.vector", name = "url")
    public DataSource pgVectorDataSource() {
        log.info("初始化 PostgreSQL 向量数据源: {}", url);

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        return dataSource;
    }

    /**
     * 创建 PgVector EmbeddingStore
     * 用于存储和检索图片元数据的向量表示
     *
     * @param pgVectorDataSource PostgreSQL 数据源（用于向量存储）
     * @param embeddingModel     Embedding 模型
     * @return EmbeddingStore 实例
     */
    @Bean
    @ConditionalOnProperty(prefix = "postgres.vector", name = "url")
    public EmbeddingStore<TextSegment> embeddingStore(
            @Qualifier("pgVectorDataSource") DataSource pgVectorDataSource,
            EmbeddingModel embeddingModel) {

        // 获取向量维度
        int dimension = embeddingModel.dimension();
        log.info("初始化 PgVector EmbeddingStore, 表前缀: {}, 向量维度: {}", tablePrefix, dimension);

        // 使用 datasourceBuilder() 方法来传入 DataSource
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(pgVectorDataSource)
                .table(tablePrefix + "_embeddings")
                .dimension(dimension)
                // 使用 HNSW 索引，性能更好
                .indexListSize(100)
                .createTable(true)
                .build();
    }
}