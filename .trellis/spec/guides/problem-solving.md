# 问题解决思路

## 系统化调试方法

### 1. 复现问题

```
明确问题现象:
- 发生了什么?
- 期望发生什么?
- 什么情况下发生?
```

### 2. 收集信息

```
- 查看错误日志
- 检查相关配置
- 确认环境信息
- 收集用户操作步骤
```

### 3. 定位原因

```
缩小范围:
- 是前端还是后端问题?
- 是哪个模块?
- 是哪段代码?

分析方法:
- 日志分析
- 断点调试
- 单元测试
- 网络抓包
```

### 4. 制定方案

```
- 确认根本原因
- 评估修复方案
- 考虑副作用
- 制定测试计划
```

### 5. 验证修复

```
- 本地验证
- 测试环境验证
- 编写回归测试
- 代码审查
```

## 常见问题模式

### 1. 空指针异常

```java
// 调试步骤
1. 定位具体哪行为空
2. 追溯对象来源
3. 确认为什么为空
4. 添加空值处理

// 预防措施
- 使用 Optional
- 添加空值检查
- 使用 @NonNull 注解
```

### 2. 数据不一致

```java
// 调试步骤
1. 确认数据源
2. 检查事务边界
3. 检查缓存一致性
4. 检查并发控制

// 预防措施
- 使用事务
- 使用分布式锁
- 使用缓存一致性工具
```

### 3. 性能问题

```java
// 调试步骤
1. 确认性能瓶颈
   - 数据库慢查询
   - 网络请求
   - 代码逻辑
2. 分析原因
3. 制定优化方案

// 工具
- 日志耗时统计
- 数据库执行计划
- JVM 性能分析
```

### 4. 内存问题

```java
// 调试步骤
1. 确认内存使用情况
2. 分析对象创建
3. 检查是否有内存泄漏

// 工具
- JVM 堆内存分析
- GC 日志分析
- 内存快照对比
```

## 日志分析技巧

### 1. 查找关键日志

```bash
# 错误日志
grep "ERROR" app.log

# 特定请求
grep "traceId=xxx" app.log

# 时间范围
grep "2024-01-01 10:" app.log
```

### 2. 分析调用链

```
1. 找到请求入口
2. 追踪 traceId
3. 分析调用链路
4. 定位异常位置
```

## 测试驱动调试

### 1. 编写失败的测试

```java
@Test
void testFollowUser_WhenTargetNotExists_ShouldThrowException() {
    // Arrange
    Long userId = 1L;
    Long targetId = 999L; // 不存在

    // Act & Assert
    assertThrows(BusinessException.class, () -> {
        followService.followUser(userId, targetId);
    });
}
```

### 2. 修复使测试通过

```java
public void followUser(Long userId, Long targetUserId) {
    // 添加缺失的校验
    User targetUser = userService.getById(targetUserId);
    Throw.throwIf(targetUser == null, ErrorCode.NOT_FOUND_ERROR, "目标用户不存在");

    // 原有逻辑...
}
```

### 3. 确认修复正确

```bash
mvn test -Dtest=FollowServiceTest#testFollowUser_WhenTargetNotExists_ShouldThrowException
```

## 求助指南

### 1. 整理问题

```markdown
## 问题描述
[简述问题]

## 复现步骤
1. ...
2. ...
3. ...

## 期望结果
[期望的行为]

## 实际结果
[实际发生的情况]

## 已尝试的方法
- 方法1: 结果
- 方法2: 结果

## 相关信息
- 环境: [开发/测试/生产]
- 版本: [版本号]
- 日志: [关键日志片段]
```

### 2. 提问技巧

- 先描述问题，再说尝试过什么
- 提供完整的上下文
- 使用清晰的格式
- 标注关键信息

---

*系统的方法比随机尝试更高效*