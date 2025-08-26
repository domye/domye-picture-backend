package com.domye.picture.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.mapper.UserMapper;
import com.domye.picture.model.entity.User;
import com.domye.picture.service.UserService;
import org.springframework.stereotype.Service;

/**
 * @author Domye
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-08-26 15:00:43
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

}




