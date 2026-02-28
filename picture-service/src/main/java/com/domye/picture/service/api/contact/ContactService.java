package com.domye.picture.service.api.contact;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.dto.contact.ContactAddRequest;
import com.domye.picture.model.dto.contact.ContactHandleRequest;
import com.domye.picture.model.dto.contact.ContactQueryRequest;
import com.domye.picture.model.entity.contact.Contact;
import com.domye.picture.model.vo.contact.ContactVO;

import java.util.List;
import java.util.Map;

/**
 * @author Domye
 * @description 针对表【contact(联系人)】的数据库操作Service
 * @createDate 2025-09-20 10:51:20
 */
public interface ContactService extends IService<Contact> {

    /**
     * 申请添加联系人
     * @param request 申请请求
     * @param userId 当前用户ID
     * @return 联系人记录ID
     */
    long applyContact(ContactAddRequest request, Long userId);

    /**
     * 处理联系人申请
     * @param request 处理请求
     * @param userId 当前用户ID
     * @return 是否处理成功
     */
    boolean handleContactRequest(ContactHandleRequest request, Long userId);

    /**
     * 获取我的联系人列表
     * @param request 查询请求
     * @param userId 当前用户ID
     * @return 联系人分页列表
     */
    Page<ContactVO> getMyContacts(ContactQueryRequest request, Long userId);

    /**
     * 获取待处理的联系人申请列表
     * @param request 查询请求
     * @param userId 当前用户ID
     * @return 联系人申请分页列表
     */
    Page<ContactVO> getPendingRequests(ContactQueryRequest request, Long userId);

    /**
     * 删除联系人
     * @param id 联系人记录ID
     * @param userId 当前用户ID
     * @return 是否删除成功
     */
    boolean removeContact(Long id, Long userId);

    /**
     * 获取查询条件
     * @param request 查询请求
     * @return QueryWrapper查询条件
     */
    QueryWrapper<Contact> getQueryWrapper(ContactQueryRequest request);

    /**
     * 获取好友列表（用于@选择器）
     * @param userId 当前用户ID
     * @return 好友列表
     */
    List<Map<String, Object>> getFriendsForMention(Long userId);
}