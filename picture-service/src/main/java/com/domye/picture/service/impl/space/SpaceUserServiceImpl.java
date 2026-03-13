package com.domye.picture.service.impl.space;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.common.exception.BusinessException;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.model.dto.space.SpaceUserAddRequest;
import com.domye.picture.model.dto.space.SpaceUserEditRequest;
import com.domye.picture.model.dto.space.SpaceUserQueryRequest;
import com.domye.picture.model.entity.contact.Contact;
import com.domye.picture.model.entity.space.Space;
import com.domye.picture.model.entity.space.SpaceUser;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.enums.ContactStatusEnum;
import com.domye.picture.model.enums.SpaceRoleEnum;
import com.domye.picture.model.mapper.space.SpaceStructMapper;
import com.domye.picture.model.mapper.space.SpaceUserStructMapper;
import com.domye.picture.model.mapper.user.UserStructMapper;
import com.domye.picture.model.vo.space.SpaceUserVO;
import com.domye.picture.service.api.contact.ContactService;
import com.domye.picture.service.api.space.SpaceService;
import com.domye.picture.service.api.space.SpaceUserService;
import com.domye.picture.service.api.user.UserService;
import com.domye.picture.service.mapper.SpaceUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Domye
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2025-09-20 10:51:20
 */
@Service
@RequiredArgsConstructor
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {
    final SpaceService spaceService;
    final UserService userService;
    final ContactService contactService;
    final UserStructMapper userStructMapper;
    final SpaceUserStructMapper spaceUserStructMapper;
    final SpaceStructMapper spaceStructMapper;

    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest, Long loginUserId) {
        // 参数校验
        Throw.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);

        Long spaceId = spaceUserAddRequest.getSpaceId();
        Long targetUserId = spaceUserAddRequest.getUserId();

        // 检查当前用户是否是该空间的管理员
        SpaceUserQueryRequest queryRequest = new SpaceUserQueryRequest();
        queryRequest.setSpaceId(spaceId);
        queryRequest.setUserId(loginUserId);
        SpaceUser currentUserSpaceRole = this.getOne(this.getQueryWrapper(queryRequest));

        // 如果不是管理员，需要检查是否在联系人列表中
        if (currentUserSpaceRole == null || !SpaceRoleEnum.ADMIN.getValue().equals(currentUserSpaceRole.getSpaceRole())) {
            // 查询联系人表，检查是否存在已通过的联系关系
            LambdaQueryWrapper<Contact> contactQuery = new LambdaQueryWrapper<>();
            contactQuery.eq(Contact::getUserId, loginUserId)
                    .eq(Contact::getContactUserId, targetUserId)
                    .eq(Contact::getStatus, ContactStatusEnum.ACCEPTED.getValue());
            Contact contact = contactService.getOne(contactQuery);
            Throw.throwIf(contact == null, ErrorCode.NO_AUTH_ERROR, "非管理员只能从联系人中添加成员");
        }

        // 实体转换和校验
        SpaceUser spaceUser = spaceUserStructMapper.toEntity(spaceUserAddRequest);
        validSpaceUser(spaceUser, true);

        // 数据库操作
        boolean result = this.save(spaceUser);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }

    @Override
    public void deleteSpaceUser(long id, Long loginUserId) {
        // 判断是否存在
        SpaceUser oldSpaceUser = this.getById(id);
        Throw.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 不能删除自己
        Throw.throwIf(oldSpaceUser.getUserId().equals(loginUserId), ErrorCode.NO_AUTH_ERROR, "无权限编辑自己");
        // 操作数据库
        boolean result = this.removeById(id);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public SpaceUser getSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest) {
        // 参数校验
        Throw.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        Throw.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        // 查询数据库
        SpaceUser spaceUser = this.getOne(this.getQueryWrapper(spaceUserQueryRequest));
        Throw.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return spaceUser;
    }

    @Override
    public List<SpaceUserVO> listSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest) {
        Throw.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUser> spaceUserList = this.list(this.getQueryWrapper(spaceUserQueryRequest));
        return this.getSpaceUserVOList(spaceUserList);
    }

    @Override
    public void editSpaceUser(SpaceUserEditRequest spaceUserEditRequest, Long loginUserId) {
        // 参数校验
        Throw.throwIf(spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);

        // 将实体类和 DTO 进行转换
        SpaceUser spaceUser = spaceUserStructMapper.toEntity(spaceUserEditRequest);
        // 数据校验
        validSpaceUser(spaceUser, false);
        // 判断是否存在
        long id = spaceUserEditRequest.getId();
        SpaceUser oldSpaceUser = this.getById(id);
        Throw.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 不能编辑自己
        Throw.throwIf(oldSpaceUser.getUserId().equals(loginUserId), ErrorCode.NO_AUTH_ERROR, "无权限编辑自己");
        // 操作数据库
        boolean result = this.updateById(spaceUser);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public List<SpaceUserVO> listMyTeamSpace(Long userId) {
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(userId);
        List<SpaceUser> spaceUserList = this.list(this.getQueryWrapper(spaceUserQueryRequest));
        return this.getSpaceUserVOList(spaceUserList);
    }

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceUserQueryRequest.getId();
        Long userId = spaceUserQueryRequest.getUserId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();

        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId)
                .eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole)
                .eq(ObjUtil.isNotEmpty(id), "id", id)
                .eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        return queryWrapper;
    }

    //查询封装类
    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {

        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        // 对象列表 => 封装对象列表
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(spaceUserStructMapper::toSpaceUserVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        // 2. 批量查询用户和空间
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));
        // 2. 填充信息
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceUserVO.setUser(userStructMapper.toUserVo(user));
            Space space = null;
            if (spaceIdSpaceListMap.containsKey(spaceUserVO.getSpaceId())) {
                space = spaceIdSpaceListMap.get(spaceUserVO.getSpaceId()).get(0);

            }
            spaceUserVO.setSpace(spaceStructMapper.toVo(space));
        });
        return spaceUserVOList;
    }

    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        Throw.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        // 要创建
        if (add) {
            Throw.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            User user = userService.getById(userId);
            Throw.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            Throw.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }

}