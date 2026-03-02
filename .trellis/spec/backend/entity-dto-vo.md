# 实体、DTO、VO 规范

## 实体 (Entity)

实体对应数据库表，放在 `picture-model/entity/` 下:

```java
@Data
@TableName("user")
public class User implements BaseEntity {
    /**
     * 主键ID (雪花算法)
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
}
```

### 实体规范

1. **主键**: 使用 `@TableId(type = IdType.ASSIGN_ID)` 生成雪花ID
2. **表名**: 使用 `@TableName` 指定表名
3. **逻辑删除**: 使用 `@TableLogic` 标记删除字段
4. **时间字段**: `createTime`, `updateTime` 使用 `Date` 类型
5. **注释**: 每个字段添加 JavaDoc 注释

## DTO (Data Transfer Object)

DTO 用于接收请求参数，放在 `picture-model/dto/` 下:

```java
@Data
public class UserQueryRequest implements BaseRequest {
    /**
     * 当前页码
     */
    private int current = 1;

    /**
     * 每页大小
     */
    private int pageSize = 10;

    /**
     * 用户名 (模糊搜索)
     */
    private String username;

    /**
     * 用户状态
     */
    private Integer status;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序
     */
    private String sortOrder;
}
```

### DTO 分类

| 类型 | 命名 | 用途 |
|------|------|------|
| 查询请求 | `XxxQueryRequest` | 列表查询参数 |
| 创建请求 | `XxxCreateRequest` | 创建操作参数 |
| 更新请求 | `XxxUpdateRequest` | 更新操作参数 |
| 操作请求 | `XxxRequest` | 通用操作参数 |

### DTO 校验

使用 JSR-303 注解进行校验:

```java
@Data
public class PictureCreateRequest {
    @NotBlank(message = "图片名称不能为空")
    private String name;

    @NotBlank(message = "图片URL不能为空")
    @URL(message = "图片URL格式不正确")
    private String url;

    @NotNull(message = "空间ID不能为空")
    private Long spaceId;

    @Size(max = 500, message = "简介不能超过500字")
    private String introduction;
}
```

## VO (View Object)

VO 用于返回响应数据，放在 `picture-model/vo/` 下:

```java
@Data
public class UserVO implements BaseVO {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 关注数
     */
    private Integer followingCount;

    /**
     * 粉丝数
     */
    private Integer followerCount;
}
```

### VO 规范

1. **敏感信息**: 不包含密码等敏感字段
2. **关联数据**: 可以包含关联实体的信息
3. **计算字段**: 可以包含统计、计算结果
4. **脱敏处理**: 对需要脱敏的字段进行处理

## 对象映射 (MapStruct)

使用 MapStruct 进行对象转换:

```java
@Mapper(componentModel = "spring")
public interface UserStructMapper {
    UserStructMapper INSTANCE = Mappers.getMapper(UserStructMapper.class);

    UserVO toVO(User user);

    List<UserVO> toVOList(List<User> users);

    @Mapping(target = "id", ignore = true)
    User toEntity(UserCreateRequest request);
}
```

### 使用示例

```java
// Entity -> VO
UserVO userVO = UserStructMapper.INSTANCE.toVO(user);

// List<Entity> -> List<VO>
List<UserVO> userVOList = UserStructMapper.INSTANCE.toVOList(users);

// DTO -> Entity
User user = UserStructMapper.INSTANCE.toEntity(createRequest);
```

## 包结构

```
picture-model/src/main/java/com/domye/picture/model/
├── entity/
│   ├── user/User.java
│   ├── picture/Picture.java
│   └── ...
├── dto/
│   ├── user/
│   │   ├── UserQueryRequest.java
│   │   ├── UserCreateRequest.java
│   │   └── UserUpdateRequest.java
│   ├── picture/
│   └── ...
├── vo/
│   ├── user/UserVO.java
│   ├── picture/PictureVO.java
│   └── ...
├── enums/
│   ├── UserStatusEnum.java
│   └── ...
└── mapper/
    ├── UserStructMapper.java
    └── ...
```

---

*遵循这些规范可以保证数据模型的一致性和可维护性*