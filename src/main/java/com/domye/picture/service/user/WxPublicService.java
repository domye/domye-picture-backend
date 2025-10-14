package com.domye.picture.service.user;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

public interface WxPublicService {
    /**
     * 获取微信token
     * @return 微信token
     */
    String getToken();
    
    /**
     * 生成二维码
     * @param sceneId 场景ID
     * @return 二维码ID
     */
    String generateQrCode(int sceneId);
    
    /**
     * 更新二维码扫描状态
     * @param qrCodeId 二维码ID
     * @param openId 用户openId
     * @return 更新是否成功
     */
    boolean updateQrScanStatus(String qrCodeId, String openId);
    
    /**
     * 获取二维码扫描状态
     * @param qrCodeId 二维码ID
     * @return 扫描状态
     */
    String getQrScanStatus(String qrCodeId);
    
    String handleLoginRequest(String openId, String nickName, String avatarUrl);

    boolean verifyCode(String openId, String code);

    String generateUniqueCode();

    boolean checkSignature(String signature, String timestamp, String nonce);

    String getRequestBody(HttpServletRequest request) throws IOException;

    String decryptWeChatMessage(String encryptMsg);

    String handleMessage(Map<String, String> msgMap);
}
