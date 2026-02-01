package com.domye.picture.service.space;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.space.dto.SpaceUserAddRequest;
import com.domye.picture.model.space.dto.SpaceUserQueryRequest;
import com.domye.picture.model.space.entity.SpaceUser;
import com.domye.picture.model.space.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Domye
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-09-20 10:51:20
 */
public interface SpaceUserService extends IService<SpaceUser> {

    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    //获取成员分装类
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    //查询封装类
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    void validSpaceUser(SpaceUser spaceUser, boolean add);
}
