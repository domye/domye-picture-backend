package com.domye.picture.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.dto.Space.SpaceAddRequest;
import com.domye.picture.model.entity.Space;
import com.domye.picture.model.entity.User;

/**
 * @author Domye
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-09-09 19:12:43
 */
public interface SpaceService extends IService<Space> {
    public Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    public void fillSpace(Space space);

    public void validSpace(Space space, boolean add);
}
