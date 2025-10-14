package com.domye.picture.service.user.wxlogin;

import com.domye.picture.service.user.WxPublicService;
import com.domye.picture.utils.WxMessageCrypt;
import com.domye.picture.utils.WxMsgUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WxPublicServiceImpl implements WxPublicService {

    /**
     * 获取微信token
     * @return 微信token
     */
    @Override
    public String getToken() {
        return wxToken;
    }

    // 缓存 Key 常量
    private final static String WX_LOGIN_CODE_KEY = "login:code:";
    private final static String WX_LOGIN_TOKEN_KEY = "login:token:";
    private final static String WX_OPENID_TO_CODE_KEY = "openid_to_code:";
    private final static String WX_QR_CODE_KEY = "qr:code:";
    private final static String WX_QR_SCAN_STATUS_KEY = "qr:scan:";
    private final static int CODE_EXPIRE_TIME = 5; // 5 分钟
    private final static int TOKEN_EXPIRE_TIME = 24 * 60; // 24 小时
    @Value("${wx.token}")
    private String wxToken;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private WxMessageCrypt wxMessageCrypt;

    /**
     * 处理登录请求，生成验证码
     */
    @Override
    public String handleLoginRequest(String openId, String nickName, String avatarUrl) {
        try {
            if (StringUtils.isEmpty(openId)) {
                log.error("处理登录请求失败，openId为空");
                return null;
            }

            log.info("处理用户登录请求: openId={}, nickName={}, avatarUrl={}", openId, nickName, avatarUrl);

            // 检查是否已有未过期的验证码
            String existingCodeKey = WX_OPENID_TO_CODE_KEY + openId;
            String existingCode = stringRedisTemplate.opsForValue().get(existingCodeKey);

            if (StringUtils.isNotEmpty(existingCode)) {
                log.info("用户已有未过期的验证码，返回现有验证码: {}", existingCode);
                return existingCode;
            }

            // 生成唯一验证码
            String code = generateUniqueCode();
            log.info("为用户生成新验证码: openId={}, code={}", openId, code);

            // 双向绑定存储
            String codeToOpenIdKey = WX_LOGIN_CODE_KEY + code;
            String openIdToCodeKey = WX_OPENID_TO_CODE_KEY + openId;

            stringRedisTemplate.opsForValue().set(codeToOpenIdKey, openId, CODE_EXPIRE_TIME, TimeUnit.MINUTES);
            stringRedisTemplate.opsForValue().set(openIdToCodeKey, code, CODE_EXPIRE_TIME, TimeUnit.MINUTES);
            log.info("验证码已存储到Redis: codeToOpenIdKey={}, openIdToCodeKey={}", codeToOpenIdKey, openIdToCodeKey);

            // 存储用户信息用于后续创建用户
            if (StringUtils.isNotEmpty(nickName) || StringUtils.isNotEmpty(avatarUrl)) {
                String userInfoKey = "login:userinfo:" + openId;
                String userInfo = (nickName != null ? nickName : "") + "|" +
                        (avatarUrl != null ? avatarUrl : "");
                stringRedisTemplate.opsForValue().set(userInfoKey, userInfo, CODE_EXPIRE_TIME, TimeUnit.MINUTES);
                log.info("用户信息已存储到Redis: userInfoKey={}, userInfo={}", userInfoKey, userInfo);
            }

            return code;
        } catch (Exception e) {
            log.error("处理用户登录请求失败: openId={}", openId, e);
            return null;
        }
    }

    /**
     * 生成二维码
     * @param sceneId 场景ID
     * @return 二维码ID
     */
    public String generateQrCode(int sceneId) {
        try {
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
        } catch (Exception e) {
            log.error("生成二维码失败", e);
            return null;
        }
    }
    
    /**
     * 更新二维码扫描状态
     * @param qrCodeId 二维码ID
     * @param openId 用户openId
     * @return 更新是否成功
     */
    public boolean updateQrScanStatus(String qrCodeId, String openId) {
        try {
            String qrScanStatusKey = WX_QR_SCAN_STATUS_KEY + qrCodeId;
            String scanStatus = "scanned:" + openId;
            
            // 更新扫描状态
            stringRedisTemplate.opsForValue().set(qrScanStatusKey, scanStatus, 30, TimeUnit.MINUTES);
            
            log.info("二维码扫描状态已更新: qrCodeId={}, openId={}", qrCodeId, openId);
            return true;
        } catch (Exception e) {
            log.error("更新二维码扫描状态失败: qrCodeId={}, openId={}", qrCodeId, openId, e);
            return false;
        }
    }
    
    /**
     * 获取二维码扫描状态
     * @param qrCodeId 二维码ID
     * @return 扫描状态
     */
    public String getQrScanStatus(String qrCodeId) {
        try {
            String qrScanStatusKey = WX_QR_SCAN_STATUS_KEY + qrCodeId;
            return stringRedisTemplate.opsForValue().get(qrScanStatusKey);
        } catch (Exception e) {
            log.error("获取二维码扫描状态失败: qrCodeId={}", qrCodeId, e);
            return null;
        }
    }
    
    /**
     * 验证验证码是否正确
     * @param openId 用户openId
     * @param code   用户输入的验证码
     * @return 验证结果，true表示验证成功
     */
    @Override
    public boolean verifyCode(String openId, String code) {
        try {
            if (StringUtils.isEmpty(openId) || StringUtils.isEmpty(code)) {
                log.error("验证码验证失败，参数为空: openId={}, code={}", openId, code);
                return false;
            }

            // 获取openId对应的验证码
            String existingCodeKey = WX_OPENID_TO_CODE_KEY + openId;
            String storedCode = stringRedisTemplate.opsForValue().get(existingCodeKey);

            if (StringUtils.isEmpty(storedCode)) {
                log.error("验证码验证失败，未找到验证码: openId={}, code={}", openId, code);
                return false;
            }

            // 验证码比较（不区分大小写）
            boolean isValid = storedCode.equalsIgnoreCase(code.trim());
            log.info("验证码验证结果: openId={}, code={}, storedCode={}, isValid={}",
                    openId, code, storedCode, isValid);

            // 验证成功后，删除验证码（防止重复使用）
            if (isValid) {
                String codeToOpenIdKey = WX_LOGIN_CODE_KEY + storedCode;
                stringRedisTemplate.delete(existingCodeKey);
                stringRedisTemplate.delete(codeToOpenIdKey);
                log.info("验证码验证成功，已删除验证码缓存: openId={}, code={}", openId, storedCode);
            }

            return isValid;
        } catch (Exception e) {
            log.error("验证码验证异常: openId={}, code={}", openId, code, e);
            return false;
        }
    }

    /**
     * 生成唯一验证码
     * @return 验证码
     */
    @Override
    public String generateUniqueCode() {
        // 生成6位数字验证码
        String code = String.format("%06d", (int) ((Math.random() * 9 + 1) * 100000));
        log.debug("生成验证码: {}", code);
        return code;
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
     * 解密微信消息
     * @param encryptMsg 加密的消息
     * @return 解密后的消息
     */
    @Override
    public String decryptWeChatMessage(String encryptMsg) {
        return wxMessageCrypt.decryptWeChatMessage(encryptMsg);
    }

    /**
     * 处理微信消息
     */
    @Override
    public String handleMessage(Map<String, String> msgMap) {
        try {
            String fromUserName = msgMap.get("FromUserName");
            String toUserName = msgMap.get("ToUserName");
            String msgType = msgMap.get("MsgType");
            String content = msgMap.get("Content");

            log.info("处理微信消息: fromUserName={}, toUserName={}, msgType={}, content={}",
                    fromUserName, toUserName, msgType, content);

            // 检查必要字段是否存在
            if (StringUtils.isEmpty(fromUserName) || StringUtils.isEmpty(toUserName) || StringUtils.isEmpty(msgType)) {
                log.error("微信消息缺少必要字段: fromUserName={}, toUserName={}, msgType={}",
                        fromUserName, toUserName, msgType);
                return WxMsgUtil.buildTextMsg(toUserName, fromUserName, "消息格式错误，请重试。");
            }

            // 处理文本消息
            if ("text".equals(msgType)) {
                // 用户输入"login"获取验证码
                if ("login".equalsIgnoreCase(content.trim())) {
                    log.info("用户请求登录验证码: {}", fromUserName);
                    String code = handleLoginRequest(fromUserName, null, null);
                    if (code != null) {
                        log.info("为用户生成验证码成功: {}, code={}", fromUserName, code);
                        return WxMsgUtil.buildTextMsg(toUserName, fromUserName, "您的验证码是：" + code + "，请在3分钟内使用该验证码完成登录。");
                    } else {
                        log.error("为用户生成验证码失败: {}", fromUserName);
                        return WxMsgUtil.buildTextMsg(toUserName, fromUserName, "生成验证码失败，请重试。");
                    }
                }
                // 用户输入其他内容，认为是验证码
                else {
                    log.info("用户输入验证码: {}, content={}", fromUserName, content);
                    // 验证码验证逻辑
                    boolean isCodeValid = verifyCode(fromUserName, content);
                    if (isCodeValid) {
                        return WxMsgUtil.buildTextMsg(toUserName, fromUserName, "验证成功！您的账号已绑定微信。");
                    } else {
                        return WxMsgUtil.buildTextMsg(toUserName, fromUserName, "验证码错误或已过期，请重新获取验证码。");
                    }
                }
            }
            // 处理事件消息（如扫码事件、关注事件等）
            else if ("event".equals(msgType)) {
                String event = msgMap.get("Event");
                String eventKey = msgMap.get("EventKey");

                log.info("处理微信事件: event={}, eventKey={}", event, eventKey);
                
                // 处理关注事件
                if ("subscribe".equals(event)) {
                    log.info("用户关注公众号: fromUserName={}", fromUserName);
                    
                    // 生成欢迎消息
                    String welcomeMsg = "感谢您的关注！\n\n回复【login】获取验证码，完成账号绑定。";
                    return WxMsgUtil.buildTextMsg(toUserName, fromUserName, welcomeMsg);
                }
                // 处理取消关注事件
                else if ("unsubscribe".equals(event)) {
                    log.info("用户取消关注公众号: fromUserName={}", fromUserName);
                    
                    // 可以在这里处理取消关注后的逻辑，比如清除用户数据等
                    return "";
                }
                // 处理扫码事件
                else if ("SCAN".equals(event)) {
                    log.info("用户扫描二维码: fromUserName={}, eventKey={}", fromUserName, eventKey);
                    
                    // 更新二维码扫描状态
                    boolean updateResult = updateQrScanStatus(eventKey, fromUserName);
                    if (updateResult) {
                        log.info("二维码扫码状态已更新: eventKey={}, fromUserName={}", eventKey, fromUserName);
                        return WxMsgUtil.buildTextMsg(toUserName, fromUserName, "扫码成功！请输入验证码完成登录。");
                    } else {
                        log.error("更新二维码扫码状态失败: eventKey={}, fromUserName={}", eventKey, fromUserName);
                        return WxMsgUtil.buildTextMsg(toUserName, fromUserName, "扫码状态更新失败，请重试。");
                    }
                }
            }

            // 其他情况返回默认消息
            return WxMsgUtil.buildTextMsg(toUserName, fromUserName, "收到消息，请回复login获取验证码。");
        } catch (Exception e) {
            log.error("处理微信消息时发生异常", e);
            // 返回错误消息
            String fromUserName = msgMap.get("FromUserName");
            String toUserName = msgMap.get("ToUserName");
            return WxMsgUtil.buildTextMsg(toUserName, fromUserName, "处理消息时发生错误，请重试。");
        }
    }
}