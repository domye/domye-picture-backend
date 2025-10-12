package com.domye.picture.service.user.filterlist;

import com.domye.picture.service.user.UserService;
import com.domye.picture.service.user.WhitelistService;
import com.domye.picture.service.user.model.entity.User;
import com.domye.picture.service.user.model.vo.UserVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WhitelistServiceImpl implements WhitelistService {
    /**
     * 使用 redis - set 来存储允许直接发文章的白名单
     */
    private static final String PICTURE_WHITE_LIST = "auth_picture_white_list";
    /**
     * 使用 redis - set 来存储允许直接发文章的黑名单
     */
    private static final String PICTURE_BLACK_LIST = "auth_picture_black_list";
    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 检查用户是否在图片白名单中
     * @param userId 用户ID
     * @return 如果用户在白名单中返回true，否则返回false
     */
    @Override
    public boolean userInPictureWhiteList(Long userId) {
        Boolean a = stringRedisTemplate.opsForSet().isMember(PICTURE_WHITE_LIST, String.valueOf(userId));
        return Boolean.TRUE.equals(a);
    }

    /**
     * 检查用户是否在图片黑名单中
     * @param userId 用户ID
     * @return 如果用户在黑名单中返回true，否则返回false
     */
    @Override
    public boolean userInPictureBlackList(Long userId) {
        Boolean a = stringRedisTemplate.opsForSet().isMember(PICTURE_BLACK_LIST, String.valueOf(userId));
        return Boolean.TRUE.equals(a);
    }

    /**
     * 查询所有图片白名单用户
     * @return 返回白名单用户列表，如果无用户则返回空列表
     */
    @Override
    public List<UserVO> queryAllPictureWhiteListUsers() {
        Set<String> users = stringRedisTemplate.opsForSet().members(PICTURE_WHITE_LIST);
        List<User> userList = users.stream().map(user -> userService.getById(Long.valueOf(user))).collect(Collectors.toList());
        return userList.stream().map(user -> userService.getUserVO(user)).collect(Collectors.toList());
    }

    /**
     * 查询所有图片黑名单用户
     * @return 返回黑名单用户列表，如果无用户则返回空列表
     */
    @Override
    public List<UserVO> queryAllPictureBlackListUsers() {
        Set<String> users = stringRedisTemplate.opsForSet().members(PICTURE_BLACK_LIST);
        List<User> userList = users.stream().map(user -> userService.getById(Long.valueOf(user))).collect(Collectors.toList());
        return userList.stream().map(user -> userService.getUserVO(user)).collect(Collectors.toList());
    }

    /**
     * 将用户添加到图片白名单
     * @param userId 要添加的用户ID
     */
    @Override
    public void addUserToPictureWhiteList(Long userId) {
        stringRedisTemplate.opsForSet().add(PICTURE_WHITE_LIST, String.valueOf(userId));

    }

    /**
     * 将用户添加到图片黑名单
     * @param userId 要添加的用户ID
     */
    @Override
    public void addUserToPictureBlackList(Long userId) {
        stringRedisTemplate.opsForSet().add(PICTURE_BLACK_LIST, String.valueOf(userId));

    }

    /**
     * 从图片白名单中移除用户
     * @param userId 要移除的用户ID
     */
    @Override
    public void removeUserFromPictureWhiteList(Long userId) {
        stringRedisTemplate.opsForSet().remove(PICTURE_WHITE_LIST, String.valueOf(userId));

    }

    /**
     * 从图片黑名单中移除用户
     * @param userId 要移除的用户ID
     */
    @Override
    public void removeUserFromPictureBlackList(Long userId) {
        stringRedisTemplate.opsForSet().remove(PICTURE_BLACK_LIST, String.valueOf(userId));

    }
}
