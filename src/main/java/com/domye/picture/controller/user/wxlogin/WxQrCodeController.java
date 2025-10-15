package com.domye.picture.controller.user.wxlogin;

import com.domye.picture.common.BaseResponse;
import com.domye.picture.common.Result;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.service.user.UserService;
import com.domye.picture.service.user.WxCodeService;
import com.domye.picture.service.user.WxPublicService;
import com.domye.picture.service.user.WxQrService;
import com.domye.picture.service.user.model.entity.User;
import com.domye.picture.service.user.model.vo.QrcodeStatusVO;
import com.domye.picture.service.user.model.vo.QrcodeVO;
import com.domye.picture.utils.WxQrCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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

    @Resource
    private WxCodeService wxCodeService;

    @Resource
    private WxQrService wxQrService;

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成微信公众号二维码
     * @param request HTTP请求
     * @return 包含二维码信息的响应
     */
    @GetMapping("/generate")
    public BaseResponse<QrcodeVO> generateQrCode(HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (Exception e) {
            log.warn("用户未登录，将生成通用登录二维码");
        }

        // 生成随机场景值ID
        int sceneId = new Random().nextInt(900000) + 100000; // 生成6位随机数
        log.info("生成随机场景值ID: sceneId={}", sceneId);

        // 创建临时二维码，有效期5分钟（300秒）
        String ticket = wxQrCodeUtil.createTempQrCode(sceneId, 300);

        if (ticket == null) {
            log.error("生成二维码失败: sceneId={}", sceneId);
            return Result.error(ErrorCode.OPERATION_ERROR, "生成二维码失败");
        }

        // 获取二维码图片URL
        String qrCodeUrl = wxQrCodeUtil.getQrCodeImageUrl(ticket);

        QrcodeVO qrcodeVO = new QrcodeVO();
        qrcodeVO.setUrl(qrCodeUrl);
        qrcodeVO.setSceneId(sceneId);
        qrcodeVO.setTicket(ticket);

        // 如果用户已登录，将sceneId与用户ID关联存储到Redis中
        if (loginUser != null) {
            String sceneUserKey = "qr_scene_user:" + sceneId;
            stringRedisTemplate.opsForValue().set(sceneUserKey, String.valueOf(loginUser.getId()), 30, TimeUnit.MINUTES);
            log.info("生成微信公众号二维码成功: sceneId={}, ticket={}, userId={}", sceneId, ticket, loginUser.getId());
        } else {
            log.info("生成微信公众号二维码成功（未登录用户）: sceneId={}, ticket={}", sceneId, ticket);
        }

        return Result.success(qrcodeVO);
    }


    /**
     * 检查二维码扫描状态
     * @param sceneId 场景值ID
     * @return 扫描状态信息
     */
    @GetMapping("/status/{sceneId}")
    public BaseResponse<QrcodeStatusVO> checkQrStatus(@PathVariable int sceneId) {

        String scanStatus = wxQrService.getQrScanStatus(String.valueOf(sceneId));
        log.info("检查二维码扫描状态: sceneId={}, scanStatus={}", sceneId, scanStatus);

        QrcodeStatusVO result = new QrcodeStatusVO();
        result.setSceneId(sceneId);

        if (scanStatus == null) {
            result.setStatus("waiting");
        } else if (scanStatus.startsWith("scanned:")) {
            result.setStatus("scanned");
            String openId = scanStatus.substring(8); // 修正索引，scanned:后面是openId
            result.setOpenId(openId);
            log.info("二维码已被扫描: sceneId={}, openId={}", sceneId, openId);

            String code = wxCodeService.handleVerifyRequest(openId, true, String.valueOf(sceneId));
            log.info("为用户生成验证码: openId={}, code={}", openId, code);
            if (code != null) {
                result.setCode(code);
                result.setMessage("验证码已生成，请在微信中查看");

                // 存储验证码与sceneId的关联关系
                wxCodeService.storeCodeSceneIdRelation(code, String.valueOf(sceneId));
                log.info("验证码与sceneId关联关系已存储: code={}, sceneId={}", code, sceneId);
            } else {
                log.error("生成验证码失败: openId={}", openId);
            }
        } else {
            result.setStatus(scanStatus);
        }

        log.info("检查二维码状态结果: sceneId={}, status={}", sceneId, result.getStatus());
        return Result.success(result);
    }
}