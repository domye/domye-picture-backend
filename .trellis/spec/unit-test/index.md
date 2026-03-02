# 单元测试规范索引

本目录包含单元测试的编写规范。

## 测试框架

- **JUnit 5**: 测试框架
- **Mockito**: Mock 框架
- **Spring Boot Test**: 集成测试支持

## 测试规范

| 文件 | 说明 |
|------|------|
| `unit-test.md` | 单元测试编写规范 |
| `integration-test.md` | 集成测试规范 |
| `mock-guide.md` | Mock 使用指南 |

## 核心原则

### 1. 测试覆盖率
- 最低覆盖率: 80%
- 核心业务逻辑: 100%

### 2. 测试命名
```java
// 格式: 方法名_场景_预期结果
@Test
void saveUser_WhenUserIsValid_ShouldReturnUserId() {
    // ...
}
```

### 3. AAA 模式
```java
@Test
void testExample() {
    // Arrange - 准备测试数据
    // Act - 执行测试操作
    // Assert - 验证结果
}
```

## 运行测试

```bash
# 运行所有测试
mvn test

# 运行指定测试类
mvn test -Dtest=UserServiceTest

# 运行指定测试方法
mvn test -Dtest=UserServiceTest#testSaveUser
```

---

*详细规范请查看各具体文件*