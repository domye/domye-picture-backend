# 后端开发规范索引

本目录包含 Spring Boot 后端开发的代码规范和模式。

## 规范文件

| 文件 | 说明 |
|------|------|
| `service-layer.md` | 服务层开发规范 |
| `controller.md` | 控制器开发规范 |
| `entity-dto-vo.md` | 实体、DTO、VO 规范 |
| `database.md` | 数据库操作规范 |
| `error-handling.md` | 错误处理规范 |
| `cache.md` | 缓存使用规范 |
| `security.md` | 安全规范 |

## 快速参考

### 项目架构
```
picture-api     → 控制器、配置
picture-auth    → 认证授权
picture-service → 服务接口和实现
picture-model   → 实体、DTO、VO
picture-common  → 工具类、异常
```

### 核心模式
- **服务接口模式**: 接口在 `api/`，实现在 `impl/`
- **DTO/VO 模式**: 请求用 DTO，响应用 VO
- **构造器注入**: 使用 `@RequiredArgsConstructor`
- **错误处理**: 使用 `Throw.throwIf(condition, ErrorCode)`

### 关键注解
```java
@Data                    // Lombok getter/setter
@Slf4j                   // 日志
@RequiredArgsConstructor  // 构造器注入
@TableId(type = IdType.ASSIGN_ID)  // 雪花ID
@Transactional           // 事务
@MdcDot(bizCode = "...") // 链路追踪
```

---

*详细规范请查看各具体文件*