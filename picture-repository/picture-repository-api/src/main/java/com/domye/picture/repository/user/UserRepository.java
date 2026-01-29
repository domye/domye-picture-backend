package com.domye.picture.repository.user;

import com.domye.picture.core.model.user.User;

public interface UserRepository{
    long getUserCount(String UserAccount);

    boolean save(User user);

    User getUser(String userAccount, String encryptPassword);

   User getById(long id);

    void removeById(Long id);
}
