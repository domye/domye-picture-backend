package com.domye.picture.model.mapper.user;

import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.vo.user.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserStructMapper {
    UserVO toUserVo(User user);
}
