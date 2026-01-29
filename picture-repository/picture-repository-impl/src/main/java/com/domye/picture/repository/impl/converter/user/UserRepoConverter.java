package com.domye.picture.repository.impl.converter.user;

import com.domye.picture.core.model.user.User;
import com.domye.picture.repository.impl.entity.user.UserDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.util.DigestUtils;

@Mapper(componentModel = "spring")
public interface UserRepoConverter {

    @Mapping(target = "userPassword", qualifiedByName = "encryptPassword")
    UserDO toDO(User user);

    @Named("encryptPassword")
    default String encryptPassword(String password) {
        final String SALT = "domye";
        return DigestUtils.md5DigestAsHex((SALT + password).getBytes());
    }

    User fromDO(UserDO user);
}
