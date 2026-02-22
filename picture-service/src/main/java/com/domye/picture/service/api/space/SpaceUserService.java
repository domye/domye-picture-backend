package com.domye.picture.service.api.space;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.dto.space.SpaceUserAddRequest;
import com.domye.picture.model.dto.space.SpaceUserQueryRequest;
import com.domye.picture.model.entity.space.SpaceUser;
import com.domye.picture.model.vo.space.SpaceUserVO;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Domye
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-09-20 10:51:20
 */
public interface SpaceUserService extends IService<SpaceUser> {

    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 获取成员封装类（单条）
     * @deprecated 存在 N+1 查询风险，请使用 {@link #getSpaceUserVOList(List)} 批量方法
     * @param spaceUser 空间用户
     * @param request HTTP请求
     * @return 封装对象
     */
    @Deprecated
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    //查询封装类
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    void validSpaceUser(SpaceUser spaceUser, boolean add);
}
