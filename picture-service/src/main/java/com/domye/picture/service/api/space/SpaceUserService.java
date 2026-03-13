package com.domye.picture.service.api.space;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.dto.space.SpaceUserAddRequest;
import com.domye.picture.model.dto.space.SpaceUserEditRequest;
import com.domye.picture.model.dto.space.SpaceUserQueryRequest;
import com.domye.picture.model.entity.space.SpaceUser;
import com.domye.picture.model.vo.space.SpaceUserVO;

import java.util.List;

/**
 * @author Domye
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-09-20 10:51:20
 */
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 添加成员到空间
     * @param spaceUserAddRequest 添加请求
     * @param loginUserId 当前登录用户ID
     * @return 新成员ID
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest, Long loginUserId);

    /**
     * 删除空间成员
     * @param id 成员记录ID
     * @param loginUserId 当前登录用户ID
     */
    void deleteSpaceUser(long id, Long loginUserId);

    /**
     * 获取空间成员信息
     * @param spaceUserQueryRequest 查询请求
     * @return 成员信息
     */
    SpaceUser getSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 查询成员列表
     * @param spaceUserQueryRequest 查询请求
     * @return 成员VO列表
     */
    List<SpaceUserVO> listSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 编辑成员信息
     * @param spaceUserEditRequest 编辑请求
     * @param loginUserId 当前登录用户ID
     */
    void editSpaceUser(SpaceUserEditRequest spaceUserEditRequest, Long loginUserId);

    /**
     * 查询我加入的团队空间列表
     * @param userId 用户ID
     * @return 成员VO列表
     */
    List<SpaceUserVO> listMyTeamSpace(Long userId);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    //查询封装类
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    void validSpaceUser(SpaceUser spaceUser, boolean add);
}
