package com.domye.picture.service.WxMessage.impl;

import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.service.WxMessage.WxCodeService;
import com.domye.picture.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.domye.picture.constant.WxConstant.*;

@Service
@Slf4j
public class WxCodeServiceImpl implements WxCodeService {

    @Override
    public Boolean findCodeType(String code) {
        // 从Redis中查找验证码对应的openId
        String loginCode = WX_LOGIN_CODE_KEY + code;
        String bindCode = WX_BIND_CODE_KEY + code;
        Boolean loginEx = RedisUtil.hasKey(loginCode);
        if (loginEx) {
            return true;
        }
        Boolean bindEx = RedisUtil.hasKey(bindCode);
        if (bindEx) {
            return false;
        }
        return null;
    }


    /**
     * 处理请求，生成验证码
     */
    @Override
    public String handleVerifyRequest(String openId, Boolean type, String sceneId) {
        Throw.throwIf(StringUtils.isEmpty(openId), ErrorCode.PARAMS_ERROR, "openId不能为空");
        log.info("处理用户请求: openId={}, type={}", openId, type);

        // 检查是否已有未过期的验证码
        String existingCodeKey = WX_OPENID_TO_CODE_KEY + openId;
        String existingCode = RedisUtil.get(existingCodeKey);

        if (StringUtils.isNotEmpty(existingCode)) {
            log.info("用户已有未过期的验证码，返回现有验证码: openId={}, existingCode={}", openId, existingCode);
            return existingCode;
        }


        // 生成唯一验证码
        String code = RedisUtil.get("qr_scene_code:" + sceneId);
        if (StringUtils.isEmpty(code)) {
            code = generateUniqueCode();
            log.info("为用户生成新验证码: openId={}, code={}", openId, code);
        }
        //删除旧验证码
        RedisUtil.delete("qr_scene_code:" + sceneId);
        // 双向绑定存储
        String codeToOpenIdKey = type ? WX_LOGIN_CODE_KEY + code : WX_BIND_CODE_KEY + code;
        String openIdToCodeKey = WX_OPENID_TO_CODE_KEY + openId;

        RedisUtil.set(codeToOpenIdKey, openId, CODE_EXPIRE_TIME, TimeUnit.MINUTES);
        RedisUtil.set(openIdToCodeKey, code, CODE_EXPIRE_TIME, TimeUnit.MINUTES);


        String codeToSceneIdKey = "code_to_scene_id:" + code;
        RedisUtil.set(codeToSceneIdKey, sceneId, CODE_EXPIRE_TIME, TimeUnit.MINUTES);
        log.info("验证码已存储到Redis: codeToOpenIdKey={}, openIdToCodeKey={}", codeToOpenIdKey, openIdToCodeKey);


        return code;
    }

    /**
     * 验证验证码是否正确
     * @param openId 用户openId
     * @param code   用户输入的验证码
     * @return 验证结果，true表示验证成功
     */
    @Override
    public boolean verifyCode(String openId, String code, Boolean type) {
        Throw.throwIf(StringUtils.isEmpty(openId) || StringUtils.isEmpty(code), ErrorCode.PARAMS_ERROR, "openId不能为空");

        // 获取openId对应的验证码
        String existingCodeKey = WX_OPENID_TO_CODE_KEY + openId;
        String storedCode = RedisUtil.get(existingCodeKey);

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
            String codeToOpenIdKey = type ? WX_BIND_CODE_KEY + code : WX_LOGIN_CODE_KEY + code;
            RedisUtil.delete(existingCodeKey);
            RedisUtil.delete(codeToOpenIdKey);
            log.info("验证码验证成功，已删除验证码缓存: openId={}, code={}", openId, storedCode);
        }

        return isValid;
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
     * 存储验证码与sceneId的关联关系
     * @param code    验证码
     * @param sceneId 场景ID
     */
    @Override
    public void storeCodeSceneIdRelation(String code, String sceneId) {
        Throw.throwIf(StringUtils.isEmpty(code) || StringUtils.isEmpty(sceneId), ErrorCode.PARAMS_ERROR, "参数不能为空");

        String codeToSceneIdKey = "code_to_scene_id:" + code;
        RedisUtil.set(codeToSceneIdKey, sceneId, CODE_EXPIRE_TIME, TimeUnit.MINUTES);
        log.info("验证码与sceneId关联关系已存储: code={}, sceneId={}", code, sceneId);
    }
}
