package com.domye.picture.constant;

public interface WxConstant {
    // 缓存 Key 常量
    String WX_LOGIN_CODE_KEY = "login:code:";
    String WX_BIND_CODE_KEY = "bind:code:";
    String WX_OPENID_TO_CODE_KEY = "openid_to_code:";
    String WX_QR_CODE_KEY = "qr:code:";
    String WX_QR_SCAN_STATUS_KEY = "qr:scan:";
    int CODE_EXPIRE_TIME = 5; // 5 分钟
}
