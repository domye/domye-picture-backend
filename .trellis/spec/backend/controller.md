# 控制器开发规范

## 控制器定义

控制器放在 `picture-api` 模块的 `controller` 包下:

```java
@RestController
@RequestMapping("/user")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
}
```

## API 设计规范

### 1. RESTful 风格

| 操作 | HTTP 方法 | URL | 方法名 |
|------|----------|-----|--------|
| 查询列表 | GET | `/users` | `listUsers` |
| 查询详情 | GET | `/users/{id}` | `getUser` |
| 创建 | POST | `/users` | `createUser` |
| 更新 | PUT | `/users/{id}` | `updateUser` |
| 删除 | DELETE | `/users/{id}` | `deleteUser` |

### 2. 响应格式

统一使用 `BaseResponse<T>` 包装响应:

```java
@GetMapping("/{id}")
public BaseResponse<UserVO> getUser(@PathVariable Long id) {
    UserVO user = userService.getUserById(id);
    return Result.success(user);
}

@GetMapping("/list")
public BaseResponse<Page<UserVO>> listUsers(UserQueryRequest request) {
    Page<UserVO> page = userService.listUsers(request);
    return Result.success(page);
}
```

### 3. 链路追踪

使用 `@MdcDot` 注解添加链路追踪:

```java
@GetMapping("/{id}")
@MdcDot(bizCode = "getUser")
public BaseResponse<UserVO> getUser(@PathVariable Long id) {
    // ...
}
```

## 参数接收

### 1. 路径参数

```java
@GetMapping("/{id}")
public BaseResponse<UserVO> getUser(@PathVariable Long id) {
    // ...
}
```

### 2. 查询参数

```java
@GetMapping("/list")
public BaseResponse<Page<UserVO>> listUsers(
        @RequestParam(defaultValue = "1") int current,
        @RequestParam(defaultValue = "10") int size) {
    // ...
}
```

### 3. 请求体

```java
@PostMapping
public BaseResponse<Long> createUser(@RequestBody @Valid UserCreateRequest request) {
    Long userId = userService.createUser(request);
    return Result.success(userId);
}
```

### 4. 表单数据

```java
@PostMapping("/login")
public BaseResponse<UserVO> login(
        @RequestParam String username,
        @RequestParam String password) {
    // ...
}
```

## 权限控制

### 1. 登录校验

使用 Sa-Token 的 `StpUtil`:

```java
@PostMapping
public BaseResponse<Long> createPicture(@RequestBody PictureRequest request) {
    // 校验登录
    StpUtil.checkLogin();
    Long userId = StpUtil.getLoginIdAsLong();
    // ...
}
```

### 2. 权限注解

```java
// 需要特定权限
@SaCheckPermission("picture:delete")
@DeleteMapping("/{id}")
public BaseResponse<Void> deletePicture(@PathVariable Long id) {
    // ...
}

// 需要特定角色
@SaCheckRole("admin")
@GetMapping("/admin/stats")
public BaseResponse<StatsVO> getStats() {
    // ...
}
```

## 参数校验

### 1. 使用 @Valid

```java
@PostMapping
public BaseResponse<Long> createUser(
        @RequestBody @Valid UserCreateRequest request) {
    // ...
}
```

### 2. DTO 中定义校验规则

```java
@Data
public class UserCreateRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度2-20")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少6位")
    private String password;

    @Email(message = "邮箱格式不正确")
    private String email;
}
```

## 分页处理

统一使用 MyBatis-Plus 的 `Page<T>`:

```java
@GetMapping("/list")
public BaseResponse<Page<UserVO>> listUsers(
        @RequestParam(defaultValue = "1") int current,
        @RequestParam(defaultValue = "10") int size,
        HttpServletRequest request) {
    UserQueryRequest query = new UserQueryRequest();
    query.setCurrent(current);
    query.setPageSize(size);

    Page<UserVO> page = userService.listUsers(query);
    return Result.success(page);
}
```

## 文件上传

```java
@PostMapping("/upload")
public BaseResponse<String> uploadFile(@RequestParam("file") MultipartFile file) {
    Throw.throwIf(file.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");

    String url = fileService.upload(file);
    return Result.success(url);
}
```

## 异常处理

控制器不处理异常，统一由全局异常处理器处理:

```java
// 控制器只管抛出异常
@GetMapping("/{id}")
public BaseResponse<UserVO> getUser(@PathVariable Long id) {
    UserVO user = userService.getUserById(id);
    Throw.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
    return Result.success(user);
}
```

---

*遵循这些规范可以保证 API 的一致性和易用性*