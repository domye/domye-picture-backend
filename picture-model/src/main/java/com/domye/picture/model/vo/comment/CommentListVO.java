package com.domye.picture.model.vo.comment;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class CommentListVO {
    // 基础信息
    private Long commentId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String content;
//    private Integer likeCount;
    private Integer replyCount;      // 总回复数
    private Date createTime;
//    private Boolean isLiked;

    // 预览回复（最多5条）
    private List<CommentReplyVO> replyPreviewList;

    // @用户列表
    private List<CommentMentionVO> mentionedUsers;

}