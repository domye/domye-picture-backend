package com.domye.picture.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

/**
 * 微信消息加解密工具类
 */
@Slf4j
@Component
public class WxMessageCrypt {

    @Value("${wx.app-id}")
    private String appId;

    @Value("${wx.token}")
    private String token;

    @Value("${wx.encoding-aes-key}")
    private String encodingAESKey;

    private byte[] aesKey;

    @PostConstruct
    public void init() {
        // 将EncodingAESKey转换为AES密钥
        aesKey = Base64.decodeBase64(encodingAESKey + "=");
    }

    /**
     * 解密微信消息
     * @param encryptMsg 加密的消息
     * @return 解密后的消息
     */
    public String decryptWeChatMessage(String encryptMsg) {
        try {
            if (StringUtils.isEmpty(encryptMsg)) {
                log.error("加密消息为空");
                throw new RuntimeException("加密消息为空");
            }
            
            log.info("开始解密微信消息，加密消息长度: {}", encryptMsg.length());
            
            // Base64解码
            byte[] encrypted = Base64.decodeBase64(encryptMsg);
            log.info("Base64解码后数据长度: {}", encrypted.length);

            // 初始化AES解密器
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            AlgorithmParameters params = AlgorithmParameters.getInstance("AES");
            params.init(new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16)));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, params);

            // 执行解密
            byte[] decrypted = cipher.doFinal(encrypted);
            log.info("解密后数据长度: {}", decrypted.length);

            // 解析解密后的数据
            return extractDecryptedMessage(decrypted);
        } catch (Exception e) {
            log.error("解密微信消息失败", e);
            throw new RuntimeException("解密微信消息失败", e);
        }
    }

    /**
     * 从解密后的数据中提取消息
     * @param decrypted 解密后的字节数组
     * @return 原始消息内容
     */
    private String extractDecryptedMessage(byte[] decrypted) {
        try {
            // 前16位是网络字节序
            int networkOrder = bytesToInt(decrypted, 16);
            // 获得AppId + +16位随机字符串 + 消息内容 + +4位消息尾
            int msgLength = bytesToInt(decrypted, 20);
            
            // 检查数据长度是否足够
            if (decrypted.length < 20 + msgLength + appId.length()) {
                log.error("解密后的数据长度不足: {}, 需要至少: {}", decrypted.length, 20 + msgLength + appId.length());
                throw new RuntimeException("解密后的数据长度不足");
            }
            
            // 提取AppId
            String fromAppId = new String(decrypted, decrypted.length - appId.length(), appId.length(), StandardCharsets.UTF_8);
            
            // 如果AppId不匹配，返回错误
            if (!appId.equals(fromAppId)) {
                log.error("AppId不匹配，期望: {}, 实际: {}", appId, fromAppId);
                throw new RuntimeException("AppId不匹配");
            }
            
            // 提取消息内容
            return new String(decrypted, 20, msgLength, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("提取解密消息失败，解密数据长度: {}", decrypted.length, e);
            throw new RuntimeException("提取解密消息失败", e);
        }
    }

    /**
     * 将字节数组转换为整数
     * @param b 字节数组
     * @param start 起始位置
     * @return 整数
     */
    private int bytesToInt(byte[] b, int start) {
        return (b[start] & 0xff) << 24 | ((b[start + 1] & 0xff) << 16) | ((b[start + 2] & 0xff) << 8) | b[start + 3] & 0xff;
    }

    /**
     * 加密微信消息
     * @param message 原始消息
     * @param nonce 随机字符串
     * @param timestamp 时间戳
     * @return 加密后的消息
     */
    public String encryptWeChatMessage(String message, String nonce, String timestamp) {
        try {
            // 生成16位随机字符串
            String randomStr = generateRandomString(16);

            // 构造需要加密的数据:网络字节序 + 随机字符串 + 消息内容 + AppId
            byte[] data = new byte[4 + randomStr.length() + message.length() + appId.length()];

            // 网络字节序 (4字节)
            int networkOrder = (int) (System.currentTimeMillis() / 1000);
            data[0] = (byte) (networkOrder & 0xff);
            data[1] = (byte) ((networkOrder >> 8) & 0xff);
            data[2] = (byte) ((networkOrder >> 16) & 0xff);
            data[3] = (byte) ((networkOrder >> 24) & 0xff);

            // 随机字符串
            System.arraycopy(randomStr.getBytes(StandardCharsets.UTF_8), 0, data, 4, randomStr.length());

            // 消息内容长度
            int msgLength = message.length();
            data[4 + randomStr.length()] = (byte) (msgLength & 0xff);
            data[5 + randomStr.length()] = (byte) ((msgLength >> 8) & 0xff);
            data[6 + randomStr.length()] = (byte) ((msgLength >> 16) & 0xff);
            data[7 + randomStr.length()] = (byte) ((msgLength >> 24) & 0xff);

            // 消息内容
            System.arraycopy(message.getBytes(StandardCharsets.UTF_8), 0, data, 8 + randomStr.length(), message.length());

            // AppId
            System.arraycopy(appId.getBytes(StandardCharsets.UTF_8), 0, data, 8 + randomStr.length() + message.length(), appId.length());

            // 加密
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            AlgorithmParameters params = AlgorithmParameters.getInstance("AES");
            params.init(new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16)));
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, params);

            byte[] encrypted = cipher.doFinal(data);

            // 生成签名
            String signature = generateSignature(token, timestamp, nonce, encrypted);

            // 构造返回数据
            return String.format("<xml><Encrypt><![CDATA[%s]]></Encrypt><MsgSignature><![CDATA[%s]]></MsgSignature><TimeStamp>%s</TimeStamp><Nonce><![CDATA[%s]]></Nonce></xml>",
                    Base64.encodeBase64String(encrypted),
                    signature,
                    timestamp,
                    nonce);
        } catch (Exception e) {
            log.error("加密微信消息失败", e);
            throw new RuntimeException("加密微信消息失败", e);
        }
    }

    /**
     * 生成随机字符串
     * @param length 字符串长度
     * @return 随机字符串
     */
    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    /**
     * 生成签名
     * @param token Token
     * @param timestamp 时间戳
     * @param nonce 随机字符串
     * @param encrypted 加密后的消息
     * @return 签名
     */
    private String generateSignature(String token, String timestamp, String nonce, byte[] encrypted) {
        try {
            // 将token、timestamp、nonce和加密后的消息进行字典序排序
            String[] arr = new String[]{token, timestamp, nonce, Base64.encodeBase64String(encrypted)};
            Arrays.sort(arr);

            // 拼接字符串
            StringBuilder sb = new StringBuilder();
            for (String s : arr) {
                sb.append(s);
            }

            // SHA1加密
            return WxMsgUtil.sha1(sb.toString());
        } catch (Exception e) {
            log.error("生成签名失败", e);
            throw new RuntimeException("生成签名失败", e);
        }
    }
}
