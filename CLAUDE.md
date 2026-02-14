# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此仓库中工作时提供指引。

## 构建与运行

Maven 多模块 Spring Boot 2.7.6 项目（Java 8+）。

```bash
# 构建所有模块
mvn clean install

# 启动应用（端口 8123，上下文路径 /api）
mvn spring-boot:run -pl picture-api

# 构建单个模块
mvn clean install -pl picture-service

# 打包部署
mvn clean package -pl picture-api
```

API 文档地址：`http://localhost:8123/api/doc.html`（Knife4j/Swagger）。

当前项目无测试用例。

## 模块架构

六个模块，严格的依赖链：

```
picture-bom      → 第三方依赖版本管理（独立模块）
picture-common   → 异常处理、缓存、MDC 链路追踪、统一响应封装
picture-model    → 实体类、DTO、VO、枚举（依赖 common）
picture-service  → 服务接口/实现、MyBatis-Plus Mapper、辅助类（依赖 model）
picture-auth     → Sa-Token 认证、@AuthCheck/@SaSpaceCheckPermission 注解（依赖 service）
picture-api      → 控制器、Spring 配置、应用入口（依赖以上所有模块）
```

启动入口：`picture-api` 模块中的 `com.domye.picture.api.DomyePictureBackendApplication`。

## 核心模式

**分层架构** — 控制器（`picture-api/controller/`）→ 服务层（`picture-service/api/` 接口，`picture-service/impl/` 实现）→ Mapper 层（`picture-service/mapper/`，XML 在 `resources/mapper/`）。

**MyBatis-Plus 约定** — 服务接口继承 `IService<T>`，实现类继承 `ServiceImpl<Mapper, Entity>`。Mapper 继承 `BaseMapper<T>`。数据库字段使用 camelCase（非 snake_case），与 Java 字段直接对应。`map-underscore-to-camel-case` 已禁用。逻辑删除使用 `isDelete` 字段。

**统一响应** — 所有控制器返回 `BaseResponse<T>`，通过 `Result.success()` / `Result.error()` 构造。异常使用 `Throw.throwIf()` / `Throw.throwEx()` 配合 `ErrorCode` 枚举抛出，由 `GlobalExceptionHandler` 统一捕获。

**认证授权** — 两个自定义注解：`@AuthCheck` 用于角色校验，`@SaSpaceCheckPermission` 用于空间级权限校验，均通过 AOP 切面处理。多账号体系通过 `StpKit` 支持。

**缓存策略** — 两级缓存：Caffeine 本地缓存（5 分钟 TTL）+ Redis 分布式缓存（随机过期 300-600 秒，防止缓存雪崩）。分布式锁通过 Redisson（`LockService`）实现。

**分表** — `picture` 表支持通过 ShardingSphere 动态分片为 `picture_{spaceId}`，用于旗舰版团队空间。由 `picture-api/manager/sharding/DynamicShardingManager` 管理。

**WebSocket 协同编辑** — 图片编辑使用 WebSocket + LMAX Disruptor 高性能事件处理（`picture-service/helper/websocket/`）。

## 命名约定

- 实体类：`User`、`Picture`、`Space`（大驼峰，单数）
- DTO：`{实体}{动作}Request`（如 `PictureEditRequest`）
- VO：`{实体}VO`（如 `PictureVO`、`LoginUserVO`）
- 服务层：`{实体}Service` / `{实体}ServiceImpl`
- Mapper：`{实体}Mapper`
- 控制器：`{实体}Controller`
- 枚举：`{实体}{字段}Enum`（如 `UserRoleEnum`）
- 基础包名：`com.domye.picture`

## 基础设施依赖

需要 MySQL 8.0+、Redis（配置端口 63793）、腾讯云 COS 对象存储。Elasticsearch 用于搜索。Sentinel 负责限流，规则持久化到 `sentinel/` 目录。配置文件在 `picture-api/src/main/resources/`，`application.yml` 默认激活 `local` 配置。

## 数据库

DDL 脚本在 `sql/` 目录。数据库名 `domye_picture`，字符集 `utf8mb4_unicode_ci`。
