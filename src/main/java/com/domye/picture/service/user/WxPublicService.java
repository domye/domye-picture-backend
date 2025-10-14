package com.domye.picture.service.user;

public interface WxPublicService {
    String handleLoginRequest(String openId, String nickName, String avatarUrl);
}
