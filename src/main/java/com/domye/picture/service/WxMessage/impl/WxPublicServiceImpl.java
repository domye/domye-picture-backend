package com.domye.picture.service.WxMessage.impl;

import com.domye.picture.helper.RedisUtil;
import com.domye.picture.helper.WxMsgUtil;
import com.domye.picture.manager.wxlogin.BaseWxMsgResVo;
import com.domye.picture.service.WxMessage.WxCodeService;
import com.domye.picture.service.WxMessage.WxPublicService;
import com.domye.picture.service.WxMessage.WxQrService;
import com.domye.picture.service.user.UserService;
import com.domye.picture.service.user.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Service
@Slf4j
public class WxPublicServiceImpl implements WxPublicService {


    @Value("${wx.token}")
    private String wxToken;

    @Resource
    private UserService userService;

    @Resource
    private WxCodeService wxCodeService;

    @Resource
    private WxQrService wxQrService;

    /**
     * 获取微信token
     * @return 微信token
     */
    @Override
    public String getToken() {
        return wxToken;
    }


    /**
     * 验证微信签名
     */
    @Override
    public boolean checkSignature(String signature, String timestamp, String nonce) {
        // 1. 将token、timestamp、nonce三个参数进行字典序排序
        String[] arr = new String[]{wxToken, timestamp, nonce};
        Arrays.sort(arr);

        // 2. 将三个参数字符串拼接成一个字符串进行sha1加密
        StringBuilder sb = new StringBuilder();
        for (String anArr : arr) {
            sb.append(anArr);
        }
        String sha1 = WxMsgUtil.sha1(sb.toString());

        // 3. 开发者获得加密后的字符串可与signature对比
        return sha1 != null && sha1.equals(signature);
    }

    /**
     * 从请求中读取XML数据
     */
    @Override
    public String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }


    /**
     * 处理微信消息
     */
    @Override
    public BaseWxMsgResVo handleMessage(Map<String, String> msgMap, HttpServletRequest request) {
        String fromUserName = msgMap.get("FromUserName");
        String toUserName = msgMap.get("ToUserName");
        String msgType = msgMap.get("MsgType");
        String content = msgMap.get("Content");

        log.info("处理微信消息: fromUserName={}, toUserName={}, msgType={}, content={}",
                fromUserName, toUserName, msgType, content);

        BaseWxMsgResVo response = new BaseWxMsgResVo();
        response.setToUserName(fromUserName); // 注意：这里需要交换发送者和接收者，因为我们要回复用户
        response.setFromUserName(toUserName);
        response.setCreateTime(System.currentTimeMillis() / 1000);
        response.setMsgType("text");

        // 检查必要字段是否存在
        if (StringUtils.isEmpty(fromUserName) || StringUtils.isEmpty(toUserName) || StringUtils.isEmpty(msgType)) {
            log.error("微信消息缺少必要字段: fromUserName={}, toUserName={}, msgType={}",
                    fromUserName, toUserName, msgType);
            response.setContent("消息格式错误，请重试。");
            return response;
        }

        // 处理文本消息
        if ("text".equals(msgType)) {
            Boolean codeType = wxCodeService.findCodeType(content);
            if (codeType == null) {
                // 验证码不存在，返回提示信息而不是抛出异常
                response.setContent("验证码不存在或已过期，请重新获取。");
            } else if (codeType) {
                // 处理登录验证码
                boolean loginResult = wxCodeService.verifyCode(fromUserName, content, true);
                if (loginResult) {
                    userService.loginByWx(fromUserName, request);
                    response.setContent("登录成功！");
                } else {
                    response.setContent("验证码错误，请重试。");
                }
            } else {
                // 处理绑定验证码
                boolean bindResult = wxCodeService.verifyCode(fromUserName, content, false);
                if (bindResult) {
                    // 验证码正确，通过sceneId查找用户ID
                    String sceneId = getSceneIdFromCode(content);
                    log.info("通过验证码获取sceneId: code={}, sceneId={}", content, sceneId);
                    if (sceneId != null) {
                        String userIdStr = RedisUtil.get("qr_scene_user:" + sceneId);
                        log.info("通过sceneId获取用户ID: sceneId={}, userIdStr={}", sceneId, userIdStr);
                        if (userIdStr != null) {
                            try {
                                Long userId = Long.valueOf(userIdStr);
                                User user = userService.getById(userId);
                                if (user != null) {
                                    // 检查用户是否已经绑定了微信
                                    if (user.getWxOpenId() != null && !user.getWxOpenId().isEmpty()) {
                                        response.setContent("该账号已经绑定了微信！");
                                    } else {
                                        // 执行绑定操作
                                        userService.bindWx(fromUserName, userId);
                                        response.setContent("微信绑定成功！");
                                        log.info("微信绑定成功: userId={}, openId={}", userId, fromUserName);
                                    }
                                } else {
                                    response.setContent("绑定失败，用户不存在。");
                                }
                            } catch (Exception e) {
                                log.error("绑定微信时发生错误: userIdStr={}, error={}", userIdStr, e.getMessage());
                                response.setContent("绑定失败，请重试。");
                            }
                        } else {
                            // 如果通过sceneId找不到用户，尝试通过openId查找用户（用户可能已经绑定过）
                            User existingUser = userService.findByOpenId(fromUserName);
                            if (existingUser != null) {
                                response.setContent("您的微信已经绑定过账号了！");
                            } else {
                                response.setContent("绑定失败，未找到用户信息。");
                            }
                        }
                    } else {
                        response.setContent("绑定失败，验证码无效。");
                    }
                } else {
                    response.setContent("验证码错误，请重试。");
                }
            }
        }
        // 处理事件消息
        else if ("event".equals(msgType)) {
            String event = msgMap.get("Event");
            String eventKey = msgMap.get("EventKey");

            log.info("处理微信事件: event={}, eventKey={}", event, eventKey);

            if ("SCAN".equals(event)) {
                log.info("用户扫描二维码: fromUserName={}, eventKey={}", fromUserName, eventKey);
                String sceneId = eventKey.replace("qrscene_", "");
                log.info("解析sceneId: eventKey={}, sceneId={}", eventKey, sceneId);

                boolean updateResult = wxQrService.updateQrScanStatus(sceneId, fromUserName);
                if (updateResult) {
                    log.info("二维码扫码状态已更新: sceneId={}, fromUserName={}", sceneId, fromUserName);
                    scanHandle(sceneId, fromUserName, request);
                    response.setContent("扫码成功！");
                } else {
                    log.error("更新二维码扫码状态失败: sceneId={}, fromUserName={}", sceneId, fromUserName);
                    response.setContent("扫码状态更新失败，请重试。");
                }
            } else {
                response.setContent("收到事件消息，感谢您的关注！");
            }
        } else {
            response.setContent("收到消息，请回复login获取验证码。");
        }

        return response;
    }

    /**
     * 处理扫码事件
     * @param sceneId      场景ID
     * @param fromUserName 用户OpenID
     * @param request      HTTP请求
     */
    private void scanHandle(String sceneId, String fromUserName, HttpServletRequest request) {
        log.info("处理扫码事件: sceneId={}, fromUserName={}", sceneId, fromUserName);

        // 通过sceneId获取用户信息
        // sceneId应该是生成二维码时与用户关联的标识
        // 这里需要根据实际业务逻辑实现，例如从Redis中获取sceneId对应的用户ID
        User user = null;
        try {
            // 从Redis中获取sceneId对应的用户ID
            String userIdKey = "qr_scene_user:" + sceneId;
            String userIdStr = RedisUtil.get(userIdKey);
            log.info("通过sceneId获取用户ID: sceneId={}, userIdKey={}, userIdStr={}", sceneId, userIdKey, userIdStr);

            if (userIdStr != null) {
                Long userId = Long.valueOf(userIdStr);
                user = userService.getById(userId);
            }
        } catch (Exception e) {
            log.error("通过sceneId获取用户信息失败: sceneId={}, error={}", sceneId, e.getMessage());
        }

        if (user != null) {
            // 用户已登录，判断是否已存在绑定微信
            if (user.getWxOpenId() != null) {
                // 已绑定微信，返回提示
                log.info("用户已绑定微信: userId={}, wxOpenId={}", user.getId(), user.getWxOpenId());
            } else {
                // 未绑定微信，生成验证码让其绑定微信
                String code = wxCodeService.handleVerifyRequest(fromUserName, false, sceneId);
                log.info("为已登录用户生成绑定验证码: userId={}, fromUserName={}, code={}",
                        user.getId(), fromUserName, code);
            }
        } else {
            // 用户未登录或sceneId无效，生成登录验证码
            String code = wxCodeService.handleVerifyRequest(fromUserName, true, sceneId);
            log.info("为未登录用户生成登录验证码: fromUserName={}, code={}", fromUserName, code);
        }
    }


    /**
     * 通过验证码获取sceneId
     * @param code 验证码
     * @return sceneId
     */
    private String getSceneIdFromCode(String code) {
        try {
            // 从Redis中查找验证码对应的sceneId
            String codeToSceneIdKey = "code_to_scene_id:" + code;
            String sceneId = RedisUtil.get(codeToSceneIdKey);
            log.info("通过验证码获取sceneId: code={}, codeToSceneIdKey={}, sceneId={}", code, codeToSceneIdKey, sceneId);
            return sceneId;
        } catch (Exception e) {
            log.error("通过验证码获取sceneId失败: code={}, error={}", code, e.getMessage());
            return null;
        }
    }
}