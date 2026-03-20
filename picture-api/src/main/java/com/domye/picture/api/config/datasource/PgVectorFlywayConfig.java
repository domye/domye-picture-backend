package com.domye.picture.api.config.datasource;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * PostgreSQL PgVector Flyway 迁移配置
 * 用于向量存储相关表的版本化管理
 *
 * 注意：此配置独立于 MySQL 主数据源的 Flyway 自动配置
 */
@Slf4j
@Configuration
public class PgVectorFlywayConfig {

    /**
     * 配置 PostgreSQL 向量数据源的 Flyway 迁移
     * 仅在 postgres.vector.url 配置存在时启用
     *
     * @param pgVectorDataSource PostgreSQL 向量数据源
     * @return Flyway 实例
     */
    @Bean(name = "pgVectorFlyway", initMethod = "migrate")
    @ConditionalOnProperty(prefix = "postgres.vector", name = "url")
    public Flyway pgVectorFlyway(@Qualifier("pgVectorDataSource") DataSource pgVectorDataSource) {
        log.info("初始化 PostgreSQL 向量数据源 Flyway 迁移");

        return Flyway.configure()
                .dataSource(pgVectorDataSource)
                // 迁移脚本位置
                .locations("classpath:db/migration-pgvector")
                // 历史记录表名（与 MySQL 区分）
                .table("flyway_pgvector_history")
                // 基线迁移（用于已有数据库）
                .baselineOnMigrate(true)
                .baselineVersion("1.0.0")
                // 校验迁移脚本
                .validateOnMigrate(true)
                // 禁用清理（安全考虑）
                .cleanDisabled(true)
                .load();
    }
}