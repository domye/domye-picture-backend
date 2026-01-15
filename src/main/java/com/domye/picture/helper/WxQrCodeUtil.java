package com.domye.picture.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信公众号二维码工具类
 */
@Slf4j
@Component
public class WxQrCodeUtil {

    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential";
    private static final String QRCODE_URL = "https://api.weixin.qq.com/cgi-bin/qrcode/create";
    @Value("${wx.app-id}")
    private String appId;
    @Value("${wx.app-secret}")
    private String appSecret;
    private ObjectMapper objectMapper = new ObjectMapper();
    private String accessToken;
    private long tokenExpireTime;

    /**
     * 获取访问令牌
     * @return 访问令牌
     */
    public String getAccessToken() {
        try {
            // 检查令牌是否过期
            if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
                return accessToken;
            }

            // 构建请求URL
            String url = ACCESS_TOKEN_URL + "&appid=" + appId + "&secret=" + appSecret;

            // 发送请求
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");

            // 读取响应
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // 解析响应
            Map<String, Object> result = objectMapper.readValue(response.toString(), Map.class);

            // 检查是否获取成功
            if (result.containsKey("access_token")) {
                accessToken = (String) result.get("access_token");
                // 设置过期时间（提前5分钟刷新）
                int expiresIn = result.containsKey("expires_in") ? (Integer) result.get("expires_in") : 7200;
                tokenExpireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L;

                log.info("获取微信访问令牌成功: {}", accessToken);
                return accessToken;
            } else {
                log.error("获取微信访问令牌失败: {}", response.toString());
                return null;
            }
        } catch (IOException e) {
            log.error("获取微信访问令牌异常", e);
            return null;
        }
    }

    /**
     * 创建临时二维码
     * @param sceneId 场景值ID，临时二维码时为32位非整型数字
     * @param seconds 二维码有效时间，单位为秒，最大为2592000（30天）
     * @return 二维码ticket
     */
    public String createTempQrCode(int sceneId, int seconds) {
        try {
            // 获取访问令牌
            String token = getAccessToken();
            if (token == null) {
                return null;
            }

            // 构建请求参数
            Map<String, Object> actionInfo = new HashMap<>();
            Map<String, Object> scene = new HashMap<>();
            scene.put("scene_id", sceneId);
            actionInfo.put("scene", scene);

            Map<String, Object> request = new HashMap<>();
            request.put("expire_seconds", seconds);
            request.put("action_name", "QR_SCENE");
            request.put("action_info", actionInfo);

            // 转换为JSON字符串
            String jsonRequest = objectMapper.writeValueAsString(request);

            // 发送请求
            URL url = new URL(QRCODE_URL + "?access_token=" + token);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // 发送请求体
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonRequest.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 读取响应
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // 解析响应
            Map<String, Object> result = objectMapper.readValue(response.toString(), Map.class);

            // 检查是否创建成功
            if (result.containsKey("ticket")) {
                String ticket = (String) result.get("ticket");
                log.info("创建临时二维码成功: sceneId={}, ticket={}", sceneId, ticket);
                return ticket;
            } else {
                log.error("创建临时二维码失败: sceneId={}, response={}", sceneId, response.toString());
                return null;
            }
        } catch (IOException e) {
            log.error("创建临时二维码异常: sceneId={}", sceneId, e);
            return null;
        }
    }

    /**
     * 获取二维码图片URL
     * @param ticket 二维码ticket
     * @return 二维码图片URL
     */
    public String getQrCodeImageUrl(String ticket) {
        return "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=" + ticket;
    }
}
