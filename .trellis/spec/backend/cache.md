# 缓存使用规范

## 缓存架构

项目使用两级缓存:
- **Redis**: 分布式缓存
- **Caffeine**: 本地缓存

## 缓存注解

### 1. Spring Cache 注解

```java
// 查询缓存
@Cacheable(value = "user", key = "#id")
public User getUserById(Long id) {
    return userMapper.selectById(id);
}

// 更新缓存
@CachePut(value = "user", key = "#user.id")
public User updateUser(User user) {
    userMapper.updateById(user);
    return user;
}

// 删除缓存
@CacheEvict(value = "user", key = "#id")
public void deleteUser(Long id) {
    userMapper.deleteById(id);
}

// 删除所有缓存
@CacheEvict(value = "user", allEntries = true)
public void clearUserCache() {
    // 清除 user 缓存下的所有数据
}
```

### 2. 自定义缓存工具

使用 `CacheConsistencyHelper` 保证缓存一致性:

```java
@Service
@RequiredArgsConstructor
public class PictureServiceImpl implements PictureService {

    private final CacheConsistencyHelper cacheHelper;

    // 查询时使用缓存
    public PictureVO getPictureVO(Long id) {
        String cacheKey = "picture:vo:" + id;

        return cacheHelper.get(cacheKey, PictureVO.class, () -> {
            Picture picture = this.getById(id);
            Throw.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            return buildPictureVO(picture);
        });
    }

    // 更新时删除缓存
    public void updatePicture(PictureUpdateRequest request) {
        Picture picture = buildPicture(request);
        this.updateById(picture);

        // 删除缓存
        cacheHelper.delete("picture:vo:" + request.getId());
    }
}
```

## 缓存策略

### 1. Cache-Aside 模式 (推荐)

```java
// 读: 先读缓存，miss 再读数据库
public User getUser(Long id) {
    User user = redisTemplate.opsForValue().get("user:" + id);
    if (user != null) {
        return user;
    }

    user = userMapper.selectById(id);
    if (user != null) {
        redisTemplate.opsForValue().set("user:" + id, user, 30, TimeUnit.MINUTES);
    }
    return user;
}

// 写: 先更新数据库，再删除缓存
public void updateUser(User user) {
    userMapper.updateById(user);
    redisTemplate.delete("user:" + user.getId());
}
```

### 2. Write-Through 模式

```java
// 同时更新缓存和数据库
public void updateUser(User user) {
    userMapper.updateById(user);
    redisTemplate.opsForValue().set("user:" + user.getId(), user);
}
```

## 缓存 Key 设计

### 1. Key 命名规范

```
{业务模块}:{实体}:{ID}:{属性}

示例:
user:info:123
user:stats:123:followerCount
picture:vo:456
space:config:789
```

### 2. Key 工具类

```java
public class CacheKeyUtil {
    private static final String SEPARATOR = ":";

    public static String build(String... parts) {
        return String.join(SEPARATOR, parts);
    }

    // 示例: user:info:123
    public static String userInfo(Long userId) {
        return build("user", "info", String.valueOf(userId));
    }

    // 示例: picture:vo:456
    public static String pictureVO(Long pictureId) {
        return build("picture", "vo", String.valueOf(pictureId));
    }
}
```

## 缓存过期

### 1. 设置过期时间

```java
// 固定过期时间
redisTemplate.opsForValue().set(key, value, 30, TimeUnit.MINUTES);

// 随机过期时间 (防止缓存雪崩)
int randomExpire = 1800 + new Random().nextInt(600); // 30-40分钟
redisTemplate.opsForValue().set(key, value, randomExpire, TimeUnit.SECONDS);
```

### 2. 常见过期时间

| 数据类型 | 过期时间 | 说明 |
|---------|---------|------|
| 用户信息 | 30分钟 | 变更频率低 |
| 图片信息 | 1小时 | 变更频率低 |
| 统计数据 | 5分钟 | 变更频率高 |
| 验证码 | 5分钟 | 安全考虑 |
| Token | 7天 | 登录状态 |

## 缓存穿透防护

### 1. 空值缓存

```java
public User getUser(Long id) {
    String key = "user:" + id;

    // 检查空值标记
    if (redisTemplate.hasKey("null:" + key)) {
        return null;
    }

    User user = (User) redisTemplate.opsForValue().get(key);
    if (user != null) {
        return user;
    }

    user = userMapper.selectById(id);
    if (user == null) {
        // 缓存空值，防止穿透
        redisTemplate.opsForValue().set("null:" + key, "", 5, TimeUnit.MINUTES);
        return null;
    }

    redisTemplate.opsForValue().set(key, user, 30, TimeUnit.MINUTES);
    return user;
}
```

### 2. 布隆过滤器

```java
@Service
@RequiredArgsConstructor
public class PictureServiceImpl implements PictureService {

    private final RBloomFilter<String> pictureBloomFilter;

    public PictureVO getPicture(Long id) {
        // 先用布隆过滤器判断
        if (!pictureBloomFilter.contains("picture:" + id)) {
            return null;
        }

        // 正常缓存逻辑
        return getPictureFromCache(id);
    }
}
```

## 缓存击穿防护

### 1. 互斥锁

```java
public User getUserWithLock(Long id) {
    String key = "user:" + id;
    String lockKey = "lock:user:" + id;

    User user = (User) redisTemplate.opsForValue().get(key);
    if (user != null) {
        return user;
    }

    // 获取锁
    Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
    if (Boolean.TRUE.equals(acquired)) {
        try {
            user = userMapper.selectById(id);
            if (user != null) {
                redisTemplate.opsForValue().set(key, user, 30, TimeUnit.MINUTES);
            }
            return user;
        } finally {
            redisTemplate.delete(lockKey);
        }
    } else {
        // 等待并重试
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return getUserWithLock(id);
    }
}
```

## 最佳实践

### 1. 不要缓存频繁变更的数据

```java
// ❌ 不推荐: 点赞数实时变化
@Cacheable(value = "picture", key = "#id")
public PictureVO getPicture(Long id) {
    // 包含点赞数，会导致缓存频繁失效
}

// ✅ 推荐: 分离热点数据
public PictureVO getPicture(Long id) {
    PictureVO vo = getPictureBaseInfo(id);  // 缓存基本信息
    vo.setLikeCount(getLikeCount(id));      // 单独查询热点数据
    return vo;
}
```

### 2. 批量操作时注意缓存

```java
// 批量删除时清除缓存
public void deletePictures(List<Long> ids) {
    this.removeByIds(ids);

    // 清除缓存
    List<String> keys = ids.stream()
            .map(id -> "picture:vo:" + id)
            .collect(Collectors.toList());
    redisTemplate.delete(keys);
}
```

---

*合理的缓存使用可以大幅提升系统性能*