package com.domye.picture.api.controller;

import cn.hutool.core.util.StrUtil;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.ai.ChatMessage;
import com.domye.picture.model.dto.ai.ChatSession;
import com.domye.picture.model.dto.ai.ChatStreamRequest;
import com.domye.picture.model.dto.ai.CreateSessionRequest;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.service.api.ai.AiChatService;
import com.domye.picture.service.api.ai.ChatHistoryService;
import com.domye.picture.service.api.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI 聊天控制器
 * 提供 SSE 流式聊天和会话管理接口
 */
@Slf4j
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final ChatHistoryService chatHistoryService;
    private final UserService userService;

    /**
     * SSE 流式聊天
     * 通过 Server-Sent Events 返回流式响应
     *
     * @param request 聊天请求
     * @param httpRequest HTTP 请求
     * @return SSE Emitter
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE 流式聊天", description = "通过 SSE 返回流式 AI 响应")
    public SseEmitter streamChat(
            @RequestBody ChatStreamRequest request,
            HttpServletRequest httpRequest) {

        // 参数校验
        Throw.throwIf(request == null || StrUtil.isBlank(request.getMessage()),
                ErrorCode.PARAMS_ERROR, "消息内容不能为空");

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(httpRequest);
        Throw.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        log.info("收到流式聊天请求: userId={}, sessionId={}", loginUser.getId(), request.getSessionId());

        return aiChatService.streamChat(loginUser.getId(), request);
    }

    /**
     * 创建新会话
     *
     * @param request 创建会话请求
     * @param httpRequest HTTP 请求
     * @return 新创建的会话
     */
    @PostMapping("/sessions")
    @Operation(summary = "创建新会话", description = "创建一个新的聊天会话")
    public BaseResponse<ChatSession> createSession(
            @RequestBody(required = false) CreateSessionRequest request,
            HttpServletRequest httpRequest) {

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(httpRequest);
        Throw.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        Long spaceId = request != null ? request.getSpaceId() : null;
        ChatSession session = aiChatService.createSession(loginUser.getId(), spaceId);

        log.info("创建会话成功: sessionId={}, userId={}", session.getId(), loginUser.getId());
        return Result.success(session);
    }

    /**
     * 获取用户会话列表
     *
     * @param httpRequest HTTP 请求
     * @return 会话列表
     */
    @GetMapping("/sessions")
    @Operation(summary = "获取用户会话列表", description = "获取当前用户的所有聊天会话")
    public BaseResponse<List<ChatSession>> getUserSessions(HttpServletRequest httpRequest) {

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(httpRequest);
        Throw.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        List<ChatSession> sessions = aiChatService.getUserSessions(loginUser.getId());
        return Result.success(sessions);
    }

    /**
     * 获取会话消息列表
     *
     * @param sessionId 会话 ID
     * @param httpRequest HTTP 请求
     * @return 消息列表
     */
    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "获取会话消息", description = "获取指定会话的所有聊天消息")
    public BaseResponse<List<ChatMessage>> getSessionMessages(
            @PathVariable String sessionId,
            HttpServletRequest httpRequest) {

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(httpRequest);
        Throw.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        // 验证权限
        Throw.throwIf(!chatHistoryService.hasAccess(sessionId, loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权访问该会话");

        List<ChatMessage> messages = chatHistoryService.getMessages(sessionId);
        return Result.success(messages);
    }

    /**
     * 删除会话
     *
     * @param sessionId 会话 ID
     * @param httpRequest HTTP 请求
     * @return 删除结果
     */
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "删除会话", description = "删除指定的聊天会话及其历史消息")
    public BaseResponse<Boolean> deleteSession(
            @PathVariable String sessionId,
            HttpServletRequest httpRequest) {

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(httpRequest);
        Throw.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        aiChatService.deleteSession(sessionId, loginUser.getId());

        log.info("删除会话成功: sessionId={}, userId={}", sessionId, loginUser.getId());
        return Result.success(true);
    }

    /**
     * 清除会话消息历史
     *
     * @param sessionId 会话 ID
     * @param httpRequest HTTP 请求
     * @return 清除结果
     */
    @DeleteMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "清除会话消息", description = "清除指定会话的历史消息，保留会话")
    public BaseResponse<Boolean> clearSessionMessages(
            @PathVariable String sessionId,
            HttpServletRequest httpRequest) {

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(httpRequest);
        Throw.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");

        // 验证权限
        Throw.throwIf(!chatHistoryService.hasAccess(sessionId, loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权访问该会话");

        chatHistoryService.clearSession(sessionId);

        log.info("清除会话消息成功: sessionId={}, userId={}", sessionId, loginUser.getId());
        return Result.success(true);
    }
}