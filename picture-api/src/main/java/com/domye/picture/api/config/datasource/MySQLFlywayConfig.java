package com.domye.picture.api.config.datasource;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;

/**
 * MySQL Flyway 迁移配置
 * 由于项目使用自定义数据源配置，需要显式配置 Flyway
 *
 * 注意：主启动类已排除 FlywayAutoConfiguration，
 * 因为自动配置无法正确绑定到自定义的 mysqlDataSource Bean
 */
@Slf4j
@Configuration
public class MySQLFlywayConfig {

    @Value("${spring.flyway.enabled:true}")
    private boolean enabled;

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String locations;

    @Value("${spring.flyway.table:flyway_schema_history}")
    private String table;

    @Value("${spring.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    @Value("${spring.flyway.baseline-version:1.0.0}")
    private String baselineVersion;

    /**
     * MySQL Flyway 迁移
     * 使用 @DependsOn 确保数据源已创建
     *
     * @param mysqlDataSource MySQL 主数据源
     * @return Flyway 实例
     */
    @Bean(name = "mysqlFlyway", initMethod = "migrate")
    @DependsOn("mysqlDataSource")
    public Flyway mysqlFlyway(DataSource mysqlDataSource) {
        if (!enabled) {
            log.info("MySQL Flyway 已禁用，跳过迁移");
            return Flyway.configure()
                    .dataSource(mysqlDataSource)
                    .load();
        }

        log.info("初始化 MySQL Flyway 迁移, 脚本位置: {}", locations);

        return Flyway.configure()
                .dataSource(mysqlDataSource)
                // 迁移脚本位置
                .locations(locations)
                // 历史记录表名
                .table(table)
                // 基线迁移（用于已有数据库）
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                // 禁用校验（开发阶段脚本可能被修改）
                .validateOnMigrate(false)
                // 允许乱序执行（开发阶段方便）
                .outOfOrder(false)
                // 禁用清理（安全考虑）
                .cleanDisabled(true)
                .load();
    }
}