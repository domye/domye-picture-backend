package com.domye.picture.service.WxMessage.model.dto;

import lombok.Data;

@Data
public class WxReceiveMessageRequest {
    String signature;
    String timestamp;
    String nonce;
}
