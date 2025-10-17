package com.domye.picture.service.WxMessage.model.vo;

import lombok.Data;

@Data
public class QrcodeStatusVO {
    /**
     * 场景值ID
     */
    private Integer sceneId;

    /**
     * 扫描状态：waiting(等待扫描)、scanned(已扫描)、confirmed(已确认)
     */
    private String status;

    /**
     * 微信OpenID（仅在status为scanned时有值）
     */
    private String openId;

    /**
     * 验证码（仅在status为scanned时有值）
     */
    private String code;

    /**
     * 提示信息
     */
    private String message;
}
