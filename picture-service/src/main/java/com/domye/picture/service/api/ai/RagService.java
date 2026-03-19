package com.domye.picture.service.api.ai;

import com.domye.picture.model.dto.ai.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

/**
 * RAG 检索增强生成服务接口
 * 负责向量检索和上下文构建
 */
public interface RagService {

    /**
     * 搜索相关图片
     * 基于用户查询进行向量相似度搜索
     *
     * @param query      用户查询文本
     * @param maxResults 最大返回结果数
     * @param spaceId    空间 ID（可选，用于限定搜索范围）
     * @return 相关的文本片段列表
     */
    List<TextSegment> searchRelevantPictures(String query, int maxResults, Long spaceId);

    /**
     * 构建 RAG 上下文
     * 将检索到的片段组装成上下文字符串
     *
     * @param segments 文本片段列表
     * @return 格式化的上下文字符串
     */
    String buildContext(List<TextSegment> segments);

    /**
     * 构建 RAG 提示词
     * 将用户问题和上下文组合成完整的提示词
     *
     * @param userMessage 用户消息
     * @param context     RAG 上下文
     * @return 完整的提示词
     */
    String buildPrompt(String userMessage, String context);

    /**
     * 检索并构建完整提示词
     * 一步完成检索和提示词构建
     *
     * @param userMessage 用户消息
     * @param maxResults  最大检索结果数
     * @param spaceId     空间 ID（可选）
     * @return 完整的提示词
     */
    String retrieveAndBuildPrompt(String userMessage, int maxResults, Long spaceId);
}