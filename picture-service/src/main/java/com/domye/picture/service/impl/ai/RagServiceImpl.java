package com.domye.picture.service.impl.ai;

import cn.hutool.core.util.StrUtil;
import com.domye.picture.service.api.ai.RagService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 检索增强生成服务实现
 * 负责向量检索和上下文构建
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    /**
     * 向量索引元数据键：空间 ID
     */
    private static final String METADATA_KEY_SPACE_ID = "spaceId";

    /**
     * 默认最小相似度阈值
     */
    private static final double DEFAULT_MIN_SCORE = 0.5;

    /**
     * RAG 系统提示词模板
     */
    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是一个图片管理系统的智能助手。你的任务是帮助用户管理和搜索图片。

            以下是从知识库中检索到的相关图片信息，请基于这些信息回答用户的问题。
            如果检索到的信息与用户问题无关，请明确告知用户。

            ===检索到的相关图片信息===
            %s
            ===结束===

            请基于以上信息回答用户的问题。回答时请注意：
            1. 如果有多张相关图片，请简要列出每张图片的关键信息
            2. 如果用户想要查看某张图片，请提供图片的 ID 或名称
            3. 如果检索到的信息不足，请如实告知用户
            """;

    @Override
    public List<TextSegment> searchRelevantPictures(String query, int maxResults, Long spaceId) {
        if (StrUtil.isBlank(query)) {
            return new ArrayList<>();
        }

        try {
            // 将查询文本向量化
            var embeddingResponse = embeddingModel.embed(query);
            var queryEmbedding = embeddingResponse.content();

            // 构建搜索请求
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults)
                    .minScore(DEFAULT_MIN_SCORE)
                    .build();

            // 执行搜索
            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

            // 处理搜索结果
            List<TextSegment> results = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
                TextSegment segment = match.embedded();
                if (segment != null) {
                    // 如果指定了空间 ID，需要过滤
                    if (spaceId != null) {
                        String segmentSpaceId = segment.metadata(METADATA_KEY_SPACE_ID);
                        if (segmentSpaceId != null && !String.valueOf(spaceId).equals(segmentSpaceId)) {
                            continue; // 跳过不属于该空间的图片
                        }
                    }
                    results.add(segment);
                    log.debug("检索到相关片段: score={}, content={}", match.score(), segment.text());
                }
            }

            log.info("RAG 检索完成: 查询={}, 结果数={}", query, results.size());
            return results;
        } catch (Exception e) {
            log.error("RAG 检索失败: 查询={}", query, e);
            return new ArrayList<>();
        }
    }

    @Override
    public String buildContext(List<TextSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return "未找到相关的图片信息。";
        }

        StringBuilder context = new StringBuilder();
        context.append("共找到 ").append(segments.size()).append(" 条相关图片信息：\n\n");

        int index = 1;
        for (TextSegment segment : segments) {
            context.append("【图片 ").append(index).append("】\n");
            context.append(segment.text()).append("\n");

            // 添加图片元数据信息
            if (segment.metadata() != null) {
                String pictureId = segment.metadata("pictureId");
                String name = segment.metadata("name");
                String url = segment.metadata("url");

                if (pictureId != null) {
                    context.append("图片ID: ").append(pictureId).append("\n");
                }
                if (name != null) {
                    context.append("图片名称: ").append(name).append("\n");
                }
                if (url != null) {
                    context.append("图片链接: ").append(url).append("\n");
                }
            }

            context.append("\n");
            index++;
        }

        return context.toString();
    }

    @Override
    public String buildPrompt(String userMessage, String context) {
        if (StrUtil.isBlank(userMessage)) {
            return "";
        }

        // 使用模板构建系统提示词
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, context);

        // 返回完整的提示词
        return systemPrompt + "\n\n用户问题：" + userMessage;
    }

    @Override
    public String retrieveAndBuildPrompt(String userMessage, int maxResults, Long spaceId) {
        if (StrUtil.isBlank(userMessage)) {
            return "";
        }

        // 1. 检索相关图片
        List<TextSegment> segments = searchRelevantPictures(userMessage, maxResults, spaceId);

        // 2. 构建上下文
        String context = buildContext(segments);

        // 3. 构建完整提示词
        return buildPrompt(userMessage, context);
    }
}