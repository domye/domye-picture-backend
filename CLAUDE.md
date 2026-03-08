# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build entire project
mvn clean compile

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CommentAIReplyServiceTest

# Run specific test method
mvn test -Dtest=CommentAIReplyServiceTest#testGenerateReplySuccess

# Package without tests
mvn clean package -DskipTests

# Run application
mvn spring-boot:run -pl picture-api
```

## Architecture

Multi-module Maven project with Spring Boot 3.3.6 and Java 17.

### Module Dependency Graph

```
picture-api → picture-auth → picture-service → picture-model → picture-common
                                                          ↓
                                                    picture-bom (versions)
```

| Module | Purpose |
|--------|---------|
| picture-api | Controllers, config, main entry (`DomyePictureBackendApplication`) |
| picture-auth | Sa-Token auth, annotations (`@AuthCheck`, `@SaSpaceCheckPermission`) |
| picture-service | Service interfaces (`api/`), implementations (`impl/`), mappers |
| picture-model | Entities, DTOs, VOs, enums, MapStruct mappers |
| picture-common | Utilities, exceptions (`ErrorCode`, `Throw`), cache helpers |
| picture-bom | Centralized dependency version management |

### Key Patterns

**Service Interface Pattern**: Services defined as interfaces in `api/` package, implementations in `impl/` package. All services extend `IService<T>` from MyBatis-Plus.

**DTO/VO Pattern**:
- Entities: Database mapping (`picture-model/src/main/java/com/domye/picture/model/entity/`)
- DTOs: Request/response objects (`dto/` package)
- VOs: View objects with transformations (`vo/` package)
- Use MapStruct for object mapping (`picture-model/src/main/java/com/domye/picture/model/mapper/`)

**Multi-level Cache**: Redis (distributed) + Caffeine (local) with `CacheConsistencyHelper` for consistency.

**WebSocket + Disruptor**: Real-time picture editing uses high-performance Disruptor queue for event processing.

### Domain Structure

```
picture-model/src/main/java/com/domye/picture/model/
├── entity/          # Database entities
├── dto/             # Data Transfer Objects (requests)
│   ├── comment/
│   ├── contact/
│   ├── picture/
│   ├── space/
│   ├── user/
│   └── vote/
├── vo/              # View Objects (responses)
└── enums/           # Status and type enums
```

### Key Configurations

| File | Purpose |
|------|---------|
| `picture-api/src/main/resources/application.yml` | Main config |
| `picture-api/src/main/resources/application-local.yml` | Local environment |
| `.env.example` | Environment variables template |

## Coding Conventions

See `AGENTS.md` for detailed code templates and style guidelines. Key rules:

1. **Constructor injection only** - Use `@RequiredArgsConstructor`, never `@Autowired` on fields
2. **Error handling** - Use `Throw.throwIf(condition, ErrorCode)` for validation
3. **Lombok annotations** - `@Data`, `@Slf4j`, `@RequiredArgsConstructor` standard
4. **Entity IDs** - Use `@TableId(type = IdType.ASSIGN_ID)` for Snowflake IDs
5. **Trace ID** - Add `@MdcDot(bizCode = "#picture")` on controllers for request tracing
6. **Transactions** - Use `@Transactional` for multi-step database operations

## Tech Stack

- **Framework**: Spring Boot 3.3.6
- **ORM**: MyBatis-Plus 3.5.9
- **Database**: MySQL 8.0
- **Cache**: Redis + Redisson 3.51.0 + Caffeine 3.2.2
- **Auth**: Sa-Token 1.39.0
- **Storage**: Tencent Cloud COS
- **MQ**: RocketMQ 2.3.1
- **API Docs**: Knife4j 4.5.0
- **AI**: LangChain4j 0.36.2
- **Mapping**: MapStruct 1.6.3
- **Testing**: JUnit 5 + Mockito

## API Documentation

Access at `http://localhost:8123/api/doc.html` when running locally.