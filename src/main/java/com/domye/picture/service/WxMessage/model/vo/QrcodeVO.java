package com.domye.picture.service.WxMessage.model.vo;

import lombok.Data;

@Data
public class QrcodeVO {
    Integer sceneId;
    String ticket;
    String url;
    String code;
}
