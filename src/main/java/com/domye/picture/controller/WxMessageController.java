package com.domye.picture.controller;

import com.domye.picture.service.user.WxPublicService;
import com.domye.picture.utils.WxMessageCrypt;
import com.domye.picture.utils.WxMsgUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/wx/message")
public class WxMessageController {

    private static final String WX_TOKEN = "domye";

    @Resource
    private WxPublicService wxPublicService;

    @Resource
    private WxMessageCrypt wxMessageCrypt;

    /**
     * 微信服务器验证
     */
    @GetMapping
    public String verify(@RequestParam("signature") String signature,
                         @RequestParam("timestamp") String timestamp,
                         @RequestParam("nonce") String nonce,
                         @RequestParam("echostr") String echostr) {

        log.info("微信验证请求: signature={}, timestamp={}, nonce={}, echostr={}",
                signature, timestamp, nonce, echostr);

        if (checkSignature(signature, timestamp, nonce)) {
            log.info("微信验证成功");
            return echostr; // 返回 echostr 表示验证成功
        } else {
            log.error("微信验证失败");
            return "error";
        }
    }

    /**
     * 接收微信消息
     */
    @PostMapping(produces = "application/xml; charset=UTF-8")
    public String receiveMessage(HttpServletRequest request,
                                 @RequestParam("signature") String signature,
                                 @RequestParam("timestamp") String timestamp,
                                 @RequestParam("nonce") String nonce) {

        log.info("接收到微信消息请求: signature={}, timestamp={}, nonce={}", signature, timestamp, nonce);

        // 1. 验证签名
        if (!checkSignature(signature, timestamp, nonce)) {
            log.error("微信消息签名验证失败");
            return "error";
        }

        try {
            // 2. 读取 XML 消息
            String xmlData = getRequestBody(request);
            log.info("接收到微信消息: {}", xmlData);

            // 3. 解析 XML 消息
            Map<String, String> msgMap = WxMsgUtil.parseXml(xmlData);

            // 检查必要字段是否存在
            if (msgMap == null || msgMap.isEmpty()) {
                log.error("解析微信消息失败，无法获取消息内容");
                return "error";
            }

            // 5. 处理消息
            return handleMessage(msgMap);

        } catch (Exception e) {
            log.error("处理微信消息异常", e);
            return "error";
        }
    }

    /**
     * 验证微信签名
     */
    private boolean checkSignature(String signature, String timestamp, String nonce) {
        // 1. 将token、timestamp、nonce三个参数进行字典序排序
        String[] arr = new String[]{WX_TOKEN, timestamp, nonce};
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
    private String getRequestBody(HttpServletRequest request) throws IOException {
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
    private String decryptWeChatMessage(String encryptMsg) {
        return wxMessageCrypt.decryptWeChatMessage(encryptMsg);
    }

    /**
     * 处理微信消息
     */
    private String handleMessage(Map<String, String> msgMap) {
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
                    String code = wxPublicService.handleLoginRequest(fromUserName, null, null);
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
                    // 这里可以添加验证码验证逻辑
                    return WxMsgUtil.buildTextMsg(toUserName, fromUserName, "验证成功！您的账号已绑定微信。");
                }
            }
            // 处理事件消息（如扫码事件）
            else if ("event".equals(msgType)) {
                String event = msgMap.get("Event");
                String eventKey = msgMap.get("EventKey");

                log.info("处理微信事件: event={}, eventKey={}", event, eventKey);

                // 处理扫码事件
                if ("SCAN".equals(event)) {
                    // 这里可以根据eventKey获取二维码关联的openId，并更新扫码状态
                    return WxMsgUtil.buildTextMsg(toUserName, fromUserName, "扫码成功！请输入验证码完成登录。");
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