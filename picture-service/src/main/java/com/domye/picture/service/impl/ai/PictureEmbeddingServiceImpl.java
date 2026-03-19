package com.domye.picture.service.impl.ai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domye.picture.model.dto.ai.SyncEmbeddingResult;
import com.domye.picture.model.entity.picture.Picture;
import com.domye.picture.service.api.ai.PictureEmbeddingService;
import com.domye.picture.service.mapper.PictureMapper;
import com.domye.picture.common.helper.impl.RedisCache;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.data.document.Metadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 图片向量索引服务实现
 * 负责将图片元数据向量化并存储到 PgVector
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PictureEmbeddingServiceImpl implements PictureEmbeddingService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final RedisCache redisCache;
    private final PictureMapper pictureMapper;

    /**
     * 向量索引元数据键：图片 ID
     */
    private static final String METADATA_KEY_PICTURE_ID = "pictureId";

    /**
     * 向量索引元数据键：空间 ID
     */
    private static final String METADATA_KEY_SPACE_ID = "spaceId";

    /**
     * 向量索引元数据键：图片名称
     */
    private static final String METADATA_KEY_NAME = "name";

    /**
     * 向量索引元数据键：图片 URL
     */
    private static final String METADATA_KEY_URL = "url";

    /**
     * Redis 缓存键前缀：图片 embedding ID 映射
     */
    private static final String REDIS_KEY_EMBEDDING_ID = "domye:ai:embedding:picture:";

    /**
     * Embedding ID 缓存过期时间（天）
     */
    private static final long EMBEDDING_ID_CACHE_DAYS = 30;

    @Override
    @Async
    public void indexPicture(Picture picture) {
        if (picture == null || picture.getId() == null) {
            log.warn("图片为空或 ID 为空，跳过向量索引");
            return;
        }

        try {
            // 构建元数据文本内容
            String content = buildPictureContent(picture);
            if (StrUtil.isBlank(content)) {
                log.warn("图片元数据为空，跳过向量索引: pictureId={}", picture.getId());
                return;
            }

            // 构建元数据
            Metadata metadata = buildPictureMetadata(picture);

            // 创建文本片段
            TextSegment segment = TextSegment.from(content, metadata);

            // 生成向量并存储，返回 embedding ID
            Embedding embedding = embeddingModel.embed(segment).content();
            String embeddingId = embeddingStore.add(embedding, segment);

            // 缓存 embedding ID
            cacheEmbeddingId(picture.getId(), embeddingId);

            log.info("图片向量索引建立成功: pictureId={}, embeddingId={}", picture.getId(), embeddingId);
        } catch (Exception e) {
            log.error("图片向量索引建立失败: pictureId={}", picture.getId(), e);
            // 不抛出异常，避免影响主流程
        }
    }

    @Override
    @Async
    public void updatePictureIndex(Picture picture) {
        if (picture == null || picture.getId() == null) {
            log.warn("图片为空或 ID 为空，跳过向量索引更新");
            return;
        }

        try {
            // 先删除旧索引
            deletePictureIndex(picture.getId());

            // 重新建立索引
            indexPicture(picture);

            log.info("图片向量索引更新成功: pictureId={}", picture.getId());
        } catch (Exception e) {
            log.error("图片向量索引更新失败: pictureId={}", picture.getId(), e);
            // 不抛出异常，避免影响主流程
        }
    }

    @Override
    @Async
    public void deletePictureIndex(Long pictureId) {
        if (pictureId == null) {
            log.warn("图片 ID 为空，跳过向量索引删除");
            return;
        }

        try {
            // 从缓存获取 embedding ID
            String embeddingId = getEmbeddingId(pictureId);

            if (embeddingId != null) {
                // 使用 embedding ID 直接删除
                embeddingStore.remove(embeddingId);
                // 删除缓存
                removeEmbeddingIdCache(pictureId);
                log.info("图片向量索引删除成功: pictureId={}, embeddingId={}", pictureId, embeddingId);
            } else {
                log.warn("未找到图片的 embedding ID，可能已被删除: pictureId={}", pictureId);
            }
        } catch (Exception e) {
            log.error("图片向量索引删除失败: pictureId={}", pictureId, e);
            // 不抛出异常，避免影响主流程
        }
    }

    @Override
    public void batchIndexPictures(Iterable<Picture> pictures) {
        if (pictures == null) {
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (Picture picture : pictures) {
            try {
                indexPicture(picture);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("批量索引图片失败: pictureId={}", picture.getId(), e);
            }
        }

        log.info("批量图片向量索引完成: 成功={}, 失败={}", successCount, failCount);
    }

    @Override
    public boolean isIndexed(Long pictureId) {
        if (pictureId == null) {
            return false;
        }

        try {
            // 从缓存检查是否存在 embedding ID
            return getEmbeddingId(pictureId) != null;
        } catch (Exception e) {
            log.error("检查图片索引状态失败: pictureId={}", pictureId, e);
            return false;
        }
    }

    @Override
    public SyncEmbeddingResult syncPictureEmbeddings(Long spaceId, boolean forceRebuild, int batchSize) {
        log.info("开始同步图片向量索引: spaceId={}, forceRebuild={}, batchSize={}", spaceId, forceRebuild, batchSize);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);
        AtomicInteger totalCount = new AtomicInteger(0);

        // 构建查询条件
        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Picture::getIsDelete, 0);
        if (spaceId != null) {
            queryWrapper.eq(Picture::getSpaceId, spaceId);
        }

        // 查询总数
        Long total = pictureMapper.selectCount(queryWrapper);
        log.info("待同步图片总数: {}", total);

        // 分批处理
        int currentPage = 1;
        int totalPages = (int) Math.ceil((double) total / batchSize);

        while (currentPage <= totalPages) {
            Page<Picture> page = new Page<>(currentPage, batchSize, false);
            List<Picture> pictures = pictureMapper.selectPage(page, queryWrapper).getRecords();

            for (Picture picture : pictures) {
                totalCount.incrementAndGet();

                try {
                    // 检查是否已索引
                    if (!forceRebuild && isIndexed(picture.getId())) {
                        skipCount.incrementAndGet();
                        continue;
                    }

                    // 如果强制重建，先删除旧索引
                    if (forceRebuild && isIndexed(picture.getId())) {
                        deletePictureIndex(picture.getId());
                    }

                    // 建立索引
                    indexPictureSync(picture);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("同步图片向量索引失败: pictureId={}", picture.getId(), e);
                }
            }

            log.info("同步进度: {}/{}, 成功={}, 失败={}, 跳过={}",
                    currentPage, totalPages, successCount.get(), failCount.get(), skipCount.get());

            currentPage++;
        }

        SyncEmbeddingResult result = SyncEmbeddingResult.builder()
                .totalCount(totalCount.get())
                .successCount(successCount.get())
                .failCount(failCount.get())
                .skipCount(skipCount.get())
                .build();

        log.info("图片向量索引同步完成: {}", result);
        return result;
    }

    /**
     * 同步建立图片向量索引（非异步，用于批量同步）
     */
    private void indexPictureSync(Picture picture) {
        if (picture == null || picture.getId() == null) {
            return;
        }

        // 构建元数据文本内容
        String content = buildPictureContent(picture);
        if (StrUtil.isBlank(content)) {
            return;
        }

        // 构建元数据
        Metadata metadata = buildPictureMetadata(picture);

        // 创建文本片段
        TextSegment segment = TextSegment.from(content, metadata);

        // 生成向量并存储，返回 embedding ID
        Embedding embedding = embeddingModel.embed(segment).content();
        String embeddingId = embeddingStore.add(embedding, segment);

        // 缓存 embedding ID
        cacheEmbeddingId(picture.getId(), embeddingId);
    }

    /**
     * 构建图片元数据文本内容
     * 用于向量化和检索
     */
    private String buildPictureContent(Picture picture) {
        StringBuilder content = new StringBuilder();

        // 图片名称
        if (StrUtil.isNotBlank(picture.getName())) {
            content.append("名称：").append(picture.getName()).append("\n");
        }

        // 分类
        if (StrUtil.isNotBlank(picture.getCategory())) {
            content.append("分类：").append(picture.getCategory()).append("\n");
        }

        // 标签
        if (StrUtil.isNotBlank(picture.getTags())) {
            try {
                List<String> tags = JSONUtil.toList(picture.getTags(), String.class);
                if (!tags.isEmpty()) {
                    content.append("标签：").append(String.join("、", tags)).append("\n");
                }
            } catch (Exception e) {
                // 标签解析失败，使用原始字符串
                content.append("标签：").append(picture.getTags()).append("\n");
            }
        }

        // 简介
        if (StrUtil.isNotBlank(picture.getIntroduction())) {
            content.append("简介：").append(picture.getIntroduction()).append("\n");
        }

        return content.toString().trim();
    }

    /**
     * 构建图片元数据
     * 用于存储到向量的 metadata 中
     */
    private Metadata buildPictureMetadata(Picture picture) {
        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put(METADATA_KEY_PICTURE_ID, String.valueOf(picture.getId()));
        metadataMap.put(METADATA_KEY_NAME, picture.getName() != null ? picture.getName() : "");
        metadataMap.put(METADATA_KEY_URL, picture.getUrl() != null ? picture.getUrl() : "");

        if (picture.getSpaceId() != null) {
            metadataMap.put(METADATA_KEY_SPACE_ID, String.valueOf(picture.getSpaceId()));
        }

        return Metadata.from(metadataMap);
    }

    /**
     * 缓存 embedding ID
     */
    private void cacheEmbeddingId(Long pictureId, String embeddingId) {
        String key = REDIS_KEY_EMBEDDING_ID + pictureId;
        redisCache.put(key, embeddingId, EMBEDDING_ID_CACHE_DAYS, TimeUnit.DAYS);
    }

    /**
     * 获取缓存的 embedding ID
     */
    private String getEmbeddingId(Long pictureId) {
        String key = REDIS_KEY_EMBEDDING_ID + pictureId;
        Object value = redisCache.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 删除缓存的 embedding ID
     */
    private void removeEmbeddingIdCache(Long pictureId) {
        String key = REDIS_KEY_EMBEDDING_ID + pictureId;
        redisCache.remove(key);
    }
}