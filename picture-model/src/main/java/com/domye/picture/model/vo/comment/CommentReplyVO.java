package com.domye.picture.model.vo.comment;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class CommentReplyVO {
    private Long commentId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String content;
    private Long parentId;
    private String parentUserName;   // @被回复人
    private Date createTime;
}