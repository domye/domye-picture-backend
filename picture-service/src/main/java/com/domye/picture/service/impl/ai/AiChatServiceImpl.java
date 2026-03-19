package com.domye.picture.service.impl.ai;

import cn.hutool.core.util.StrUtil;
import com.domye.picture.model.dto.ai.ChatMessage;
import com.domye.picture.model.dto.ai.ChatSession;
import com.domye.picture.model.dto.ai.ChatStreamRequest;
import com.domye.picture.service.api.ai.AiChatService;
import com.domye.picture.service.api.ai.ChatHistoryService;
import com.domye.picture.service.api.ai.RagService;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * AI 聊天服务实现
 * 负责处理流式聊天逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final StreamingChatLanguageModel streamingChatModel;
    private final RagService ragService;
    private final ChatHistoryService chatHistoryService;

    /**
     * 用于异步执行流式响应的线程池
     */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * SSE 超时时间（5分钟）
     */
    private static final long SSE_TIMEOUT = 5 * 60 * 1000L;

    /**
     * 默认最大检索结果数
     */
    private static final int DEFAULT_MAX_RESULTS = 5;

    @Override
    public SseEmitter streamChat(Long userId, ChatStreamRequest request) {
        // 参数校验
        Throw.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        Throw.throwIf(StrUtil.isBlank(request.getMessage()), ErrorCode.PARAMS_ERROR, "消息内容不能为空");

        // 创建 SSE Emitter
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 异步处理流式响应
        executor.execute(() -> {
            try {
                processStreamChat(userId, request, emitter);
            } catch (Exception e) {
                log.error("流式聊天处理失败: userId={}, error={}", userId, e.getMessage(), e);
                sendError(emitter, "处理请求时发生错误: " + e.getMessage());
            }
        });

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时: userId={}", userId);
            sendDone(emitter);
        });

        emitter.onCompletion(() -> {
            log.debug("SSE 连接完成: userId={}", userId);
        });

        emitter.onError(e -> {
            log.error("SSE 连接错误: userId={}, error={}", userId, e.getMessage());
        });

        return emitter;
    }

    /**
     * 处理流式聊天
     */
    private void processStreamChat(Long userId, ChatStreamRequest request, SseEmitter emitter) {
        String userMessage = request.getMessage();
        String sessionId = request.getSessionId();
        Long spaceId = request.getSpaceId();
        int maxResults = request.getMaxResults() != null ? request.getMaxResults() : DEFAULT_MAX_RESULTS;

        // 1. 处理会话
        ChatSession session;
        boolean isNewSession = false;
        if (StrUtil.isBlank(sessionId)) {
            // 创建新会话
            session = chatHistoryService.createSession(userId, spaceId);
            sessionId = session.getId();
            isNewSession = true;
            log.info("创建新会话: sessionId={}, userId={}", sessionId, userId);
        } else {
            // 验证会话权限
            Throw.throwIf(!chatHistoryService.hasAccess(sessionId, userId),
                    ErrorCode.NO_AUTH_ERROR, "无权访问该会话");
            session = chatHistoryService.getSession(sessionId);
            Throw.throwIf(session == null, ErrorCode.NOT_FOUND_ERROR, "会话不存在");
        }

        // 2. 保存用户消息
        ChatMessage userChatMessage = ChatMessage.userMessage(sessionId, userMessage);
        chatHistoryService.addMessage(sessionId, userChatMessage);

        // 3. RAG 检索并构建提示词
        String ragPrompt = ragService.retrieveAndBuildPrompt(userMessage, maxResults, spaceId);

        // 4. 构建对话历史
        StringBuilder conversationBuilder = new StringBuilder();
        List<ChatMessage> recentMessages = chatHistoryService.getRecentMessages(sessionId, 10);
        for (ChatMessage msg : recentMessages) {
            if ("user".equals(msg.getRole())) {
                conversationBuilder.append("用户: ").append(msg.getContent()).append("\n");
            } else if ("assistant".equals(msg.getRole())) {
                conversationBuilder.append("助手: ").append(msg.getContent()).append("\n");
            }
        }

        // 5. 组合完整提示词
        String fullPrompt = ragPrompt + "\n\n=== 对话历史 ===\n" + conversationBuilder.toString();

        // 6. 流式生成响应
        StringBuilder assistantResponse = new StringBuilder();
        final String finalSessionId = sessionId;
        final boolean finalIsNewSession = isNewSession;

        streamingChatModel.generate(fullPrompt, new StreamingResponseHandler<>() {
            @Override
            public void onNext(String token) {
                assistantResponse.append(token);
                sendContent(emitter, token);
            }

            @Override
            public void onComplete(Response response) {
                log.info("流式响应完成: sessionId={}, responseLength={}", finalSessionId, assistantResponse.length());

                // 保存助手消息
                ChatMessage assistantMessage = ChatMessage.assistantMessage(finalSessionId, assistantResponse.toString());
                chatHistoryService.addMessage(finalSessionId, assistantMessage);

                // 如果是新会话，更新标题
                if (finalIsNewSession) {
                    String title = generateTitle(userMessage);
                    chatHistoryService.updateSessionTitle(finalSessionId, title);
                }

                // 发送完成信号
                sendDone(emitter);
            }

            @Override
            public void onError(Throwable error) {
                log.error("流式生成错误: sessionId={}", finalSessionId, error);
                sendError(emitter, "AI 生成响应时发生错误");
            }
        });
    }

    @Override
    public ChatSession createSession(Long userId, Long spaceId) {
        Throw.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        return chatHistoryService.createSession(userId, spaceId);
    }

    @Override
    public List<ChatSession> getUserSessions(Long userId) {
        Throw.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        return chatHistoryService.getUserSessions(userId);
    }

    @Override
    public void deleteSession(String sessionId, Long userId) {
        Throw.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        Throw.throwIf(StrUtil.isBlank(sessionId), ErrorCode.PARAMS_ERROR, "会话 ID 不能为空");

        // 验证权限
        Throw.throwIf(!chatHistoryService.hasAccess(sessionId, userId),
                ErrorCode.NO_AUTH_ERROR, "无权删除该会话");

        chatHistoryService.deleteSession(sessionId);
        log.info("删除会话: sessionId={}, userId={}", sessionId, userId);
    }

    /**
     * 发送内容到 SSE
     */
    private void sendContent(SseEmitter emitter, String content) {
        try {
            String json = String.format("{\"type\":\"content\",\"content\":\"%s\"}",
                    escapeJson(content));
            emitter.send(SseEmitter.event().data(json));
        } catch (IOException e) {
            log.error("发送 SSE 内容失败: {}", e.getMessage());
        }
    }

    /**
     * 发送错误到 SSE
     */
    private void sendError(SseEmitter emitter, String message) {
        try {
            String json = String.format("{\"type\":\"error\",\"message\":\"%s\"}",
                    escapeJson(message));
            emitter.send(SseEmitter.event().data(json));
            sendDone(emitter);
        } catch (IOException e) {
            log.error("发送 SSE 错误失败: {}", e.getMessage());
        }
    }

    /**
     * 发送完成信号
     */
    private void sendDone(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().data("{\"type\":\"done\"}"));
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
        } catch (IOException e) {
            log.error("发送 SSE 完成信号失败: {}", e.getMessage());
        }
    }

    /**
     * 转义 JSON 字符串
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 根据用户消息生成会话标题
     */
    private String generateTitle(String userMessage) {
        if (StrUtil.isBlank(userMessage)) {
            return "新对话";
        }
        // 截取前20个字符作为标题
        int maxLength = Math.min(userMessage.length(), 20);
        String title = userMessage.substring(0, maxLength);
        if (userMessage.length() > 20) {
            title += "...";
        }
        return title;
    }

    /**
     * 销毁线程池
     */
    @PreDestroy
    public void destroy() {
        log.info("关闭 AI 聊天服务线程池...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}