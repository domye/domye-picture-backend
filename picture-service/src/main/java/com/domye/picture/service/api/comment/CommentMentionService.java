package com.domye.picture.service.api.comment;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.entity.comment.CommentMention;

import java.util.List;

/**
 * @author Domye
 * @description 针对表【comment_mention(评论提及)】的数据库操作Service
 * @createDate 2026-02-23
 */
public interface CommentMentionService extends IService<CommentMention> {

    /**
     * 批量保存@提及记录
     * @param commentId 评论ID
     * @param userIds 被@的用户ID列表
     */
    void batchSaveMentions(Long commentId, List<Long> userIds);

}
