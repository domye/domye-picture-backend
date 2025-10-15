package com.domye.picture.service.user;

public interface WxQrService {
    String generateQrCode(int sceneId);

    boolean updateQrScanStatus(String qrCodeId, String openId);

    String getQrScanStatus(String qrCodeId);

    String getQrScanStatusBySceneId(String sceneId);
}
