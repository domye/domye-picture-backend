package com.domye.picture.service.WxMessage.impl;

import com.domye.picture.helper.RedisUtil;
import com.domye.picture.service.WxMessage.WxQrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.domye.picture.constant.WxConstant.WX_QR_SCAN_STATUS_KEY;

@Service
@Slf4j
public class WxQrServiceImpl implements WxQrService {


    /**
     * 更新二维码扫描状态
     * @param qrCodeId 二维码ID
     * @param openId   用户openId
     * @return 更新是否成功
     */
    @Override
    public boolean updateQrScanStatus(String qrCodeId, String openId) {

        String qrScanStatusKey = WX_QR_SCAN_STATUS_KEY + qrCodeId;
        String scanStatus = "scanned:" + openId;

        // 更新扫描状态
        RedisUtil.setWithExpire(qrScanStatusKey, scanStatus, 30, TimeUnit.MINUTES);

        log.info("二维码扫描状态已更新: qrCodeId={}, openId={}, qrScanStatusKey={}", qrCodeId, openId, qrScanStatusKey);
        return true;

    }

    /**
     * 获取二维码扫描状态
     * @param qrCodeId 二维码ID
     * @return 扫描状态
     */
    @Override
    public String getQrScanStatus(String qrCodeId) {
        String qrScanStatusKey = WX_QR_SCAN_STATUS_KEY + qrCodeId;
        String status = RedisUtil.get(qrScanStatusKey);
        log.info("获取二维码扫描状态: qrCodeId={}, qrScanStatusKey={}, status={}", qrCodeId, qrScanStatusKey, status);
        return status;
    }

}
