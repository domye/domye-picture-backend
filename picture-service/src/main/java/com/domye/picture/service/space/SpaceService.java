package com.domye.picture.service.space;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.space.dto.SpaceAddRequest;
import com.domye.picture.model.space.dto.SpaceQueryRequest;
import com.domye.picture.model.space.entity.Space;
import com.domye.picture.model.space.vo.SpaceVO;
import com.domye.picture.model.user.entity.User;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Domye
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-09-09 19:12:43
 */
public interface SpaceService extends IService<Space> {
    public Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    public void fillSpace(Space space);

    public void validSpace(Space space, boolean add);

    void checkSpaceAuth(User loginUser, Space space);

    void deleteSpace(long id, User loginUser);

    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);
}
