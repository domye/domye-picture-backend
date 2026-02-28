package com.domye.picture.model.vo.contact;

import com.domye.picture.model.entity.contact.Contact;
import com.domye.picture.model.mapper.ContactStructMapper;
import com.domye.picture.model.vo.user.UserVO;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ContactVO implements Serializable {

    private static final long serialVersionUID = 1L;
    
    /**
     * id
     */
    private Long id;
    
    /**
     * 用户 id
     */
    private Long userId;
    
    /**
     * 联系人用户 id
     */
    private Long contactUserId;
    
    /**
     * 状态：0-未验证 1-已通过 2-已拒绝
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 联系人信息
     */
    private UserVO contactUser;
    
    /**
     * 对象转封装类
     * @param contact
     * @return
     */
    public static ContactVO objToVo(Contact contact) {
        if (contact == null) {
            return null;
        }
        return ContactStructMapper.INSTANCE.toVo(contact);
    }
}