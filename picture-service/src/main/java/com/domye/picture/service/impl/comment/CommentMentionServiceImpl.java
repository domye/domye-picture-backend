package com.domye.picture.service.impl.comment;


import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.model.entity.comment.CommentMention;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.vo.comment.CommentMentionVO;
import com.domye.picture.service.api.comment.CommentMentionService;
import com.domye.picture.service.api.user.UserService;
import com.domye.picture.service.mapper.CommentMentionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Override
    public List<CommentMentionVO> getMentionsByCommentId(Long commentId) {
        if (commentId == null) {
            return Collections.emptyList();
        }

        // 查询评论的所有@提及记录
        QueryWrapper<CommentMention> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("commentId", commentId);
        List<CommentMention> mentions = list(queryWrapper);

        if (CollUtil.isEmpty(mentions)) {
            return Collections.emptyList();
        }

        // 获取所有被提及用户ID
        Set<Long> userIds = mentions.stream()
                .map(CommentMention::getMentionedUserId)
                .collect(Collectors.toSet());

        // 查询用户信息
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (existing, replacement) -> existing));

        // 转换为VO列表
        return mentions.stream()
                .map(mention -> {
                    CommentMentionVO vo = new CommentMentionVO();
                    vo.setId(mention.getId());
                    vo.setCommentId(mention.getCommentId());
                    vo.setMentionedUserId(mention.getMentionedUserId());
                    vo.setIsRead(mention.getIsRead());
                    vo.setCreateTime(mention.getCreatedTime());

                    // 填充用户信息
                    User user = userMap.get(mention.getMentionedUserId());
                    if (user != null) {
                        vo.setMentionedUserName(user.getUserName());
                        vo.setMentionedUserAvatar(user.getUserAvatar());
                    }

                    return vo;
                })
                .collect(Collectors.toList());
    }
}
