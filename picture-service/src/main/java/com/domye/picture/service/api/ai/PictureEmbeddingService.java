package com.domye.picture.service.api.ai;

import com.domye.picture.model.dto.ai.SyncEmbeddingResult;
import com.domye.picture.model.entity.picture.Picture;

/**
 * 图片向量索引服务接口
 * 负责将图片元数据向量化并存储到 PgVector
 */
public interface PictureEmbeddingService {

    /**
     * 为图片建立向量索引
     * 在图片上传/创建时调用
     *
     * @param picture 图片实体
     */
    void indexPicture(Picture picture);

    /**
     * 更新图片向量索引
     * 在图片信息更新时调用
     *
     * @param picture 更新后的图片实体
     */
    void updatePictureIndex(Picture picture);

    /**
     * 删除图片向量索引
     * 在图片删除时调用
     *
     * @param pictureId 图片 ID
     */
    void deletePictureIndex(Long pictureId);

    /**
     * 批量建立向量索引
     * 用于初始化或重建索引
     *
     * @param pictures 图片列表
     */
    void batchIndexPictures(Iterable<Picture> pictures);

    /**
     * 检查图片是否已建立向量索引
     *
     * @param pictureId 图片 ID
     * @return 是否已索引
     */
    boolean isIndexed(Long pictureId);

    /**
     * 同步图片元数据到向量索引
     * 用于初始同步或重建索引
     *
     * @param spaceId      空间 ID（可选，null 表示同步所有空间）
     * @param forceRebuild 是否强制重建（删除旧索引后重新建立）
     * @param batchSize    批次大小
     * @return 同步结果
     */
    SyncEmbeddingResult syncPictureEmbeddings(Long spaceId, boolean forceRebuild, int batchSize);
}