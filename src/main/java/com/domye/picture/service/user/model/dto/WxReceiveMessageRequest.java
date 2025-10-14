package com.domye.picture.service.user.model.dto;

import lombok.Data;

@Data
public class WxReceiveMessageRequest {
    String signature;
    String timestamp;
    String nonce;
}
