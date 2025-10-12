package com.domye.picture.service.user;

import com.domye.picture.service.user.model.vo.UserVO;

import java.util.List;

public interface WhitelistService {
    /**
     * 判断用户是否再图片发布的白名单中；
     * @param userId
     * @return
     */
    boolean userInPictureWhiteList(Long userId);

    /**
     * 判断用户是否再图片发布的黑名单中；
     * @param userId
     * @return
     */
    boolean userInPictureBlackList(Long userId);

    /**
     * 获取所有的白名单用户
     * @return
     */
    List<UserVO> queryAllPictureWhiteListUsers();

    /**
     * 获取所有的黑名单用户
     * @return
     */
    List<UserVO> queryAllPictureBlackListUsers();

    /**
     * 将用户添加到白名单中
     * @param userId
     */
    void addUserToPictureWhiteList(Long userId);

    /**
     * 将用户添加到黑名单中
     * @param userId
     */
    void addUserToPictureBlackList(Long userId);

    /**
     * 从白名单中移除用户
     * @param userId
     */
    void removeUserFromPictureWhiteList(Long userId);

    /**
     * 从黑名单中移除用户
     * @param userId
     */
    void removeUserFromPictureBlackList(Long userId);
}

