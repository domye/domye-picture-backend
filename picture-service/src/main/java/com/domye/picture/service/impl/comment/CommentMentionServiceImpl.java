package com.domye.picture.service.impl.comment;


import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.model.entity.comment.CommentMention;
import com.domye.picture.service.api.comment.CommentMentionService;
import com.domye.picture.service.api.user.UserService;
import com.domye.picture.service.mapper.CommentMentionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Domye
 * @description 针对表【comment_mention(评论提及)】的数据库操作Service实现
 * @createDate 2026-02-23
 */
@Service
@RequiredArgsConstructor
public class CommentMentionServiceImpl extends ServiceImpl<CommentMentionMapper, CommentMention>
        implements CommentMentionService {

    private final UserService userService;

    @Override
    public void batchSaveMentions(Long commentId, List<Long> userIds) {
        if (CollUtil.isEmpty(userIds) || commentId == null) {
            return;
        }

        // 去重（同一评论中同一用户只保存一次）
        List<Long> distinctUserIds = userIds.stream()
                .distinct()
                .collect(Collectors.toList());

        // 检查@人数上限（≤10人）
        if (distinctUserIds.size() > 10) {
            distinctUserIds = distinctUserIds.subList(0, 10);
        }

        // 批量保存到 comment_mention 表
        List<CommentMention> mentions = distinctUserIds.stream()
                .map(userId -> {
                    CommentMention mention = new CommentMention();
                    mention.setCommentId(commentId);
                    mention.setMentionedUserId(userId);
                    mention.setIsRead(0);
                    return mention;
                })
                .collect(Collectors.toList());

        saveBatch(mentions);
    }
}
