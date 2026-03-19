package com.domye.picture.model.dto.ai;

import lombok.Data;

/**
 * 向量索引同步请求
 */
@Data
public class SyncEmbeddingRequest {

    /**
     * 空间 ID（可选，不传则同步所有空间）
     */
    private Long spaceId;

    /**
     * 是否强制重建（删除旧索引后重新建立）
     */
    private Boolean forceRebuild = false;

    /**
     * 批次大小（默认 100）
     */
    private Integer batchSize = 100;
}