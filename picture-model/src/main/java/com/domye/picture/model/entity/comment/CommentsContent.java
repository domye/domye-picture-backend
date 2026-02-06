package com.domye.picture.model.entity.comment;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @TableName comments_content
 */
@TableName(value = "comments_content")
@Data
public class CommentsContent implements Serializable {
    /**
     *
     */
    @TableId
    private Long commentId;

    private String commentText;

    private Date commentTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}