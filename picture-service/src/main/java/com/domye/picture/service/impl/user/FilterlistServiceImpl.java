package com.domye.picture.service.impl.user;


import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.helper.impl.RedisCache;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.enums.FilterModeEnum;
import com.domye.picture.model.enums.FilterTypeEnum;
import com.domye.picture.model.vo.user.UserVO;
import com.domye.picture.service.api.user.FilterlistService;
import com.domye.picture.service.api.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FilterlistServiceImpl implements FilterlistService {

    final UserService userService;
    final RedisCache redisCache;

    /**
     * 检查用户是否在名单中
     * @param userId 用户ID
     * @return 如果用户在白名单中返回true，否则返回false
     */
    @Override
    public boolean isInFilterList(Long userId, Long type, Long mode) {
        String key = FilterTypeEnum.getTextByValue(type) + "_" + FilterModeEnum.getTextByValue(mode);
        Boolean a = redisCache.sIsMember(key, String.valueOf(userId));
        return Boolean.TRUE.equals(a);
    }

    /**
     * 查询所有名单用户
     * @return 返回名单用户列表，如果无用户则返回空列表
     */
    @Override
    public List<UserVO> queryAllFilterListUsers(Long type, Long mode) {
        String key = FilterTypeEnum.getTextByValue(type) + "_" + FilterModeEnum.getTextByValue(mode);
        Set<Object> users = redisCache.sMembers(key);

        List<User> userList = null;
        if (users != null) {
            userList = users.stream().map(user -> userService.getById(Long.valueOf((String) user))).collect(Collectors.toList());
        }
        if (userList != null) {
            return userList.stream().map(user -> userService.getUserVO(user)).collect(Collectors.toList());
        }
        return null;
    }


    /**
     * 将用户添加到名单
     * @param userId 要添加的用户ID
     * @return
     */
    @Override
    public void addUserToFilterList(Long userId, Long type, Long mode) {
        User user = userService.getById(userId);
        Throw.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        // 如果用户已经在名单中，则抛出异常
        Throw.throwIf(isInFilterList(userId, type, mode), ErrorCode.OPERATION_ERROR, "用户已经在名单中");
        String key = FilterTypeEnum.getTextByValue(type) + "_" + FilterModeEnum.getTextByValue(mode);
        redisCache.sAdd(key, userId);

    }


    /**
     * 从名单中移除用户
     * @param userId 要移除的用户ID
     */
    @Override
    public void removeUserFromFilterList(Long userId, Long type, Long mode) {
        User user = userService.getById(userId);
        Throw.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        Throw.throwIf(!isInFilterList(userId, type, mode), ErrorCode.OPERATION_ERROR, "用户不在名单中");
        String key = FilterTypeEnum.getTextByValue(type) + "_" + FilterModeEnum.getTextByValue(mode);
        redisCache.sRemove(key, userId);

    }
}
