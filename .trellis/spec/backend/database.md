# 数据库操作规范

## Mapper 接口

Mapper 接口继承 `BaseMapper<T>`:

```java
public interface UserMapper extends BaseMapper<User> {
    // 自定义 SQL 方法
    @Select("SELECT * FROM user WHERE username = #{username}")
    User selectByUsername(@Param("username") String username);

    // 复杂查询使用 XML
    List<UserVO> selectUserVOList(@Param("request") UserQueryRequest request);
}
```

## 查询构建

### 1. 使用 QueryWrapper

```java
public Page<User> listUsers(UserQueryRequest request) {
    QueryWrapper<User> queryWrapper = new QueryWrapper<>();

    // 条件查询
    queryWrapper.eq(StrUtil.isNotBlank(request.getUsername()),
            "username", request.getUsername());
    queryWrapper.eq(request.getStatus() != null, "status", request.getStatus());

    // 排序
    queryWrapper.orderBy(StrUtil.isNotBlank(request.getSortField()),
            "asc".equalsIgnoreCase(request.getSortOrder()),
            request.getSortField());

    // 分页
    return this.page(new Page<>(request.getCurrent(), request.getPageSize()), queryWrapper);
}
```

### 2. 使用 LambdaQueryWrapper (推荐)

```java
public Page<User> listUsers(UserQueryRequest request) {
    LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

    queryWrapper.eq(StrUtil.isNotBlank(request.getUsername()),
            User::getUsername, request.getUsername());
    queryWrapper.eq(request.getStatus() != null, User::getStatus, request.getStatus());
    queryWrapper.orderByDesc(User::getCreateTime);

    return this.page(new Page<>(request.getCurrent(), request.getPageSize()), queryWrapper);
}
```

### 3. 条件拼接工具

```java
public Page<Picture> listPictures(PictureQueryRequest request) {
    LambdaQueryWrapper<Picture> wrapper = Wrappers.lambdaQuery(Picture.class)
            .eq(request.getSpaceId() != null, Picture::getSpaceId, request.getSpaceId())
            .eq(request.getReviewStatus() != null, Picture::getReviewStatus, request.getReviewStatus())
            .like(StrUtil.isNotBlank(request.getName()), Picture::getName, request.getName())
            .eq(Picture::getIsDelete, 0)
            .orderByDesc(Picture::getCreateTime);

    return this.page(new Page<>(request.getCurrent(), request.getPageSize()), wrapper);
}
```

## 批量操作

### 1. 批量插入

```java
// 使用 saveBatch
List<User> users = buildUserList(request);
this.saveBatch(users, 100); // 每批 100 条

// 或使用 foreach 插入 XML
userMapper.insertBatch(users);
```

### 2. 批量更新

```java
// 使用 updateBatchById
List<User> users = getUpdateList();
this.updateBatchById(users, 100);
```

### 3. 批量删除

```java
// 物理删除
this.removeByIds(ids);

// 逻辑删除 (自动处理)
this.removeByIds(ids);
```

## 关联查询

### 1. 简单关联

```java
// 查询图片并填充用户信息
public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage) {
    List<Picture> pictures = picturePage.getRecords();
    if (CollUtil.isEmpty(pictures)) {
        return new Page<>();
    }

    // 获取用户ID集合
    Set<Long> userIds = pictures.stream()
            .map(Picture::getUserId)
            .collect(Collectors.toSet());

    // 批量查询用户
    Map<Long, User> userMap = userService.listByIds(userIds)
            .stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

    // 组装 VO
    List<PictureVO> pictureVOList = pictures.stream()
            .map(picture -> {
                PictureVO vo = PictureStructMapper.INSTANCE.toVO(picture);
                User user = userMap.get(picture.getUserId());
                if (user != null) {
                    vo.setUserName(user.getUserName());
                    vo.setUserAvatar(user.getAvatarUrl());
                }
                return vo;
            })
            .collect(Collectors.toList());

    // 返回分页结果
    Page<PictureVO> voPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
    voPage.setRecords(pictureVOList);
    return voPage;
}
```

### 2. 复杂关联 (XML)

```xml
<!-- UserMapper.xml -->
<select id="selectUserWithStats" resultType="UserVO">
    SELECT
        u.*,
        (SELECT COUNT(*) FROM follow WHERE followed_user_id = u.id) as follower_count,
        (SELECT COUNT(*) FROM follow WHERE user_id = u.id) as following_count
    FROM user u
    WHERE u.id = #{userId}
</select>
```

## 事务管理

### 1. 注解事务

```java
@Service
public class OrderServiceImpl implements OrderService {

    @Transactional(rollbackFor = Exception.class)
    public void createOrder(OrderRequest request) {
        // 创建订单
        Order order = createOrderRecord(request);

        // 扣减库存
        deductInventory(request.getProductId(), request.getQuantity());

        // 创建支付记录
        createPaymentRecord(order.getId());
    }
}
```

### 2. 编程式事务

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final TransactionTemplate transactionTemplate;

    public void createOrderWithCallback(OrderRequest request) {
        transactionTemplate.execute(status -> {
            try {
                // 业务操作
                return doCreateOrder(request);
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
    }
}
```

## 性能优化

### 1. 避免 N+1 问题

```java
// ❌ N+1 问题
for (Picture picture : pictures) {
    User user = userService.getById(picture.getUserId()); // 每次查询
    // ...
}

// ✅ 批量查询
Set<Long> userIds = pictures.stream().map(Picture::getUserId).collect(Collectors.toSet());
Map<Long, User> userMap = userService.listByIds(userIds)
        .stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));
```

### 2. 分页查询

```java
// 使用分页避免大量数据
Page<Picture> page = this.page(
    new Page<>(current, size),
    queryWrapper
);
```

### 3. 索引优化

确保查询字段有索引:
- WHERE 条件字段
- JOIN 关联字段
- ORDER BY 排序字段

---

*遵循这些规范可以保证数据库操作的性能和正确性*