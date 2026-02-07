package com.domye.picture.model.vo.comment;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class CommentReplyListVO {
    // 基础信息
    private Long commentId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String content;
    private Date createTime;

    private List<CommentReplyVO> replyList;

}