package com.domye.picture.api.controller;

import com.domye.picture.auth.annotation.AuthCheck;
import com.domye.picture.common.constant.UserConstant;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.ai.SyncEmbeddingRequest;
import com.domye.picture.model.dto.ai.SyncEmbeddingResult;
import com.domye.picture.service.api.ai.PictureEmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI 向量索引管理控制器
 * 用于管理图片元数据的向量索引
 */
@Slf4j
@RestController
@RequestMapping("/ai/embedding")
@RequiredArgsConstructor
public class AiEmbeddingController {

    private final PictureEmbeddingService pictureEmbeddingService;

    /**
     * 同步图片元数据到向量索引
     * 用于初始同步或重建索引
     */
    @PostMapping("/sync")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "同步图片元数据到向量索引", description = "将图片的元数据同步到 PgVector 向量存储，用于 RAG 检索")
    public BaseResponse<SyncEmbeddingResult> syncEmbeddings(@RequestBody SyncEmbeddingRequest request) {
        SyncEmbeddingResult result = pictureEmbeddingService.syncPictureEmbeddings(
                request.getSpaceId(),
                Boolean.TRUE.equals(request.getForceRebuild()),
                request.getBatchSize() != null ? request.getBatchSize() : 100
        );

        return Result.success(result);
    }

    /**
     * 检查图片是否已建立向量索引
     */
    @GetMapping("/check/{pictureId}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "检查图片索引状态", description = "检查指定图片是否已建立向量索引")
    public BaseResponse<Boolean> checkEmbedding(@PathVariable Long pictureId) {
        boolean indexed = pictureEmbeddingService.isIndexed(pictureId);
        return Result.success(indexed);
    }
}