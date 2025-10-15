package com.domye.picture.service.user.wxlogin;

import com.domye.picture.service.user.WxQrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.domye.picture.constant.WxConstant.WX_QR_CODE_KEY;
import static com.domye.picture.constant.WxConstant.WX_QR_SCAN_STATUS_KEY;

@Service
@Slf4j
public class WxQrServiceImpl implements WxQrService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成二维码
     * @param sceneId 场景ID
     * @return 二维码ID
     */
    @Override
    public String generateQrCode(int sceneId) {
        // 生成唯一二维码ID
        String qrCodeId = UUID.randomUUID().toString().replace("-", "");

        // 存储二维码信息
        String qrCodeKey = WX_QR_CODE_KEY + qrCodeId;
        String qrScanStatusKey = WX_QR_SCAN_STATUS_KEY + qrCodeId;

        // 存储二维码信息到Redis，30分钟过期
        stringRedisTemplate.opsForValue().set(qrCodeKey, String.valueOf(sceneId), 30, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(qrScanStatusKey, "waiting", 30, TimeUnit.MINUTES);

        log.info("生成二维码成功: qrCodeId={}, sceneId={}", qrCodeId, sceneId);
        return qrCodeId;
    }

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
        stringRedisTemplate.opsForValue().set(qrScanStatusKey, scanStatus, 30, TimeUnit.MINUTES);

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
        String status = stringRedisTemplate.opsForValue().get(qrScanStatusKey);
        log.info("获取二维码扫描状态: qrCodeId={}, qrScanStatusKey={}, status={}", qrCodeId, qrScanStatusKey, status);
        return status;
    }

    /**
     * 获取场景ID对应的二维码状态
     */
    @Override
    public String getQrScanStatusBySceneId(String sceneId) {
        // 根据场景ID查找对应的二维码状态
        // 在实际应用中，这里可能需要从数据库或其他存储中查找
        // 这里简化处理，直接返回Redis中的状态
        String qrScanStatusKey = WX_QR_SCAN_STATUS_KEY + sceneId;
        String status = stringRedisTemplate.opsForValue().get(qrScanStatusKey);
        log.info("获取场景ID对应的二维码状态: sceneId={}, qrScanStatusKey={}, status={}", sceneId, qrScanStatusKey, status);
        return status;
    }

}
