package com.domye.picture.service.api.comment;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.entity.comment.CommentMention;
import com.domye.picture.model.vo.comment.CommentMentionVO;

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

    /**
     * 根据评论ID获取@提及列表
     * @param commentId 评论ID
     * @return @提及VO列表
     */
    List<CommentMentionVO> getMentionsByCommentId(Long commentId);
}
