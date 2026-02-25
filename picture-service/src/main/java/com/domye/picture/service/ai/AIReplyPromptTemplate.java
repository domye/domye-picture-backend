package com.domye.picture.service.ai;

import com.domye.picture.model.entity.picture.Picture;
import org.springframework.stereotype.Component;

/**
 * AI回复提示词模板
 */
@Component
public class AIReplyPromptTemplate {

    private static final String SYSTEM_PROMPT = """
            你是一个友好热情的社区助手，专门帮助用户讨论图片。
            请根据以下信息给出有帮助、友好的回复（控制在100字以内）：
            
            """;

    /**
     * 构建提示词
     *
     * @param picture 图片信息
     * @param userComment 用户评论内容
     * @return 完整的提示词
     */
    public String buildPrompt(Picture picture, String userComment) {
        StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT);
        
        // 添加图片信息
        prompt.append("图片标题：").append(getSafeValue(picture.getName())).append("\n");
        prompt.append("图片描述：").append(getSafeValue(picture.getIntroduction())).append("\n\n");
        
        // 添加用户评论
        prompt.append("用户评论：").append(userComment);
        
        return prompt.toString();
    }

    /**
     * 获取安全的字符串值（处理null情况）
     */
    private String getSafeValue(String value) {
        return value != null && !value.trim().isEmpty() ? value : "暂无";
    }
}
