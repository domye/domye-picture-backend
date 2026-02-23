package com.domye.picture.model.entity.comment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 评论提及
 * @TableName comment_mention
 */
@TableName("comment_mention")
@Data
public class CommentMention implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 评论 id
     */
    private Long commentId;

    /**
     * 被提及用户 id
     */
    private Long mentionedUserId;

    /**
     * 是否已读：0-未读，1-已读
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    private Date createdTime;

    private static final long serialVersionUID = 1L;
}
