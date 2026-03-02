# 服务层开发规范

## 服务接口定义

服务接口放在 `api/` 包下，继承 `IService<T>`:

```java
public interface UserService extends IService<User> {
    // 自定义方法
    User getLoginUser(HttpServletRequest request);
    Page<UserVO> listUser(UserQueryRequest request);
}
```

## 服务实现规范

服务实现放在 `impl/` 包下:

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    private final UserMapper userMapper;
    private final CacheConsistencyHelper cacheHelper;

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 实现逻辑
    }
}
```

## 方法设计原则

### 1. 单一职责
每个方法只做一件事:

```java
// ✅ 好 - 职责清晰
public void followUser(Long userId, Long targetUserId) {
    validateFollowRequest(userId, targetUserId);
    doFollow(userId, targetUserId);
    notifyFollow(userId, targetUserId);
}

// ❌ 差 - 做太多事
public void followUser(Long userId, Long targetUserId) {
    // 验证、关注、发通知、更新统计...全在一个方法里
}
```

### 2. 命名规范

| 操作类型 | 方法前缀 | 示例 |
|---------|---------|------|
| 查询单个 | get/find | `getUserById`, `findByEmail` |
| 查询列表 | list | `listUsers` |
| 查询分页 | page | `pageUsers` |
| 创建 | create/save | `createUser` |
| 更新 | update | `updateUser` |
| 删除 | delete/remove | `deleteUser` |
| 校验 | validate/check | `validateUser` |
| 判断 | is/has/can | `isActive`, `hasPermission` |

### 3. 参数校验

在方法入口校验参数:

```java
@Override
public void followUser(Long userId, Long targetUserId) {
    Throw.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
    Throw.throwIf(userId.equals(targetUserId), ErrorCode.PARAMS_ERROR, "不能关注自己");
    Throw.throwIf(!userExists(targetUserId), ErrorCode.NOT_FOUND_ERROR, "目标用户不存在");
}
```

### 4. 事务管理

多步操作使用 `@Transactional`:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void createOrder(OrderRequest request) {
    // 多步数据库操作
    Order order = createOrderRecord(request);
    deductInventory(request.getProductId(), request.getQuantity());
    createPaymentRecord(order.getId());
}
```

## 依赖注入

### 使用构造器注入 (推荐)

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final CacheService cacheService;
}
```

### 避免字段注入

```java
// ❌ 不推荐
@Autowired
private UserMapper userMapper;
```

## 异常处理

使用 `Throw` 工具类抛出业务异常:

```java
// 简单条件
Throw.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

// 带条件判断
Throw.throwIf(!user.isActive(), ErrorCode.OPERATION_ERROR, "用户已被禁用");

// 复杂条件
Throw.throwIf(
    user.getBalance().compareTo(amount) < 0,
    ErrorCode.OPERATION_ERROR,
    "余额不足"
);
```

## 日志规范

### 使用 Slf4j

```java
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    public void processUser(Long userId) {
        log.info("开始处理用户: userId={}", userId);

        try {
            // 业务逻辑
            log.debug("用户处理详情: {}", userDetails);
        } catch (Exception e) {
            log.error("处理用户失败: userId={}", userId, e);
            throw e;
        }

        log.info("用户处理完成: userId={}", userId);
    }
}
```

### 日志级别

| 级别 | 使用场景 |
|------|---------|
| ERROR | 异常、错误 |
| WARN | 警告、潜在问题 |
| INFO | 关键业务节点 |
| DEBUG | 调试信息 |

---

*遵循这些规范可以保证代码的一致性和可维护性*