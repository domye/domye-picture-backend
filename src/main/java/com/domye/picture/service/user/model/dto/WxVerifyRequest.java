package com.domye.picture.service.user.model.dto;

import lombok.Data;

@Data
public class WxVerifyRequest {
    private String signature;

    private String timestamp;

    private String nonce;

    private String echostr;
}