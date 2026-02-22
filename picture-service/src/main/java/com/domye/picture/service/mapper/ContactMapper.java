package com.domye.picture.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domye.picture.model.entity.contact.Contact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Domye
 * @description 针对表【contact(联系人)】的数据库操作Mapper
 * @createDate 2025-09-20 10:51:20
 * @Entity com.domye.picture.model.entity.contact.Contact
 */
@Mapper
public interface ContactMapper extends BaseMapper<Contact> {
    /**
     * 查询我的联系人列表
     * @param userId 用户ID
     * @return 联系人列表
     */
    List<Contact> selectMyContacts(@Param("userId") Long userId);

    /**
     * 查询待确认的联系请求数量
     * @param contactUserId 联系人用户ID
     * @return 待确认的联系请求数量
     */
    List<Contact> selectPendingRequests(@Param("contactUserId") Long contactUserId);
}