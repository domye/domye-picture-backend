package com.domye.picture.service.user;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WxPublicServiceImpl implements WxPublicService {

    // 缓存 Key 常量
    private final static String WX_LOGIN_CODE_KEY = "login:code:";
    private final static String WX_LOGIN_TOKEN_KEY = "login:token:";
    private final static String WX_OPENID_TO_CODE_KEY = "openid_to_code:";
    private final static int CODE_EXPIRE_TIME = 5; // 5 分钟
    private final static int TOKEN_EXPIRE_TIME = 24 * 60; // 24 小时

    @Resource
    private StringRedisTemplate stringRedisTemplate;

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
     * 生成唯一验证码
     * @return 验证码
     */
    private String generateUniqueCode() {
        // 生成6位数字验证码
        String code = String.format("%06d", (int) ((Math.random() * 9 + 1) * 100000));
        log.debug("生成验证码: {}", code);
        return code;
    }
}