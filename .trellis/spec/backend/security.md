# 安全规范

## 认证与授权

### 1. Sa-Token 配置

项目使用 Sa-Token 进行认证授权:

```java
// 登录
@PostMapping("/login")
public BaseResponse<UserVO> login(@RequestBody LoginRequest request) {
    User user = userService.login(request.getUsername(), request.getPassword());
    StpUtil.login(user.getId());  // 登录
    return Result.success(userService.getUserVO(user));
}

// 登出
@PostMapping("/logout")
public BaseResponse<Void> logout() {
    StpUtil.logout();  // 登出
    return Result.success(null);
}

// 获取当前登录用户
public User getLoginUser(HttpServletRequest request) {
    Long userId = StpUtil.getLoginIdAsLong();
    return userService.getById(userId);
}
```

### 2. 权限检查

```java
// 检查登录状态
StpUtil.checkLogin();

// 检查权限
@SaCheckPermission("picture:delete")
public void deletePicture(Long id) { }

// 检查角色
@SaCheckRole("admin")
public void adminOperation() { }

// 自定义权限检查
@AuthCheck(mustRole = "admin")
public void adminOnly() { }
```

### 3. 空间权限

```java
@SaSpaceCheckPermission(value = "picture:edit")
public void editPicture(Long spaceId, Long pictureId) { }
```

## 输入验证

### 1. 参数校验

```java
@Data
public class UserCreateRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度2-20")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字、下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度8-32")
    private String password;

    @Email(message = "邮箱格式不正确")
    private String email;
}
```

### 2. XSS 防护

```java
// 使用 Hutool 的 XSS 过滤
public String sanitizeInput(String input) {
    return HtmlUtil.cleanHtmlTag(input);
}

// 存储前转义
public void saveComment(String content) {
    String safeContent = HtmlUtil.escape(content);
    comment.setContent(safeContent);
    commentMapper.insert(comment);
}
```

### 3. SQL 注入防护

```java
// ✅ 使用参数化查询 (自动防护)
@Select("SELECT * FROM user WHERE username = #{username}")
User selectByUsername(@Param("username") String username);

// ✅ 使用 QueryWrapper (自动防护)
QueryWrapper<User> wrapper = new QueryWrapper<>();
wrapper.eq("username", username);

// ❌ 字符串拼接 (危险)
String sql = "SELECT * FROM user WHERE username = '" + username + "'";
```

## 敏感信息处理

### 1. 密码加密

```java
// 注册时加密
public void register(UserCreateRequest request) {
    String encryptedPassword = PasswordEncoder.encode(request.getPassword());
    user.setPassword(encryptedPassword);
    userMapper.insert(user);
}

// 登录时验证
public User login(String username, String password) {
    User user = userMapper.selectByUsername(username);
    Throw.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
    Throw.throwIf(!PasswordEncoder.matches(password, user.getPassword()),
        ErrorCode.PARAMS_ERROR, "密码错误");
    return user;
}
```

### 2. 敏感字段脱敏

```java
@Data
public class UserVO {
    private Long id;
    private String username;

    // 手机号脱敏
    @JsonSerialize(using = PhoneDesensitizeSerializer.class)
    private String phone;

    // 邮箱脱敏
    @JsonSerialize(using = EmailDesensitizeSerializer.class)
    private String email;

    // 不返回密码
    // private String password;
}
```

### 3. 日志脱敏

```java
// 不要在日志中输出敏感信息
log.info("用户登录: userId={}", userId);  // ✅

log.info("用户登录: password={}", password);  // ❌
log.info("用户登录: {}", user);  // ❌ 可能包含敏感信息
```

## API 安全

### 1. 接口限流

```java
// 使用注解限流
@RateLimit(key = "login", time = 60, count = 5)
@PostMapping("/login")
public BaseResponse<UserVO> login(@RequestBody LoginRequest request) {
    // ...
}

// 或使用 Redis 限流
public void checkRateLimit(String key, int limit, int period) {
    String fullKey = "rate_limit:" + key;
    Long count = redisTemplate.opsForValue().increment(fullKey);
    if (count == 1) {
        redisTemplate.expire(fullKey, period, TimeUnit.SECONDS);
    }
    Throw.throwIf(count > limit, ErrorCode.OPERATION_ERROR, "操作过于频繁");
}
```

### 2. 接口签名

```java
// 验证请求签名
public void verifySignature(HttpServletRequest request) {
    String timestamp = request.getHeader("X-Timestamp");
    String sign = request.getHeader("X-Sign");

    // 检查时间戳 (防重放)
    long now = System.currentTimeMillis();
    Throw.throwIf(Math.abs(now - Long.parseLong(timestamp)) > 300000,
        ErrorCode.OPERATION_ERROR, "请求已过期");

    // 验证签名
    String expectedSign = generateSign(request);
    Throw.throwIf(!expectedSign.equals(sign), ErrorCode.OPERATION_ERROR, "签名无效");
}
```

### 3. CORS 配置

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

## 文件上传安全

### 1. 文件类型校验

```java
public void validateFile(MultipartFile file) {
    // 检查文件大小
    Throw.throwIf(file.getSize() > 10 * 1024 * 1024, ErrorCode.PARAMS_ERROR, "文件不能超过10MB");

    // 检查文件类型 (通过魔数)
    byte[] bytes = file.getBytes();
    String fileType = detectFileType(bytes);
    Throw.throwIf(!ALLOWED_TYPES.contains(fileType), ErrorCode.PARAMS_ERROR, "不支持的文件类型");

    // 检查扩展名
    String extension = FileUtil.getSuffix(file.getOriginalFilename());
    Throw.throwIf(!ALLOWED_EXTENSIONS.contains(extension), ErrorCode.PARAMS_ERROR, "不支持的文件扩展名");
}
```

### 2. 文件名处理

```java
// 生成安全的文件名
public String generateSafeFileName(String originalName) {
    String extension = FileUtil.getSuffix(originalName);
    String uuid = IdUtil.simpleUUID();
    return uuid + "." + extension;
}

// 不要使用用户提供的文件名
String safeName = generateSafeFileName(file.getOriginalFilename());
```

## 安全检查清单

### 开发时检查

- [ ] 所有用户输入都经过校验
- [ ] 敏感操作都有权限检查
- [ ] SQL 使用参数化查询
- [ ] 密码等敏感信息已加密
- [ ] 响应不包含敏感字段
- [ ] 日志不输出敏感信息

### 部署前检查

- [ ] 关闭调试模式
- [ ] 配置 HTTPS
- [ ] 配置安全响应头
- [ ] 配置 CORS 白名单
- [ ] 配置接口限流
- [ ] 配置错误页面

---

*安全是系统稳定运行的基础*