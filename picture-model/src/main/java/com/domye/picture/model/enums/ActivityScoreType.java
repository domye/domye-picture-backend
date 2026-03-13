package com.domye.picture.model.enums;

import lombok.Getter;

/**
 * 活跃行为类型枚举
 * 定义不同行为的分数权重
 */
@Getter
public enum ActivityScoreType {
    /**
     * 查看图片 - 低权重，高频行为
     */
    VIEW_PICTURE("查看图片", 1),

    /**
     * 发布图片 - 高权重，核心贡献
     */
    PUBLISH_PICTURE("发布图片", 10),

    /**
     * 评论图片 - 中等权重
     */
    COMMENT_PICTURE("评论图片", 5),

    /**
     * 点赞图片 - 低权重
     */
    LIKE_PICTURE("点赞图片", 2),

    /**
     * 收藏图片 - 中低权重
     */
    FAVORITE_PICTURE("收藏图片", 3),

    /**
     * 分享图片 - 中等权重
     */
    SHARE_PICTURE("分享图片", 4),
    ;

    private final String text;
    private final int score;

    ActivityScoreType(String text, int score) {
        this.text = text;
        this.score = score;
    }

}