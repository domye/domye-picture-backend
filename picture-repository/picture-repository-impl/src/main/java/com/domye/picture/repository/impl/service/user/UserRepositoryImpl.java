package com.domye.picture.repository.impl.service.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.domye.picture.core.model.user.User;
import com.domye.picture.repository.impl.converter.user.UserRepoConverter;
import com.domye.picture.repository.impl.entity.user.UserDO;
import com.domye.picture.repository.impl.mapper.UserMapper;
import com.domye.picture.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserMapper userMapper;
    private final UserRepoConverter userRepoConverter;

    @Override
    public long getUserCount(String UserAccount) {
        QueryWrapper<UserDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", UserAccount);
        long count = userMapper.selectCount(queryWrapper);
        return count;
    }

    @Override
    public boolean save(User user) {
        UserDO userDO = userRepoConverter.toDO(user);
        userMapper.insert(userDO);
        user.setId(userDO.getId());
        return true;
    }

    @Override
    public User getUser(String userAccount, String encryptPassword) {
        //查询数据是否存在
        QueryWrapper<UserDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount).eq("userPassword", encryptPassword);
        UserDO user = userMapper.selectOne(queryWrapper);
        return userRepoConverter.fromDO(user);
    }
}
