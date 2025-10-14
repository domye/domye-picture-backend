package com.domye.picture.controller.user.wxlogin;

import com.domye.picture.service.user.WxPublicService;
import com.domye.picture.utils.WxQrCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 微信二维码相关接口
 */
@Slf4j
@RestController
@RequestMapping("/wx/qr")
public class WxQrCodeController {

    @Resource
    private WxPublicService wxPublicService;

    @Resource
    private WxQrCodeUtil wxQrCodeUtil;

    /**
     * 生成微信公众号二维码
     * @return 包含二维码信息的响应
     */
    @GetMapping("/generate")
    public Map<String, Object> generateQrCode() {
        try {
            // 生成随机场景值ID
            int sceneId = new Random().nextInt(900000) + 100000; // 生成6位随机数

            // 创建临时二维码，有效期5分钟（300秒）
            String ticket = wxQrCodeUtil.createTempQrCode(sceneId, 300);

            if (ticket == null) {
                log.error("生成微信二维码失败");
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("code", 500);
                errorResult.put("message", "生成二维码失败");
                return errorResult;
            }

            // 获取二维码图片URL
            String qrCodeUrl = wxQrCodeUtil.getQrCodeImageUrl(ticket);

            // 返回二维码信息
            Map<String, Object> result = new HashMap<>();
            result.put("sceneId", sceneId);
            result.put("ticket", ticket);
            result.put("qrCodeUrl", qrCodeUrl);

            log.info("生成微信公众号二维码成功: sceneId={}, ticket={}", sceneId, ticket);

            return result;
        } catch (Exception e) {
            log.error("生成微信公众号二维码失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", 500);
            errorResult.put("message", "生成二维码失败");
            return errorResult;
        }
    }

    /**
     * 检查二维码扫描状态
     * @param sceneId 场景值ID
     * @return 扫描状态信息
     */
    @GetMapping("/status/{sceneId}")
    public Map<String, Object> checkQrStatus(@PathVariable int sceneId) {
        try {
            // 这里应该从Redis或其他存储中获取扫描状态
            // 模拟返回状态
            Map<String, Object> result = new HashMap<>();
            result.put("sceneId", sceneId);
            result.put("status", "waiting"); // waiting, scanned, confirmed

            log.info("检查二维码状态: sceneId={}, status={}", sceneId, result.get("status"));

            return result;
        } catch (Exception e) {
            log.error("检查二维码状态失败: sceneId={}", sceneId, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", 500);
            errorResult.put("message", "检查二维码状态失败");
            return errorResult;
        }
    }

    /**
     * 获取微信配置信息
     * @return 微信配置信息
     */
    @GetMapping("/config")
    public Map<String, Object> getWxConfig() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("token", wxPublicService.getToken());
            result.put("message", "获取微信配置成功");

            log.info("获取微信配置: token={}", result.get("token"));

            return result;
        } catch (Exception e) {
            log.error("获取微信配置失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", 500);
            errorResult.put("message", "获取微信配置失败");
            return errorResult;
        }
    }
}
