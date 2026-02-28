package com.domye.picture.service.impl.contact;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.model.dto.contact.ContactAddRequest;
import com.domye.picture.model.dto.contact.ContactHandleRequest;
import com.domye.picture.model.dto.contact.ContactQueryRequest;
import com.domye.picture.model.entity.contact.Contact;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.enums.ContactStatusEnum;
import com.domye.picture.model.mapper.user.UserStructMapper;
import com.domye.picture.model.mapper.ContactStructMapper;
import com.domye.picture.model.vo.contact.ContactVO;
import com.domye.picture.service.api.contact.ContactService;
import com.domye.picture.service.api.user.UserService;
import com.domye.picture.service.mapper.ContactMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Domye
 * @description 针对表【contact(联系人)】的数据库操作Service实现
 * @createDate 2025-09-20 10:51:20
 */
@Service
@RequiredArgsConstructor
public class ContactServiceImpl extends ServiceImpl<ContactMapper, Contact>
        implements ContactService {

    private final UserService userService;
    private final UserStructMapper userStructMapper;
    private final ContactStructMapper contactStructMapper;

    @Override
    public long applyContact(ContactAddRequest request, Long userId) {
        // 参数校验
        Throw.throwIf(request == null || request.getContactUserId() == null, ErrorCode.PARAMS_ERROR);
        Long contactUserId = request.getContactUserId();

        // 不能添加自己为联系人
        Throw.throwIf(userId.equals(contactUserId), ErrorCode.PARAMS_ERROR, "不能添加自己为联系人");

        // 检查目标用户是否存在
        User contactUser = userService.getById(contactUserId);
        Throw.throwIf(contactUser == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        // 检查是否已存在好友关系（无论状态）
        QueryWrapper<Contact> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId).eq("contactUserId", contactUserId);
        Contact existingContact = this.getOne(queryWrapper);
        Throw.throwIf(existingContact != null, ErrorCode.PARAMS_ERROR, "已存在好友关系");

        // 创建双向记录
        // 记录1: userId -> contactUserId (申请人视角)
        Contact contact1 = new Contact();
        contact1.setUserId(userId);
        contact1.setContactUserId(contactUserId);
        contact1.setStatus(ContactStatusEnum.PENDING.getValue());

        // 记录2: contactUserId -> userId (被申请人视角)
        Contact contact2 = new Contact();
        contact2.setUserId(contactUserId);
        contact2.setContactUserId(userId);
        contact2.setStatus(ContactStatusEnum.PENDING.getValue());

        // 保存两条记录（逐个保存以确保ID回填）
        boolean result1 = this.save(contact1);
        Throw.throwIf(!result1, ErrorCode.OPERATION_ERROR, "添加联系人失败");
        
        boolean result2 = this.save(contact2);
        Throw.throwIf(!result2, ErrorCode.OPERATION_ERROR, "添加联系人失败");

        return contact1.getId();
    }

    @Override
    public boolean handleContactRequest(ContactHandleRequest request, Long userId) {
        // 参数校验
        Throw.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        Long contactId = request.getId();
        String statusStr = request.getStatus();

        // 查询申请记录
        Contact contact = this.getById(contactId);
        Throw.throwIf(contact == null, ErrorCode.NOT_FOUND_ERROR, "联系人记录不存在");

        // 验证当前用户是否是被申请人（contactUserId）
        Throw.throwIf(!userId.equals(contact.getContactUserId()), ErrorCode.NO_AUTH_ERROR, "无权限处理此申请");

        // 验证当前状态是否为待确认
        Throw.throwIf(!ContactStatusEnum.PENDING.getValue().equals(contact.getStatus()),
                ErrorCode.PARAMS_ERROR, "该申请已被处理");

        // 解析状态
        ContactStatusEnum targetStatus = ContactStatusEnum.getEnumByValue(Integer.valueOf(statusStr));
        Throw.throwIf(targetStatus == null, ErrorCode.PARAMS_ERROR, "无效的状态");

        // 更新双方的记录状态
        QueryWrapper<Contact> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.eq("userId", contact.getUserId())
                .eq("contactUserId", contact.getContactUserId());
        Contact record1 = this.getOne(queryWrapper1);

        QueryWrapper<Contact> queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.eq("userId", contact.getContactUserId())
                .eq("contactUserId", contact.getUserId());
        Contact record2 = this.getOne(queryWrapper2);

        if (targetStatus == ContactStatusEnum.ACCEPTED) {
            // 接受：更新双方状态为已通过
            record1.setStatus(ContactStatusEnum.ACCEPTED.getValue());
            record2.setStatus(ContactStatusEnum.ACCEPTED.getValue());
            return this.updateBatchById(List.of(record1, record2));
        } else if (targetStatus == ContactStatusEnum.REJECTED) {
            // 拒绝：删除双方记录
            return this.removeBatchByIds(List.of(record1.getId(), record2.getId()));
        }

        return false;
    }

    @Override
    public Page<ContactVO> getMyContacts(ContactQueryRequest request, Long userId) {
        long current = request.getCurrent();
        long size = request.getPageSize();

        // 查询我的联系人（状态为已通过的记录）
        QueryWrapper<Contact> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("status", ContactStatusEnum.ACCEPTED.getValue());
        
        Page<Contact> contactPage = this.page(new Page<>(current, size), queryWrapper);
        return this.getContactVOPage(contactPage);
    }

    @Override
    public Page<ContactVO> getPendingRequests(ContactQueryRequest request, Long userId) {
        long current = request.getCurrent();
        long size = request.getPageSize();

        // 查询待处理的申请（contactUserId等于当前用户且状态为待确认）
        QueryWrapper<Contact> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("contactUserId", userId)
                .eq("status", ContactStatusEnum.PENDING.getValue());

        Page<Contact> contactPage = this.page(new Page<>(current, size), queryWrapper);
        return this.getContactVOPage(contactPage);
    }

    @Override
    public boolean removeContact(Long id, Long userId) {
        // 查询联系人记录
        Contact contact = this.getById(id);
        Throw.throwIf(contact == null, ErrorCode.NOT_FOUND_ERROR, "联系人不存在");

        // 验证权限（只能删除自己的联系人）
        Throw.throwIf(!userId.equals(contact.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权限删除此联系人");

        // 查找双向记录
        QueryWrapper<Contact> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", contact.getContactUserId())
                .eq("contactUserId", contact.getUserId());
        Contact reverseContact = this.getOne(queryWrapper);

        // 删除双方记录
        if (reverseContact != null) {
            return this.removeBatchByIds(List.of(id, reverseContact.getId()));
        }
        return this.removeById(id);
    }

    @Override
    public QueryWrapper<Contact> getQueryWrapper(ContactQueryRequest request) {
        QueryWrapper<Contact> queryWrapper = new QueryWrapper<>();
        if (request == null) {
            return queryWrapper;
        }
        Integer status = request.getStatus();
        queryWrapper.eq(ObjUtil.isNotEmpty(status), "status", status);
        return queryWrapper;
    }

    /**
     * 获取联系人视图对象分页信息
     *
     * @param contactPage 联系人分页对象
     * @return 返回封装后的联系人视图对象分页信息
     */
    private Page<ContactVO> getContactVOPage(Page<Contact> contactPage) {
        List<Contact> contactList = contactPage.getRecords();
        Page<ContactVO> contactVOPage = new Page<>(contactPage.getCurrent(), contactPage.getSize(), contactPage.getTotal());
        
        if (CollUtil.isEmpty(contactList)) {
            return contactVOPage;
        }

        // 对象列表 => 封装对象列表
        List<ContactVO> contactVOList = contactList.stream()
                .map(contactStructMapper::toVo)
                .collect(Collectors.toList());

        // 关联查询联系人用户信息
        Set<Long> contactUserIdSet = contactList.stream()
                .map(Contact::getContactUserId)
                .collect(Collectors.toSet());
        
        Map<Long, List<User>> contactUserIdUserMap = userService.listByIds(contactUserIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充联系人用户信息
        contactVOList.forEach(contactVO -> {
            Long contactUserId = contactVO.getContactUserId();
            User contactUser = null;
            if (contactUserIdUserMap.containsKey(contactUserId)) {
                contactUser = contactUserIdUserMap.get(contactUserId).get(0);
            }
            contactVO.setContactUser(userStructMapper.toUserVo(contactUser));
        });

        contactVOPage.setRecords(contactVOList);
        return contactVOPage;
    }

    @Override
    public List<Map<String, Object>> getFriendsForMention(Long userId) {
        ContactQueryRequest queryRequest = new ContactQueryRequest();
        queryRequest.setCurrent(1);
        queryRequest.setPageSize(1000);
        queryRequest.setStatus(1); // 仅返回已通过的好友
        Page<ContactVO> contactPage = this.getMyContacts(queryRequest, userId);
        
        return contactPage.getRecords().stream()
                .map(contact -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", contact.getContactUserId());
                    if (contact.getContactUser() != null) {
                        map.put("userName", contact.getContactUser().getUserName());
                        map.put("userAvatar", contact.getContactUser().getUserAvatar());
                    }
                    return map;
                })
                .collect(Collectors.toList());
    }
}
