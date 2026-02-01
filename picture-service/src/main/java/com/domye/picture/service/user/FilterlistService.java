package com.domye.picture.service.user;

import com.domye.picture.model.user.vo.UserVO;

import java.util.List;

public interface FilterlistService {

    boolean isInFilterList(Long userId, Long type, Long mode);

    List<UserVO> queryAllFilterListUsers(Long type, Long mode);

    void addUserToFilterList(Long userId, Long type, Long mode);

    void removeUserFromFilterList(Long userId, Long type, Long mode);

}

