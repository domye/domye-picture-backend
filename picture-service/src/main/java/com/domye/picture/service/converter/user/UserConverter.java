package com.domye.picture.service.converter.user;

import com.domye.picture.core.model.user.User;
import com.domye.picture.service.dto.command.user.UserRegisterCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserConverter {

    public abstract User fromCommand(UserRegisterCommand command);

}
