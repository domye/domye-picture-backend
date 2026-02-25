package com.domye.picture.service.ai;

import com.domye.picture.model.entity.picture.Picture;
import com.domye.picture.model.message.CommentAIReplyMessage;
import com.domye.picture.service.impl.ai.CommentAIReplyServiceImpl;
import com.domye.picture.service.api.picture.PictureService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AI评论回复服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class CommentAIReplyServiceTest {

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @Mock
    private PictureService pictureService;

    private AIReplyPromptTemplate promptTemplate;
    private CommentAIReplyServiceImpl commentAIReplyService;

    @BeforeEach
    void setUp() {
        promptTemplate = new AIReplyPromptTemplate();
        commentAIReplyService = new CommentAIReplyServiceImpl(chatLanguageModel, pictureService, promptTemplate);
    }

    @Test
    @DisplayName("测试AI回复生成成功")
    void testGenerateReplySuccess() {
        // Given
        Long pictureId = 1L;
        Long commentId = 100L;
        String userComment = "@AI助手 这张图片真好看！";

        Picture picture = new Picture();
        picture.setId(pictureId);
        picture.setName("美丽的风景");
        picture.setIntroduction("一张美丽的风景照片");

        CommentAIReplyMessage message = CommentAIReplyMessage.builder()
                .commentId(commentId)
                .pictureId(pictureId)
                .content(userComment)
                .userId(1L)
                .createTime(new Date())
                .build();

        String aiResponse = "谢谢您的赞美！这张风景确实很美呢～";

        when(pictureService.getById(pictureId)).thenReturn(picture);
        when(chatLanguageModel.generate(any(String.class))).thenReturn(aiResponse);

        // When
        String result = commentAIReplyService.generateReply(message);

        // Then
        assertNotNull(result);
        assertEquals(aiResponse, result);
        verify(pictureService).getById(pictureId);
        verify(chatLanguageModel).generate(any(String.class));
    }

    @Test
    @DisplayName("测试图片不存在时返回null")
    void testGenerateReplyPictureNotFound() {
        // Given
        Long pictureId = 999L;
        CommentAIReplyMessage message = CommentAIReplyMessage.builder()
                .commentId(100L)
                .pictureId(pictureId)
                .content("@AI助手 测试")
                .userId(1L)
                .createTime(new Date())
                .build();

        when(pictureService.getById(pictureId)).thenReturn(null);

        // When
        String result = commentAIReplyService.generateReply(message);

        // Then
        assertNull(result);
        verify(pictureService).getById(pictureId);
        verify(chatLanguageModel, never()).generate(any(String.class));
    }

    @Test
    @DisplayName("测试AI调用异常处理")
    void testGenerateReplyWithException() {
        // Given
        Long pictureId = 1L;
        Picture picture = new Picture();
        picture.setId(pictureId);
        picture.setName("测试图片");

        CommentAIReplyMessage message = CommentAIReplyMessage.builder()
                .commentId(100L)
                .pictureId(pictureId)
                .content("@AI助手 测试")
                .userId(1L)
                .createTime(new Date())
                .build();

        when(pictureService.getById(pictureId)).thenReturn(picture);
        when(chatLanguageModel.generate(any(String.class)))
                .thenThrow(new RuntimeException("AI服务异常"));

        // When
        String result = commentAIReplyService.generateReply(message);

        // Then
        assertNull(result);
    }
}
