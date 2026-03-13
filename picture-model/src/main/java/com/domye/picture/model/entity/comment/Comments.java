package com.domye.picture.model.entity.comment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * @TableName comments
 */
@TableName(value = "comments")
@Data
@Builder
public class Comments implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long commentid;

    private Long pictureid;

    private Long parentid;

    private Long userid;

    private Long rootid;

    private Integer replycount;

    private Integer likecount;


    private Date createdtime;


    private Date updatedtime;

    @Serial
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
        Comments other = (Comments) that;
        return (this.getCommentid() == null ? other.getCommentid() == null : this.getCommentid().equals(other.getCommentid()))
                && (this.getPictureid() == null ? other.getPictureid() == null : this.getPictureid().equals(other.getPictureid()))
                && (this.getParentid() == null ? other.getParentid() == null : this.getParentid().equals(other.getParentid()))
                && (this.getUserid() == null ? other.getUserid() == null : this.getUserid().equals(other.getUserid()))
                && (this.getRootid() == null ? other.getRootid() == null : this.getRootid().equals(other.getRootid()))
                && (this.getReplycount() == null ? other.getReplycount() == null : this.getReplycount().equals(other.getReplycount()))
                && (this.getLikecount() == null ? other.getLikecount() == null : this.getLikecount().equals(other.getLikecount()))
                && (this.getCreatedtime() == null ? other.getCreatedtime() == null : this.getCreatedtime().equals(other.getCreatedtime()))
                && (this.getUpdatedtime() == null ? other.getUpdatedtime() == null : this.getUpdatedtime().equals(other.getUpdatedtime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getCommentid() == null) ? 0 : getCommentid().hashCode());
        result = prime * result + ((getPictureid() == null) ? 0 : getPictureid().hashCode());
        result = prime * result + ((getParentid() == null) ? 0 : getParentid().hashCode());
        result = prime * result + ((getUserid() == null) ? 0 : getUserid().hashCode());
        result = prime * result + ((getRootid() == null) ? 0 : getRootid().hashCode());
        result = prime * result + ((getReplycount() == null) ? 0 : getReplycount().hashCode());
        result = prime * result + ((getLikecount() == null) ? 0 : getLikecount().hashCode());
        result = prime * result + ((getCreatedtime() == null) ? 0 : getCreatedtime().hashCode());
        result = prime * result + ((getUpdatedtime() == null) ? 0 : getUpdatedtime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                " [" +
                "Hash = " + hashCode() +
                ", commentid=" + commentid +
                ", pictureid=" + pictureid +
                ", parentid=" + parentid +
                ", userid=" + userid +
                ", rootid=" + rootid +
                ", replycount=" + replycount +
                ", likecount=" + likecount +
                ", createdtime=" + createdtime +
                ", updatedtime=" + updatedtime +
                ", serialVersionUID=" + serialVersionUID +
                "]";
    }
}