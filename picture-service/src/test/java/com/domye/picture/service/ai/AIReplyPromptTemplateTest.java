package com.domye.picture.service.ai;

import com.domye.picture.model.entity.picture.Picture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI回复提示词模板测试
 */
class AIReplyPromptTemplateTest {

    private AIReplyPromptTemplate promptTemplate;

    @BeforeEach
    void setUp() {
        promptTemplate = new AIReplyPromptTemplate();
    }

    @Test
    @DisplayName("测试构建完整提示词")
    void testBuildPrompt() {
        // Given
        Picture picture = new Picture();
        picture.setName("美丽的日落");
        picture.setIntroduction("夕阳西下的美丽景色");

        String userComment = "@AI助手 这张图片真美！";

        // When
        String prompt = promptTemplate.buildPrompt(picture, userComment);

        // Then
        assertNotNull(prompt);
        assertTrue(prompt.contains("美丽的日落"));
        assertTrue(prompt.contains("夕阳西下的美丽景色"));
        assertTrue(prompt.contains("@AI助手 这张图片真美！"));
        assertTrue(prompt.contains("社区助手"));
    }

    @Test
    @DisplayName("测试图片无描述时使用默认值")
    void testBuildPromptWithNullIntroduction() {
        // Given
        Picture picture = new Picture();
        picture.setName("测试图片");
        picture.setIntroduction(null);

        String userComment = "测试评论";

        // When
        String prompt = promptTemplate.buildPrompt(picture, userComment);

        // Then
        assertNotNull(prompt);
        assertTrue(prompt.contains("测试图片"));
        assertTrue(prompt.contains("暂无"));
    }

    @Test
    @DisplayName("测试图片名称和描述都为空")
    void testBuildPromptWithEmptyValues() {
        // Given
        Picture picture = new Picture();
        picture.setName("");
        picture.setIntroduction("");

        String userComment = "测试评论";

        // When
        String prompt = promptTemplate.buildPrompt(picture, userComment);

        // Then
        assertNotNull(prompt);
        assertTrue(prompt.contains("暂无"));
    }
}
