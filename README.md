# 多页图片协同平台

一个基于 Spring Boot 3 的云端摄影图片协同平台，支持实时协同编辑、AI 智能回复等功能。

## 功能特性

### 用户管理
- 用户注册、登录、权限控制
- 基于角色的访问控制(RBAC)
- 集成 Sa-Token 进行认证授权
- 微信公众号扫码登录

### 空间管理
- 支持创建个人空间和团队空间
- 空间容量和数量限制
- 空间权限管理

### 图片管理
- 图片上传、预览、删除
- 支持图片分类和标签
- 图片审核机制
- 按颜色搜索图片

### 实时协同
- 基于 WebSocket 的实时图片协同编辑
- 高性能 Disruptor 队列处理编辑事件
- 支持多用户同时编辑

### 社交功能
- 图片评论与回复
- 评论 @提及功能
- AI 智能回复（基于 LangChain4j）
- 信息流推送

### 存储系统
- 腾讯云 COS 对象存储
- 多级缓存：Redis（分布式）+ Caffeine（本地）
- 缓存一致性保障

### 数据分析
- 空间使用情况分析
- 图片分类统计
- 用户行为分析

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.3.6 |
| ORM | MyBatis-Plus | 3.5.9 |
| 数据库 | MySQL | 8.0 |
| 分布式缓存 | Redis + Redisson | 3.51.0 |
| 本地缓存 | Caffeine | 3.2.2 |
| 认证授权 | Sa-Token | 1.39.0 |
| 对象存储 | 腾讯云 COS | 5.6.227 |
| 消息队列 | RabbitMQ | 3.12+ |
| AI 框架 | LangChain4j | 0.36.2 |
| 对象映射 | MapStruct | 1.6.3 |
| 高性能队列 | Disruptor | 3.4.2 |
| API 文档 | Knife4j | 4.5.0 |
| 工具库 | Hutool | 5.8.26 |
| 测试 | JUnit 5 + Mockito | - |

## 项目架构

### 模块结构

```
domye-picture-backend/
├── picture-api/          # 控制器、配置、启动入口
├── picture-auth/         # Sa-Token 认证、权限注解
├── picture-service/      # 服务接口与实现、Mapper
├── picture-model/        # 实体、DTO、VO、枚举、MapStruct
├── picture-common/       # 工具类、异常、缓存辅助
├── picture-bom/          # 依赖版本管理
└── sql/                  # 数据库脚本
```

### 模块依赖关系

```
picture-api → picture-auth → picture-service → picture-model → picture-common
                                                          ↓
                                                    picture-bom (版本管理)
```

| 模块 | 职责 |
|------|------|
| picture-api | Controllers、配置类、主启动类 |
| picture-auth | Sa-Token 认证、`@AuthCheck`、`@SaSpaceCheckPermission` 注解 |
| picture-service | 服务接口 (`api/`)、实现 (`impl/`)、Mapper |
| picture-model | 实体、DTO、VO、枚举、MapStruct 映射器 |
| picture-common | 工具类、异常 (`ErrorCode`, `Throw`)、缓存辅助 |
| picture-bom | 集中管理第三方依赖版本 |

### 核心设计模式

**服务接口模式**: 服务定义为接口在 `api/` 包，实现在 `impl/` 包，继承 MyBatis-Plus `IService<T>`。

**DTO/VO 模式**:
- Entity: 数据库映射 (`entity/`)
- DTO: 请求对象 (`dto/`)
- VO: 响应对象 (`vo/`)
- MapStruct: 对象转换 (`mapper/`)

**多级缓存**: Redis（分布式）+ Caffeine（本地），通过 `CacheConsistencyHelper` 保证一致性。

**WebSocket + Disruptor**: 实时图片编辑使用高性能 Disruptor 队列处理事件。

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### 安装配置

1. **克隆项目**

```bash
git clone https://github.com/domye/domye-picture-backend.git
cd domye-picture-backend
```

2. **配置数据库**

```sql
-- 创建数据库
CREATE DATABASE domye_picture;

-- 导入表结构
mysql -u root -p domye_picture < sql/create_table.sql
```

3. **配置环境变量**

复制 `.env.example` 为 `.env` 并填写配置：

```bash
cp .env.example .env
```

4. **修改配置**

编辑 `picture-api/src/main/resources/application-local.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/domye_picture
    username: your_username
    password: your_password
```

5. **启动项目**

```bash
# 编译
mvn clean compile

# 运行
mvn spring-boot:run -pl picture-api
```

## 常用命令

```bash
# 编译整个项目
mvn clean compile

# 运行所有测试
mvn test

# 运行指定测试类
mvn test -Dtest=CommentAIReplyServiceTest

# 打包（跳过测试）
mvn clean package -DskipTests

# 运行应用
mvn spring-boot:run -pl picture-api
```

## API 文档

启动项目后访问：`http://localhost:8123/api/doc.html`

## 编码规范

- **依赖注入**: 仅使用构造器注入，使用 `@RequiredArgsConstructor`，禁止 `@Autowired` 字段注入
- **错误处理**: 使用 `Throw.throwIf(condition, ErrorCode)` 进行校验
- **Lombok 注解**: 标准使用 `@Data`, `@Slf4j`, `@RequiredArgsConstructor`
- **实体 ID**: 使用 `@TableId(type = IdType.ASSIGN_ID)` 生成雪花 ID
- **请求追踪**: 控制器添加 `@MdcDot(bizCode = "#picture")` 用于请求追踪
- **事务管理**: 多步骤数据库操作使用 `@Transactional`

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 联系方式

- 项目地址：https://github.com/domye/domye-picture-backend
- 问题反馈：https://github.com/domye/domye-picture-backend/issues

## 致谢

- [Spring Boot](https://spring.io/projects/spring-boot)
- [MyBatis-Plus](https://baomidou.com/)
- [Sa-Token](https://sa-token.dev33.cn/)
- [LangChain4j](https://docs.langchain4j.dev/)
- [Knife4j](https://doc.xiaominfo.com/)