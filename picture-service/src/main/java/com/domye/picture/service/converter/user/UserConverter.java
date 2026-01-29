package com.domye.picture.service.converter.user;

import com.domye.picture.core.model.user.User;
import com.domye.picture.service.dto.command.user.UserRegisterCommand;
import com.domye.picture.service.dto.presentation.user.UserVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserConverter {

    User fromCommand(UserRegisterCommand command);

    UserVO toVO(User user);
}
