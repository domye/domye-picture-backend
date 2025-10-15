package com.domye.picture.service.user;

import com.domye.picture.manager.wxlogin.BaseWxMsgResVo;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

public interface WxPublicService {
    /**
     * 获取微信token
     * @return 微信token
     */
    String getToken();


    boolean checkSignature(String signature, String timestamp, String nonce);

    String getRequestBody(HttpServletRequest request) throws IOException;

    BaseWxMsgResVo handleMessage(Map<String, String> msgMap, HttpServletRequest request);


}
