package com.domye.picture.model.entity.contact;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 联系人
 * @TableName contact
 */
@TableName(value = "contact")
@Data
public class Contact {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private Long id;

    /**
     * 用户id（发起者）
     */
    private Long userId;

    /**
     * 联系人用户id
     */
    private Long contactUserId;

    /**
     * 状态：0-待确认，1-已通过，2-已拒绝
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}