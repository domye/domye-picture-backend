package com.domye.picture.service.user.filterlist;

import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.helper.RedisUtil;
import com.domye.picture.service.user.FilterlistService;
import com.domye.picture.service.user.UserService;
import com.domye.picture.service.user.model.entity.User;
import com.domye.picture.service.user.model.enums.FilterModeEnum;
import com.domye.picture.service.user.model.enums.FilterTypeEnum;
import com.domye.picture.service.user.model.vo.UserVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FilterlistServiceImpl implements FilterlistService {

    @Resource
    private UserService userService;

    /**
     * 检查用户是否在名单中
     * @param userId 用户ID
     * @return 如果用户在白名单中返回true，否则返回false
     */
    @Override
    public boolean isInFilterList(Long userId, Long type, Long mode) {
        String key = FilterTypeEnum.getTextByValue(type) + "_" + FilterModeEnum.getTextByValue(mode);
        Boolean a = RedisUtil.hasSet(key, String.valueOf(userId));
        return Boolean.TRUE.equals(a);
    }

    /**
     * 查询所有名单用户
     * @return 返回名单用户列表，如果无用户则返回空列表
     */
    @Override
    public List<UserVO> queryAllFilterListUsers(Long type, Long mode) {
        String key = FilterTypeEnum.getTextByValue(type) + "_" + FilterModeEnum.getTextByValue(mode);
        Set<String> users = RedisUtil.getSet(key);
        List<User> userList = null;
        if (users != null) {
            userList = users.stream().map(user -> userService.getById(Long.valueOf(user))).collect(Collectors.toList());
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
        RedisUtil.addSet(key, String.valueOf(userId));

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
        RedisUtil.deleteSet(key, String.valueOf(userId));

    }
}
