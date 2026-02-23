package com.domye.picture.model.vo.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 评论@提及信息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentMentionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 提及记录ID
     */
    private Long id;

    /**
     * 评论ID
     */
    private Long commentId;

    /**
     * 被提及用户ID
     */
    private Long mentionedUserId;

    /**
     * 被提及用户名称
     */
    private String mentionedUserName;

    /**
     * 被提及用户头像
     */
    private String mentionedUserAvatar;

    /**
     * 是否已读：0-未读，1-已读
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    private Date createTime;
}
