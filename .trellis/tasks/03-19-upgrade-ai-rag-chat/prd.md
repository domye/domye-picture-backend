# 升级AI功能为真正的RAG + 添加在线聊天助手页面

## Goal

将现有的简单AI评论回复功能升级为真正的RAG（检索增强生成）系统，基于PostgreSQL 16 + pgvector，同时在前端增加在线AI聊天助手页面，实现图片元数据知识库的智能问答。

## Requirements

### 后端需求

**基础架构**
- [ ] PostgreSQL pgvector扩展安装与配置
- [ ] 添加LangChain4j pgvector依赖
- [ ] PostgreSQL数据源配置（新增，与MySQL并存）

**RAG核心功能**
- [ ] Embedding模型配置（OpenAI text-embedding-3-small, 1536维度）
- [ ] PgVectorEmbeddingStore向量存储配置
- [ ] 图片元数据索引服务（图片上传/更新时自动建立向量索引）
- [ ] 图片元数据删除时清理向量索引
- [ ] RAG检索增强生成服务

**聊天API**
- [ ] SSE流式聊天接口 `/ai/chat/stream`
- [ ] 聊天历史管理（Redis缓存 + 可选数据库持久化）
- [ ] 会话管理接口（创建/获取/删除会话）

### 前端需求

**独立聊天页面**
- [ ] AI聊天页面路由 `/ai-chat`
- [ ] 聊天UI组件（消息列表、输入框、发送按钮）
- [ ] SSE流式消息显示（打字机效果）
- [ ] 会话列表侧边栏
- [ ] Markdown渲染支持

**侧边栏悬浮窗口**
- [ ] 右下角悬浮按钮
- [ ] 弹出式聊天窗口组件
- [ ] 与独立页面共享聊天状态

## Acceptance Criteria

- [ ] 用户可以向AI提问图片相关问题（如"找一张风景图"、"有哪些猫的图片"），AI基于RAG给出准确回答
- [ ] 图片上传后自动建立向量索引（响应时间 < 2秒）
- [ ] 图片更新/删除后向量索引同步更新/删除
- [ ] 聊天页面支持SSE流式响应，首字节响应 < 1秒
- [ ] 支持多轮对话，上下文关联正确
- [ ] 刷新页面后可恢复近期聊天历史（Redis缓存）
- [ ] 侧边栏悬浮窗口与独立页面功能一致

## Definition of Done

- [ ] 单元测试覆盖率 > 80%
- [ ] 集成测试通过
- [ ] Lint/TypeCheck检查通过
- [ ] API文档更新
- [ ] 前端组件可复用

## Out of Scope

- MySQL到PostgreSQL的数据迁移（本次只做AI功能，PostgreSQL仅用于向量存储）
- 原有评论AI回复功能的修改
- 图片内容的视觉理解（仅基于元数据）
- 用户文档上传（PDF、Word等）

## Decision (ADR-lite)

### 数据源
**Context**: 需要确定RAG知识库的数据范围
**Decision**: 仅使用图片元数据（name、category、tags、introduction）
**Consequences**: 实现简单，无需处理文档解析；检索范围限定在图片相关问答

### 响应方式
**Context**: 需要确定聊天API的响应方式
**Decision**: SSE流式响应
**Consequences**: 用户体验好，首字节响应快；需要前端支持SSE处理

### Embedding模型
**Context**: 需要选择向量嵌入模型
**Decision**: OpenAI text-embedding-3-small（1536维度）
**Consequences**: 与现有GPT模型生态一致，成本较低；需要调用OpenAI API

### 聊天历史存储
**Context**: 需要确定聊天历史的存储策略
**Decision**: Redis缓存 + 可选数据库持久化
**Consequences**: 近期对话快速访问；历史可归档；需要设计缓存过期策略

### 前端UI
**Context**: 需要确定聊天界面的集成方式
**Decision**: 独立页面 + 侧边栏悬浮窗口
**Consequences**: 用户可选择全屏聊天或快捷聊天；组件可复用

## Technical Approach

### 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend (Vue 3)                      │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │  AI Chat Page    │  │  Floating Widget │                │
│  │  (/ai-chat)      │  │  (右下角悬浮)     │                │
│  └────────┬─────────┘  └────────┬─────────┘                │
│           │    SSE             │                           │
│           └────────┬───────────┘                           │
└────────────────────┼───────────────────────────────────────┘
                     │
┌────────────────────┼───────────────────────────────────────┐
│              Backend (Spring Boot)                          │
│  ┌─────────────────▼─────────────────┐                     │
│  │      AI ChatController            │                     │
│  │  POST /ai/chat/stream (SSE)       │                     │
│  └─────────────────┬─────────────────┘                     │
│                    │                                        │
│  ┌─────────────────▼─────────────────┐                     │
│  │      RAG Service                  │                     │
│  │  1. Embedding Question            │                     │
│  │  2. Vector Search (PgVector)      │                     │
│  │  3. Build Context                 │                     │
│  │  4. Stream Generate (LLM)         │                     │
│  └─────────────────┬─────────────────┘                     │
│                    │                                        │
│  ┌─────────────────┴─────────────────┐                     │
│  │                                   │                     │
│  ▼                                   ▼                     │
│ PgVector Store              Chat Memory (Redis)            │
│ (PostgreSQL)                          │                     │
└─────────────────────────────────────────────────────────────┘
```

### 核心数据结构

**向量存储表 (picture_embeddings)**
```sql
CREATE TABLE picture_embeddings (
    id SERIAL PRIMARY KEY,
    picture_id BIGINT NOT NULL,
    content TEXT NOT NULL,           -- 元数据文本
    embedding vector(1536),          -- 1536维向量
    metadata JSONB,                  -- 额外元数据
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX ON picture_embeddings USING ivfflat (embedding vector_cosine_ops);
```

**Redis聊天历史**
```
Key: chat:session:{sessionId}:messages
Value: List<Message>
TTL: 7 days
```

### 关键代码结构

```
picture-service/
├── ai/
│   ├── rag/
│   │   ├── RagService.java              # RAG主服务
│   │   ├── PictureEmbeddingService.java # 图片向量化服务
│   │   └── EmbeddingConfig.java         # Embedding配置
│   └── chat/
│       ├── ChatService.java             # 聊天服务
│       ├── ChatHistoryService.java      # 历史管理
│       └── SseEmitterService.java       # SSE流式处理
picture-model/
├── dto/ai/
│   ├── ChatRequest.java
│   ├── ChatMessage.java
│   └── ChatSession.java
picture-api/
├── controller/
│   └── AiChatController.java            # SSE聊天接口
```

## Technical Notes

### 依赖变更
```xml
<!-- LangChain4j PgVector -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-pgvector</artifactId>
    <version>0.36.2</version>
</dependency>
<!-- PostgreSQL Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

### 配置变更 (application.yml)
```yaml
# PostgreSQL for Vector Store
spring:
  datasource:
    postgres:
      url: jdbc:postgresql://localhost:5432/domye_picture_vector
      username: ${PG_USERNAME:}
      password: ${PG_PASSWORD:}

# AI Embedding Config
ai:
  embedding:
    model: text-embedding-3-small
    dimension: 1536
```

### 实现计划（小PR）

**PR1: 后端基础设施**
- PostgreSQL数据源配置
- PgVector依赖和EmbeddingStore配置
- Embedding模型配置

**PR2: RAG核心功能**
- 图片元数据索引服务
- RAG检索服务
- 聊天历史管理（Redis）

**PR3: API与流式响应**
- SSE聊天接口
- 会话管理接口

**PR4: 前端聊天页面**
- 独立聊天页面
- SSE消息处理
- 侧边栏悬浮组件

### 参考资料
- [LangChain4j PgVector官方文档](https://docs.langchain4j.dev/integrations/embedding-stores/pgvector/)
- [RAG Chatbot with Spring Boot Example](https://github.com/mnhnam/rag-ai-chatbot-with-langchain4j)
- [LangChain4j Streaming](https://docs.langchain4j.dev/tutorials/chat-and-language-models#streaming)