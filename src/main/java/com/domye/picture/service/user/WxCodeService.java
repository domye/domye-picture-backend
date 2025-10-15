package com.domye.picture.service.user;

public interface WxCodeService {

    Boolean findCodeType(String code);

    String handleVerifyRequest(String openId, Boolean type, String sceneId);

    boolean verifyCode(String openId, String code, Boolean type);

    String generateUniqueCode();

    String findOpenIdByCode(String code);

    void storeCodeSceneIdRelation(String code, String sceneId);
}