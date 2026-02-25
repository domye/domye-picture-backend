package com.domye.picture.service.impl.ai;

import com.domye.picture.model.entity.picture.Picture;
import com.domye.picture.model.message.CommentAIReplyMessage;
import com.domye.picture.service.ai.AIReplyPromptTemplate;
import com.domye.picture.service.ai.CommentAIReplyService;
import com.domye.picture.service.api.picture.PictureService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI评论回复服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentAIReplyServiceImpl implements CommentAIReplyService {

    private final ChatLanguageModel chatLanguageModel;
    private final PictureService pictureService;
    private final AIReplyPromptTemplate promptTemplate;

    @Override
    public String generateReply(CommentAIReplyMessage message) {
        try {
            // 1. 查询图片信息
            Picture picture = pictureService.getById(message.getPictureId());
            if (picture == null) {
                log.warn("[AI回复] 图片不存在: pictureId={}", message.getPictureId());
                return null;
            }

            // 2. 组装提示词
            String prompt = promptTemplate.buildPrompt(picture, message.getContent());

            // 3. 调用AI模型生成回复
            log.info("[AI回复] 开始生成回复: commentId={}", message.getCommentId());
            String reply = chatLanguageModel.generate(prompt);
            
            log.info("[AI回复] 生成成功: commentId={}, reply={}", message.getCommentId(), reply);
            return reply;

        } catch (Exception e) {
            log.error("[AI回复] 生成失败: commentId={}, error={}", 
                    message.getCommentId(), e.getMessage(), e);
            return null;
        }
    }
}
