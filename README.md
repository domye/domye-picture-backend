一个基于 Spring Boot 的云端摄影图片协同平台

## 🚀 功能特性

- **用户管理**
  - 用户注册、登录、权限控制
  - 基于角色的访问控制(RBAC)
  - 集成 Sa-Token 进行认证授权

- **空间管理**
  - 支持创建个人空间和团队空间
  - 空间容量和数量限制
  - 动态分表存储图片数据

- **图片管理**
  - 图片上传、预览、删除
  - 支持图片分类和标签
  - 图片审核机制
  - 按颜色搜索图片

- **存储系统**
  - 腾讯云 COS 对象存储
  - 本地缓存优化
  - Redis 缓存支持

- **数据分析**
  - 空间使用情况分析
  - 图片分类统计
  - 用户行为分析

## 🛠️ 技术栈

- **后端框架**: Spring Boot 2.7.6
- **数据库**: MySQL 8.0
- **ORM框架**: MyBatis-Plus 3.5.12
- **缓存**: Redis + Redisson
- **分库分表**: ShardingSphere 5.2.0
- **对象存储**: 腾讯云 COS
- **文档**: Knife4j (Swagger)
- **工具库**: Hutool、Lombok、Caffeine

## 📦 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### 安装配置

1. **克隆项目**
```bash
git clone https://github.com/yourusername/domye-picture-backend.git
cd domye-picture-backend
```

2. **配置数据库**
```sql
-- 创建数据库
CREATE DATABASE dmoye_picture;

-- 导入表结构
mysql -u root -p dmoye_picture < sql/create_table.sql
```

3. **修改配置**
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/dmoye_picture
    username: your_username
    password: your_password
```

4. **启动项目**
```bash
mvn spring-boot:run
```

## 📖 API文档

启动项目后访问：`http://localhost:8123/api/doc.html`

## 📁 项目结构

```
domye-picture-backend/
├── src/main/java/com/domye/picture/
│   ├── controller/     # 控制器层
│   ├── service/       # 服务层
│   ├── manager/       # 业务管理层
│   ├── mapper/        # 数据访问层
│   ├── model/         # 数据模型
│   ├── config/        # 配置类
│   └── utils/         # 工具类
├── src/main/resources/
│   ├── mapper/        # MyBatis映射文件
│   ├── application.yml # 应用配置
│   └── biz/          # 业务配置
└── sql/              # 数据库脚本
```

## 🤝 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系方式

- 项目地址：https://github.com/domye/domye-picture-backend
- 问题反馈：https://github.com/domye/domye-picture-backend/issues

## 🙏 致谢

- [Spring Boot](https://spring.io/projects/spring-boot)
- [MyBatis-Plus](https://baomidou.com/)
- [ShardingSphere](https://shardingsphere.apache.org/)
- [Sa-Token](https://sa-token.dev33.cn/)