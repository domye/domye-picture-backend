package com.domye.picture.service.WxMessage;

public interface WxCodeService {

    Boolean findCodeType(String code);

    String handleVerifyRequest(String openId, Boolean type, String sceneId);

    boolean verifyCode(String openId, String code, Boolean type);

    String generateUniqueCode();

    void storeCodeSceneIdRelation(String code, String sceneId);
}