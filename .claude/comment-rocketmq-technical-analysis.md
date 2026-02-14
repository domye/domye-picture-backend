# 评论发送功能引入RocketMQ技术方案分析

## 一、现状分析

| 项目 | 现状 |
|------|------|
| **依赖** | `picture-service`模块已引入`rocketmq-spring-boot-starter:2.2.3` |
| **处理模式** | 同步处理，全部在一个事务中完成 |
| **流程** | 参数校验 → 验证图片 → 处理楼中楼 → 保存评论 → 保存内容 → 更新回复数 |
| **响应时间** | 包含多次数据库IO，高并发时可能成为瓶颈 |
| **扩展能力** | 缺乏消息通知、异步处理机制 |

### 1.1 现有代码结构

**核心文件**：
- Controller: `picture-api/controller/CommentController.java`
- Service接口: `picture-service/api/comment/CommentsService.java`
- Service实现: `picture-service/impl/comment/CommentsServiceImpl.java`
- 实体类: `picture-model/entity/comment/Comments.java`, `CommentsContent.java`
- DTO: `picture-model/dto/comment/CommentAddRequest.java`

**现有流程**（`CommentsServiceImpl.addComment`）：
```java
@Transactional(rollbackFor = Exception.class)
public Long addComment(CommentAddRequest request, Long userId, HttpServletRequest httpRequest) {
    // 1. 参数校验
    validateCommentRequest(request, userId);
    // 2. 验证图片存在
    Picture picture = pictureService.getById(pictureId);
    // 3. 处理楼中楼逻辑，获取根评论ID
    Long rootId = resolveRootId(parentId);
    // 4. 保存评论记录
    save(comment);
    // 5. 保存评论内容
    commentsContentService.save(content);
    // 6. 更新父评论回复数（如果是楼中楼）
    commentsMapper.incrementReplyCount(rootId);
    return comment.getCommentid();
}
```

---

## 二、RocketMQ应用场景分析

### 2.1 场景1：异步化评论发送（推荐优先级：高）

```
【现有同步流程】
用户请求 → 校验 → 写评论表 → 写内容表 → 更新计数 → 返回响应
                                            ↓
                                     用户等待时间较长

【异步化后】
用户请求 → 校验 → 发送MQ消息 → 立即返回（评论ID预生成）
                ↓
         消费者异步处理：写评论表 → 写内容表 → 更新计数 → 发送通知
```

**收益**：
- 接口响应时间从 ~50-100ms 降低到 ~5-10ms
- 提升系统吞吐量，应对突发流量

**风险**：
- 需要处理消息丢失、消费失败场景
- 用户可能短暂看不到刚发布的评论（最终一致性）

### 2.2 场景2：评论通知解耦（推荐优先级：高）

```
【新增功能：评论通知】
评论发送成功 → 发送MQ消息 → 消费者处理：
                              ├─ 通知被回复用户（WebSocket/站内信）
                              ├─ 通知图片作者
                              └─ 更新未读消息计数
```

**收益**：
- 核心逻辑与通知解耦，互不影响
- 支持多种通知渠道的灵活扩展

### 2.3 场景3：评论审核（推荐优先级：中）

```
评论发送 → 发送MQ消息 → 审核服务消费 → 内容审核 → 审核通过后写入DB
```

### 2.4 场景4：数据统计（推荐优先级：低）

```
评论发送 → 发送MQ消息 → 消费者更新：
                        ├─ 图片评论数统计
                        ├─ 用户评论活跃度
                        └─ 热门评论排行榜
```

---

## 三、技术方案对比

### 3.1 方案A：半异步模式（推荐）

**适用场景**：保证评论立即可见，仅异步处理扩展功能

```
Controller层（同步）：
  1. 参数校验
  2. 验证图片存在
  3. 保存评论到数据库（事务保证）
  4. 发送MQ消息（评论事件）
  5. 返回评论ID

消费者（异步）：
  ├─ 更新图片评论数计数
  ├─ 发送通知给被回复用户
  └─ 触发数据统计更新
```

**优点**：
- 评论立即可见，用户体验好
- 核心写入逻辑不受MQ影响，数据一致性有保障
- 扩展功能异步处理，不影响主流程性能

### 3.2 方案B：全异步模式

**适用场景**：极高并发场景，接受最终一致性

```
Controller层：
  1. 参数校验（可加本地缓存优化）
  2. 预生成评论ID（雪花算法）
  3. 发送MQ消息（包含完整评论数据）
  4. 立即返回评论ID

消费者：
  1. 写入评论表
  2. 写入内容表
  3. 更新计数
  4. 发送通知
```

**优点**：
- 响应时间最短
- 削峰能力最强

**缺点**：
- 用户可能短暂看不到评论
- 需要处理消息丢失、重复消费问题
- 事务复杂度增加

### 3.3 方案对比总结

| 维度 | 方案A（半异步） | 方案B（全异步） |
|------|----------------|----------------|
| 响应时间 | 中等（含DB写入） | 最短（仅MQ发送） |
| 数据一致性 | 强一致 | 最终一致 |
| 实现复杂度 | 低 | 高 |
| 用户体验 | 好（立即可见） | 一般（短暂延迟） |
| 削峰能力 | 中等 | 强 |
| 推荐场景 | 当前阶段推荐 | 极高并发场景 |

---

## 四、详细设计（推荐方案A）

### 4.1 消息定义

**位置**：`picture-model/src/main/java/com/domye/picture/model/dto/comment/CommentEventMessage.java`

```java
package com.domye.picture.model.dto.comment;

import lombok.Data;
import java.io.Serializable;

/**
 * 评论事件消息
 */
@Data
public class CommentEventMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    private Long commentId;

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 父评论ID（楼中楼）
     */
    private Long parentId;

    /**
     * 根评论ID
     */
    private Long rootId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 创建时间戳
     */
    private Long createdTime;

    /**
     * 事件类型
     */
    private EventType eventType;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        /** 创建评论 */
        CREATE,
        /** 删除评论 */
        DELETE,
        /** 点赞 */
        LIKE
    }
}
```

### 4.2 Topic规划

| Topic | 用途 | 消费组 | 说明 |
|-------|------|--------|------|
| `comment-event` | 评论事件 | `comment-notify-group` | 通知服务消费 |
| `comment-event` | 评论事件 | `comment-stat-group` | 统计服务消费 |

### 4.3 生产者实现

**位置**：`picture-service/src/main/java/com/domye/picture/service/helper/mq/CommentEventProducer.java`

```java
package com.domye.picture.service.helper.mq;

import com.domye.picture.model.dto.comment.CommentEventMessage;
import com.domye.picture.model.entity.comment.Comments;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

/**
 * 评论事件消息生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentEventProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * Topic名称
     */
    private static final String TOPIC_COMMENT_EVENT = "comment-event";

    /**
     * 发送评论创建事件
     *
     * @param comment 评论实体
     * @param content 评论内容
     */
    public void sendCommentCreatedEvent(Comments comment, String content) {
        CommentEventMessage message = buildMessage(comment, content, CommentEventMessage.EventType.CREATE);

        try {
            // 同步发送确保消息可靠性
            rocketMQTemplate.syncSend(TOPIC_COMMENT_EVENT, message);
            log.info("评论事件发送成功, commentId={}", comment.getCommentid());
        } catch (Exception e) {
            // 发送失败不影响主流程，记录日志即可
            log.error("评论事件发送失败, commentId={}", comment.getCommentid(), e);
        }
    }

    /**
     * 发送评论删除事件
     */
    public void sendCommentDeletedEvent(Comments comment) {
        CommentEventMessage message = buildMessage(comment, null, CommentEventMessage.EventType.DELETE);

        try {
            rocketMQTemplate.syncSend(TOPIC_COMMENT_EVENT, message);
            log.info("评论删除事件发送成功, commentId={}", comment.getCommentid());
        } catch (Exception e) {
            log.error("评论删除事件发送失败, commentId={}", comment.getCommentid(), e);
        }
    }

    private CommentEventMessage buildMessage(Comments comment, String content, CommentEventMessage.EventType eventType) {
        CommentEventMessage message = new CommentEventMessage();
        message.setCommentId(comment.getCommentid());
        message.setPictureId(comment.getPictureid());
        message.setUserId(comment.getUserid());
        message.setParentId(comment.getParentid());
        message.setRootId(comment.getRootid());
        message.setContent(content);
        message.setCreatedTime(System.currentTimeMillis());
        message.setEventType(eventType);
        return message;
    }
}
```

### 4.4 消费者实现（通知服务）

**位置**：`picture-service/src/main/java/com/domye/picture/service/helper/mq/CommentNotifyConsumer.java`

```java
package com.domye.picture.service.helper.mq;

import com.domye.picture.model.dto.comment.CommentEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 评论通知消费者
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "comment-event",
        consumerGroup = "comment-notify-group"
)
@RequiredArgsConstructor
public class CommentNotifyConsumer implements RocketMQListener<CommentEventMessage> {

    @Override
    public void onMessage(CommentEventMessage message) {
        log.info("收到评论事件消息: commentId={}, eventType={}",
                message.getCommentId(), message.getEventType());

        try {
            switch (message.getEventType()) {
                case CREATE:
                    handleCommentCreated(message);
                    break;
                case DELETE:
                    handleCommentDeleted(message);
                    break;
                default:
                    log.warn("未知事件类型: {}", message.getEventType());
            }
        } catch (Exception e) {
            log.error("处理评论事件失败, commentId={}", message.getCommentId(), e);
            // 抛出异常触发RocketMQ重试机制
            throw new RuntimeException("处理评论事件失败", e);
        }
    }

    /**
     * 处理评论创建事件
     */
    private void handleCommentCreated(CommentEventMessage message) {
        if (message.getParentId() != null) {
            // 楼中楼回复：通知被回复用户
            notifyReplyUser(message);
        } else {
            // 顶级评论：通知图片作者
            notifyPictureAuthor(message);
        }
    }

    /**
     * 处理评论删除事件
     */
    private void handleCommentDeleted(CommentEventMessage message) {
        // 可扩展：删除相关通知记录
        log.info("处理评论删除事件, commentId={}", message.getCommentId());
    }

    /**
     * 通知被回复用户
     */
    private void notifyReplyUser(CommentEventMessage message) {
        // TODO: 实现通知逻辑
        // 1. 查询被回复用户ID
        // 2. 发送站内信/推送通知
        // 3. 通过WebSocket实时推送
        log.info("通知被回复用户: parentId={}, commentId={}",
                message.getParentId(), message.getCommentId());
    }

    /**
     * 通知图片作者
     */
    private void notifyPictureAuthor(CommentEventMessage message) {
        // TODO: 实现通知逻辑
        log.info("通知图片作者: pictureId={}, commentId={}",
                message.getPictureId(), message.getCommentId());
    }
}
```

### 4.5 消费者实现（统计服务）

**位置**：`picture-service/src/main/java/com/domye/picture/service/helper/mq/CommentStatConsumer.java`

```java
package com.domye.picture.service.helper.mq;

import com.domye.picture.model.dto.comment.CommentEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 评论统计消费者
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "comment-event",
        consumerGroup = "comment-stat-group"
)
@RequiredArgsConstructor
public class CommentStatConsumer implements RocketMQListener<CommentEventMessage> {

    @Override
    public void onMessage(CommentEventMessage message) {
        log.info("收到评论统计消息: commentId={}", message.getCommentId());

        try {
            switch (message.getEventType()) {
                case CREATE:
                    updateCommentStats(message);
                    break;
                case DELETE:
                    decreaseCommentStats(message);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error("更新评论统计失败, commentId={}", message.getCommentId(), e);
            throw new RuntimeException("更新评论统计失败", e);
        }
    }

    /**
     * 更新评论统计（新增）
     */
    private void updateCommentStats(CommentEventMessage message) {
        // TODO: 实现统计逻辑
        // 1. 更新图片评论数
        // 2. 更新用户评论活跃度
        log.info("更新评论统计: pictureId={}, userId={}",
                message.getPictureId(), message.getUserId());
    }

    /**
     * 减少评论统计（删除）
     */
    private void decreaseCommentStats(CommentEventMessage message) {
        log.info("减少评论统计: pictureId={}", message.getPictureId());
    }
}
```

### 4.6 服务层改造

**修改文件**：`picture-service/src/main/java/com/domye/picture/service/impl/comment/CommentsServiceImpl.java`

```java
// 新增依赖注入
final CommentEventProducer commentEventProducer;

@Override
@Transactional(rollbackFor = Exception.class)
public Long addComment(CommentAddRequest request, Long userId, HttpServletRequest httpRequest) {
    // 1. 参数校验
    validateCommentRequest(request, userId);

    Long pictureId = request.getPictureid();
    Long parentId = request.getParentid();

    // 2. 验证图片存在
    Picture picture = pictureService.getById(pictureId);
    Throw.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

    // 3. 处理楼中楼逻辑，获取根评论ID
    Long rootId = resolveRootId(parentId);

    // 4. 保存评论记录
    Comments comment = Comments.builder()
            .pictureid(pictureId)
            .userid(userId)
            .parentid(parentId)
            .rootid(rootId)
            .replycount(0)
            .likecount(0)
            .build();
    save(comment);

    // 5. 保存评论内容
    CommentsContent content = new CommentsContent();
    content.setCommentId(comment.getCommentid());
    content.setCommentText(request.getContent());
    commentsContentService.save(content);

    // 6. 更新父评论回复数（如果是楼中楼）
    if (parentId != null) {
        commentsMapper.incrementReplyCount(rootId);
    }

    // 7. 【新增】发送MQ消息
    // 注意：发送失败不影响主流程，已在Producer中做异常捕获
    commentEventProducer.sendCommentCreatedEvent(comment, request.getContent());

    return comment.getCommentid();
}
```

### 4.7 事务消息优化（可选增强）

使用 Spring 事件机制确保事务提交后再发送消息：

**位置**：`picture-model/src/main/java/com/domye/picture/model/event/CommentCreatedEvent.java`

```java
package com.domye.picture.model.event;

import com.domye.picture.model.entity.comment.Comments;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CommentCreatedEvent extends ApplicationEvent {

    private final Comments comment;
    private final String content;

    public CommentCreatedEvent(Object source, Comments comment, String content) {
        super(source);
        this.comment = comment;
        this.content = content;
    }
}
```

**位置**：`picture-service/src/main/java/com/domye/picture/service/helper/mq/CommentEventListener.java`

```java
package com.domye.picture.service.helper.mq;

import com.domye.picture.model.event.CommentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 评论事件监听器
 * 确保数据库事务提交后再发送MQ消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentEventListener {

    private final CommentEventProducer commentEventProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {
        log.info("事务提交后发送评论事件, commentId={}", event.getComment().getCommentid());
        commentEventProducer.sendCommentCreatedEvent(event.getComment(), event.getContent());
    }
}
```

---

## 五、配置要求

### 5.1 application.yml 配置

```yaml
rocketmq:
  name-server: ${ROCKETMQ_NAME_SERVER:localhost:9876}
  producer:
    group: comment-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
    retry-times-when-send-async-failed: 2
```

### 5.2 环境变量

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `ROCKETMQ_NAME_SERVER` | RocketMQ NameServer地址 | `192.168.1.100:9876` |

---

## 六、实施计划

### 6.1 阶段划分

| 阶段 | 内容 | 预计工时 | 优先级 |
|------|------|----------|--------|
| **Phase 1** | 搭建RocketMQ环境，验证连通性 | 1天 | 必须 |
| **Phase 2** | 实现消息定义、生产者、消费者基础框架 | 1天 | 高 |
| **Phase 3** | 改造CommentsServiceImpl，集成MQ发送 | 0.5天 | 高 |
| **Phase 4** | 实现通知功能（站内信/WebSocket推送） | 2天 | 中 |
| **Phase 5** | 实现统计功能（评论数、活跃度） | 1天 | 中 |
| **Phase 6** | 压力测试、监控告警配置 | 1天 | 中 |

### 6.2 依赖检查清单

- [ ] RocketMQ环境已部署（NameServer + Broker）
- [ ] 项目依赖已引入（`rocketmq-spring-boot-starter:2.2.3` 已存在）
- [ ] 配置文件已添加RocketMQ连接信息
- [ ] 消息模型已定义
- [ ] 生产者/消费者已实现
- [ ] 单元测试覆盖
- [ ] 压力测试通过

---

## 七、风险与应对

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| **消息丢失** | 通知丢失，统计数据不准确 | 1. 使用同步发送<br>2. 使用`@TransactionalEventListener`确保事务后发送<br>3. 开启Broker同步刷盘 |
| **消费失败** | 通知未送达 | 1. 配置重试策略（默认16次）<br>2. 配置死信队列<br>3. 监控告警 |
| **消息重复** | 重复通知 | 1. 消费者幂等处理（基于commentId去重）<br>2. 使用Redis记录已处理消息 |
| **RocketMQ不可用** | MQ功能失效 | 1. Producer中捕获异常，不影响主流程<br>2. 降级策略：直接跳过通知功能 |
| **消息积压** | 通知延迟 | 1. 监控消费进度<br>2. 增加消费者实例<br>3. 优化消费逻辑 |

### 7.1 幂等性处理示例

```java
@Component
@RequiredArgsConstructor
public class CommentNotifyConsumer implements RocketMQListener<CommentEventMessage> {

    private final StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENT_KEY_PREFIX = "comment:notify:processed:";

    @Override
    public void onMessage(CommentEventMessage message) {
        String idempotentKey = IDEMPOTENT_KEY_PREFIX + message.getCommentId();

        // 幂等性检查
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", Duration.ofHours(24));

        if (Boolean.FALSE.equals(isNew)) {
            log.info("消息已处理，跳过: commentId={}", message.getCommentId());
            return;
        }

        // 处理消息...
        handleCommentCreated(message);
    }
}
```

---

## 八、监控与告警

### 8.1 关键指标

| 指标 | 说明 | 告警阈值 |
|------|------|----------|
| `rocketmq_producer_send_cost` | 消息发送耗时 | > 100ms |
| `rocketmq_producer_send_fail` | 消息发送失败数 | > 10/min |
| `rocketmq_consumer_lag` | 消费延迟 | > 1000 |
| `rocketmq_consumer_fail` | 消费失败数 | > 5/min |

### 8.2 日志规范

```java
// 生产者日志
log.info("评论事件发送成功, commentId={}, cost={}ms", commentId, cost);
log.error("评论事件发送失败, commentId={}", commentId, e);

// 消费者日志
log.info("收到评论事件消息: commentId={}, eventType={}", commentId, eventType);
log.error("处理评论事件失败, commentId={}", commentId, e);
```

---

## 九、总结

推荐采用**方案A（半异步模式）**，先在现有同步写入基础上增加MQ消息发送，用于异步处理通知、统计等扩展功能。

**核心收益**：
1. 评论主流程响应时间不受影响
2. 核心逻辑与扩展功能解耦
3. 为后续功能迭代打好基础（审核、更多通知渠道等）
4. 具备削峰能力，应对突发流量

**实施优先级**：
1. Phase 1-3（基础框架）立即启动
2. Phase 4-5（通知/统计）根据业务需求排期
3. Phase 6（监控）随功能上线同步完成
