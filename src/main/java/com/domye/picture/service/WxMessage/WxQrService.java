package com.domye.picture.service.WxMessage;

public interface WxQrService {

    boolean updateQrScanStatus(String qrCodeId, String openId);

    String getQrScanStatus(String qrCodeId);

}
