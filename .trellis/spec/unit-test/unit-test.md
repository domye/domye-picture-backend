# 单元测试编写规范

## 测试原则

### 1. AAA 模式

```java
@Test
void testSaveUser_WhenUserIsValid_ShouldReturnUserId() {
    // Arrange (准备)
    UserCreateRequest request = new UserCreateRequest();
    request.setUsername("testuser");
    request.setPassword("password123");

    // Act (执行)
    Long userId = userService.saveUser(request);

    // Assert (断言)
    assertNotNull(userId);
    assertTrue(userId > 0);
}
```

### 2. 命名规范

```java
// 格式: 方法名_场景_预期结果
@Test
void findById_WhenIdExists_ShouldReturnUser() { }

@Test
void findById_WhenIdNotExists_ShouldThrowException() { }

@Test
void saveUser_WhenUsernameExists_ShouldThrowException() { }
```

### 3. 单一职责

每个测试只验证一个行为:

```java
// ✅ 好 - 每个测试验证一个行为
@Test
void testSaveUser_Success() { }

@Test
void testSaveUser_DuplicateUsername() { }

@Test
void testSaveUser_InvalidEmail() { }

// ❌ 差 - 一个测试验证多个行为
@Test
void testSaveUser_AllCases() {
    // 测试成功情况
    // 测试重复用户名
    // 测试无效邮箱
}
```

## 测试结构

### 1. 测试类组织

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    // 按功能分组
    @Nested
    class SaveUserTests {
        @Test
        void saveUser_WhenValid_ShouldSuccess() { }

        @Test
        void saveUser_WhenDuplicate_ShouldThrow() { }
    }

    @Nested
    class GetUserTests {
        @Test
        void getUser_WhenExists_ShouldReturn() { }

        @Test
        void getUser_WhenNotExists_ShouldThrow() { }
    }
}
```

### 2. 测试方法结构

```java
@Test
void testMethod() {
    // Given (准备测试数据)
    Long id = 1L;
    User expected = new User();
    expected.setId(id);
    expected.setUsername("test");

    when(userMapper.selectById(id)).thenReturn(expected);

    // When (执行测试方法)
    User actual = userService.getById(id);

    // Then (验证结果)
    assertNotNull(actual);
    assertEquals(id, actual.getId());
    assertEquals("test", actual.getUsername());

    // Verify (验证交互)
    verify(userMapper, times(1)).selectById(id);
}
```

## Mock 使用

### 1. Mock 对象

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderServiceImpl orderService;
}
```

### 2. Mock 行为

```java
// 返回值
when(userMapper.selectById(1L)).thenReturn(user);
when(userMapper.selectById(999L)).thenReturn(null);

// 抛异常
when(userMapper.selectById(any())).thenThrow(new RuntimeException());

// 无返回值
doNothing().when(cacheService).delete(any());

// 验证调用
verify(userMapper, times(1)).selectById(1L);
verify(userMapper, never()).selectById(999L);
```

### 3. 参数匹配

```java
// 任意参数
when(userMapper.selectById(any())).thenReturn(user);

// 指定类型
when(userMapper.selectById(anyLong())).thenReturn(user);

// 自定义匹配
when(userMapper.selectById(argThat(id -> id > 0))).thenReturn(user);
```

## 测试场景

### 1. 正常场景

```java
@Test
void saveUser_WhenValid_ShouldSuccess() {
    // Given
    UserCreateRequest request = createUserRequest();
    User expected = createUser();

    // When
    Long userId = userService.saveUser(request);

    // Then
    assertNotNull(userId);

    // Verify
    verify(userMapper, times(1)).insert(any(User.class));
}
```

### 2. 异常场景

```java
@Test
void saveUser_WhenDuplicateUsername_ShouldThrowException() {
    // Given
    UserCreateRequest request = createUserRequest();
    when(userMapper.selectByUsername(request.getUsername())).thenReturn(existingUser);

    // When & Then
    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> userService.saveUser(request)
    );

    assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
}
```

### 3. 边界场景

```java
@Test
void listUsers_WhenEmpty_ShouldReturnEmptyPage() {
    // Given
    UserQueryRequest request = new UserQueryRequest();
    when(userMapper.selectPage(any(), any())).thenReturn(new Page<>());

    // When
    Page<UserVO> result = userService.listUsers(request);

    // Then
    assertNotNull(result);
    assertEquals(0, result.getRecords().size());
}
```

## 数据准备

### 1. 测试数据工厂

```java
public class TestDataFactory {

    public static User createUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        return user;
    }

    public static UserCreateRequest createUserRequest() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setEmail("test@example.com");
        return request;
    }
}
```

### 2. 测试数据复用

```java
class UserServiceTest {

    private User testUser;
    private UserCreateRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser();
        testRequest = TestDataFactory.createUserRequest();
    }
}
```

## 测试覆盖率

### 1. 运行覆盖率报告

```bash
# 生成覆盖率报告
mvn jacoco:report

# 查看报告
open target/site/jacoco/index.html
```

### 2. 覆盖率要求

| 类型 | 最低覆盖率 |
|------|-----------|
| 工具类 | 90% |
| 服务层 | 80% |
| 控制器 | 70% |

### 3. 检查覆盖率

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## 运行测试

```bash
# 运行所有测试
mvn test

# 运行指定测试类
mvn test -Dtest=UserServiceTest

# 运行指定测试方法
mvn test -Dtest=UserServiceTest#testSaveUser

# 跳过测试
mvn package -DskipTests
```

---

*好的测试是代码质量的保障*