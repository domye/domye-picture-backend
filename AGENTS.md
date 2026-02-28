# AGENTS.md - Coding Guidelines for domye-picture-backend

## Build Commands

```bash
# Build the entire project
mvn clean compile

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=CommentAIReplyServiceTest

# Run a specific test method
mvn test -Dtest=CommentAIReplyServiceTest#testGenerateReplySuccess

# Package the application
mvn clean package -DskipTests

# Run the application
mvn spring-boot:run -pl picture-api
```

## Project Structure

Multi-module Maven project with Spring Boot 3.3.6:

- **picture-api**: REST controllers, configuration, main entry point
- **picture-service**: Service layer implementations, mappers, business logic
- **picture-model**: Entity classes, DTOs, VO, enums
- **picture-common**: Utilities, exceptions, constants, helpers
- **picture-auth**: Authentication/authorization with Sa-Token
- **picture-bom**: Dependency management (BOM)

## Technology Stack

- Java 17+ (required)
- Maven 3.6+
- Spring Boot 3.3.6
- MyBatis-Plus 3.5.12
- MySQL 8.0
- Redis + Redisson
- Sa-Token (auth)
- Lombok
- Hutool
- JUnit 5 + Mockito

## Code Style Guidelines

### Package Declaration
```java
package com.domye.picture.{module}.{subPackage};
```

### Import Order
1. `java.*` imports
2. Third-party libraries (Spring, Hutool, Lombok, etc.)
3. Internal project imports (com.domye.picture.*)

### Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Classes | PascalCase | `PictureService`, `UserVO` |
| Interfaces | PascalCase + Service suffix | `PictureService` |
| Implementations | PascalCase + Impl suffix | `PictureServiceImpl` |
| Methods | camelCase | `uploadPicture()`, `getById()` |
| Variables | camelCase | `pictureId`, `loginUser` |
| Constants | UPPER_SNAKE_CASE | `USER_LOGIN_STATE` |
| Enums | PascalCase + Enum suffix | `PictureReviewStatusEnum` |

### Class Templates

**Controller:**
```java
@RestController
@RequestMapping("/picture")
@MdcDot(bizCode = "#picture")
@RequiredArgsConstructor
public class PictureController {
    final PictureService pictureService;
    // Use constructor injection via @RequiredArgsConstructor
}
```

**Service Interface:**
```java
public interface PictureService extends IService<Picture> {
    PictureVO uploadPicture(...);
}
```

**Service Implementation:**
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    final PictureMapper pictureMapper;
    final UserService userService;
}
```

**Mapper:**
```java
public interface PictureMapper extends BaseMapper<Picture> {
    List<Picture> findAllByEditTimeAfter(@Param("minEditTime") Date minEditTime);
}
```

**Entity:**
```java
@TableName(value = "picture")
@Data
public class Picture implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
}
```

### Error Handling

Always use the custom exception utilities:

```java
// Throw exception with ErrorCode
Throw.throwIf(condition, ErrorCode.PARAMS_ERROR);
Throw.throwIf(condition, ErrorCode.NOT_FOUND_ERROR, "Custom message");

// Throw BusinessException directly
throw new BusinessException(ErrorCode.SYSTEM_ERROR);
```

Error codes (ErrorCode enum):
- SUCCESS(0)
- PARAMS_ERROR(40000)
- NOT_LOGIN_ERROR(40100)
- NO_AUTH_ERROR(40101)
- NOT_FOUND_ERROR(40400)
- FORBIDDEN_ERROR(40300)
- SYSTEM_ERROR(50000)
- OPERATION_ERROR(50001)

### API Response

```java
// Success response
return Result.success(data);

// Error response
return Result.error(ErrorCode.PARAMS_ERROR);
```

### Authentication/Authorization

```java
// Check login
@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)

// Check space permission
@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)

// Get current user
User loginUser = userService.getLoginUser(request);
```

### Testing

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    @Mock
    private DependencyService dependencyService;

    @BeforeEach
    void setUp() {
        myService = new MyServiceImpl(dependencyService);
    }

    @Test
    @DisplayName("描述测试场景")
    void testScenario() {
        // Given - setup mocks
        when(dependencyService.method()).thenReturn(value);

        // When - execute
        Result result = myService.doSomething();

        // Then - verify
        assertNotNull(result);
        verify(dependencyService).method();
    }
}
```

### Key Rules

1. **Never use field injection** - Use constructor injection with `@RequiredArgsConstructor`
2. **Always use Lombok** - `@Data`, `@Slf4j`, `@RequiredArgsConstructor`
3. **Use `Throw.throwIf()`** for validation instead of manual if-throw
4. **Entity IDs** - Use `@TableId(type = IdType.ASSIGN_ID)` for Snowflake IDs
5. **Serializability** - Entities must implement `Serializable`
6. **Documentation** - Add Javadoc for public methods in Chinese
7. **Trace ID** - Use `@MdcDot` on controllers for tracing
8. **Transactional** - Use `@Transactional` for multi-step DB operations
9. **Enum naming** - Status enums end with `Enum`, e.g., `PictureReviewStatusEnum`
