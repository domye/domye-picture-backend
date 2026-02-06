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
    private Long commentid;

    private String commenttext;

    private Date commenttime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        CommentsContent other = (CommentsContent) that;
        return (this.getCommentid() == null ? other.getCommentid() == null : this.getCommentid().equals(other.getCommentid()))
                && (this.getCommenttext() == null ? other.getCommenttext() == null : this.getCommenttext().equals(other.getCommenttext()))
                && (this.getCommenttime() == null ? other.getCommenttime() == null : this.getCommenttime().equals(other.getCommenttime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getCommentid() == null) ? 0 : getCommentid().hashCode());
        result = prime * result + ((getCommenttext() == null) ? 0 : getCommenttext().hashCode());
        result = prime * result + ((getCommenttime() == null) ? 0 : getCommenttime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", commentid=").append(commentid);
        sb.append(", commenttext=").append(commenttext);
        sb.append(", commenttime=").append(commenttime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}