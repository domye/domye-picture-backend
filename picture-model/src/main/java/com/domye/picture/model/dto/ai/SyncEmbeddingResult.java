package com.domye.picture.model.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 向量索引同步结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncEmbeddingResult {

    /**
     * 总图片数
     */
    private Integer totalCount;

    /**
     * 成功数
     */
    private Integer successCount;

    /**
     * 失败数
     */
    private Integer failCount;

    /**
     * 跳过数（已索引）
     */
    private Integer skipCount;

    /**
     * 是否全部成功
     */
    public boolean isAllSuccess() {
        return failCount == 0 && totalCount.equals(successCount + skipCount);
    }
}