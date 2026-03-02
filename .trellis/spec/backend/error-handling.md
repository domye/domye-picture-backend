# 错误处理规范

## 错误码定义

错误码定义在 `picture-common` 的 `ErrorCode` 枚举中:

```java
public enum ErrorCode {
    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败");

    private final int code;
    private final String message;
}
```

## 异常抛出

使用 `Throw` 工具类统一抛出异常:

```java
// 基本用法
Throw.throwIf(condition, ErrorCode.PARAMS_ERROR);

// 自定义消息
Throw.throwIf(condition, ErrorCode.PARAMS_ERROR, "用户名不能为空");

// 复杂条件
Throw.throwIf(
    user.getBalance().compareTo(amount) < 0,
    ErrorCode.OPERATION_ERROR,
    "余额不足，当前余额: " + user.getBalance()
);
```

## 常用校验场景

### 1. 参数校验

```java
// 空值校验
Throw.throwIf(StrUtil.isBlank(username), ErrorCode.PARAMS_ERROR, "用户名不能为空");
Throw.throwIf(CollUtil.isEmpty(ids), ErrorCode.PARAMS_ERROR, "ID列表不能为空");

// 范围校验
Throw.throwIf(pageSize > 100, ErrorCode.PARAMS_ERROR, "每页最多100条");

// 格式校验
Throw.throwIf(!EmailValidator.isValid(email), ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
```

### 2. 存在性校验

```java
// 数据不存在
User user = userMapper.selectById(userId);
Throw.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

// 关联数据不存在
Picture picture = pictureMapper.selectById(pictureId);
Throw.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
```

### 3. 权限校验

```java
// 无权限
Throw.throwIf(!userId.equals(picture.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权操作此图片");

// 非管理员
Throw.throwIf(!user.isAdmin(), ErrorCode.NO_AUTH_ERROR, "仅管理员可操作");
```

### 4. 状态校验

```java
// 状态不允许
Throw.throwIf(space.getStatus() != SpaceStatusEnum.NORMAL.getValue(),
    ErrorCode.OPERATION_ERROR, "空间已禁用");

// 已存在
Throw.throwIf(followMapper.exists(userId, targetUserId),
    ErrorCode.OPERATION_ERROR, "已关注该用户");
```

## 全局异常处理

异常由全局异常处理器统一处理:

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException: {}", e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException: {}", e.getMessage(), e);
        return Result.error(ErrorCode.SYSTEM_ERROR);
    }
}
```

## 错误响应格式

所有错误响应使用统一格式:

```json
{
    "code": 40000,
    "data": null,
    "message": "请求参数错误"
}
```

## 最佳实践

### 1. 错误信息要具体

```java
// ❌ 不推荐
Throw.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

// ✅ 推荐
Throw.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户ID: " + userId + " 不存在");
```

### 2. 按顺序校验

```java
// 先校验基础参数，再校验业务逻辑
public void transfer(Long fromId, Long toId, BigDecimal amount) {
    // 1. 基础参数校验
    Throw.throwIf(fromId == null || toId == null, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
    Throw.throwIf(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0,
        ErrorCode.PARAMS_ERROR, "转账金额必须大于0");

    // 2. 业务校验
    User fromUser = userMapper.selectById(fromId);
    Throw.throwIf(fromUser == null, ErrorCode.NOT_FOUND_ERROR, "转出用户不存在");

    User toUser = userMapper.selectById(toId);
    Throw.throwIf(toUser == null, ErrorCode.NOT_FOUND_ERROR, "转入用户不存在");

    // 3. 状态校验
    Throw.throwIf(fromUser.getBalance().compareTo(amount) < 0,
        ErrorCode.OPERATION_ERROR, "余额不足");
}
```

### 3. 记录日志

```java
try {
    // 业务逻辑
} catch (Exception e) {
    log.error("操作失败: userId={}, error={}", userId, e.getMessage(), e);
    throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作失败: " + e.getMessage());
}
```

---

*统一的错误处理可以提高系统的可维护性和用户体验*