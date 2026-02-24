package com.domye.picture.api.controller;

import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.ai.AiChatRequest;
import com.domye.picture.service.api.ai.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;

/**
 * AI 聊天接口
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController implements Serializable {

    private final AiChatService aiChatService;

    /**
     * AI 对话接口（支持多模态）
     *
     * @param aiChatRequest AI 聊天请求
     * @return AI 回复
     */
    @Operation(summary = "AI 对话接口（支持多模态）")
    @PostMapping("/chat")
    public BaseResponse<String> chat(@RequestBody AiChatRequest aiChatRequest) {
        Throw.throwIf(aiChatRequest == null || aiChatRequest.getMessage() == null, ErrorCode.PARAMS_ERROR, "消息不能为空");
        String response = aiChatService.chat(aiChatRequest.getMessage(), aiChatRequest.getImageUrls());
        return Result.success(response);
    }
}
