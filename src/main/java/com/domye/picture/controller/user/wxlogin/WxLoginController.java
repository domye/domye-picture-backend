package com.domye.picture.controller.user.wxlogin;

import com.domye.picture.common.BaseResponse;
import com.domye.picture.common.Result;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.service.user.WxPublicService;
import com.domye.picture.service.user.model.dto.WxReceiveMessageRequest;
import com.domye.picture.service.user.model.dto.WxVerifyRequest;
import com.domye.picture.utils.WxMsgUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/wx/message")
public class WxLoginController {

    @Resource
    private WxPublicService wxPublicService;


    /**
     * 微信服务器验证
     */
    @GetMapping
    public String verify(WxVerifyRequest request) {

        String signature = request.getSignature();
        String timestamp = request.getTimestamp();
        String nonce = request.getNonce();
        String echostr = request.getEchostr();
        log.info("微信验证请求: signature={}, timestamp={}, nonce={}, echostr={}",
                signature, timestamp, nonce, echostr);

        if (wxPublicService.checkSignature(signature, timestamp, nonce)) {
            log.info("微信验证成功");
            return echostr; // 返回 echostr 表示验证成功
        } else {
            log.error("微信验证失败");
            return "error";
        }
    }

    /**
     * 检查服务器配置
     */
    @GetMapping("/config")
    public String checkConfig() {
        log.info("微信服务器配置检查: token={}", wxPublicService.getToken());
        return "配置检查完成，token=" + wxPublicService.getToken();
    }

    /**
     * 接收微信消息
     */
    @PostMapping(produces = "application/xml; charset=UTF-8")
    public String receiveMessage(HttpServletRequest request, WxReceiveMessageRequest wxReceiveMessageRequest) {
        String signature = wxReceiveMessageRequest.getSignature();
        String timestamp = wxReceiveMessageRequest.getTimestamp();
        String nonce = wxReceiveMessageRequest.getNonce();

        log.info("接收到微信消息请求: signature={}, timestamp={}, nonce={}", signature, timestamp, nonce);

        // 1. 验证签名
        if (!wxPublicService.checkSignature(signature, timestamp, nonce)) {
            log.error("微信消息签名验证失败");
            return "error";
        }

        try {
            // 2. 读取 XML 消息
            String xmlData = wxPublicService.getRequestBody(request);
            log.info("接收到微信消息: {}", xmlData);

            // 3. 解析 XML 消息
            Map<String, String> msgMap = WxMsgUtil.parseXml(xmlData);

            // 检查必要字段是否存在
            if (msgMap.isEmpty()) {
                log.error("解析微信消息失败，无法获取消息内容");
                return "error";
            }

            // 4. 处理消息
            return wxPublicService.handleMessage(msgMap);

        } catch (Exception e) {
            log.error("处理微信消息异常", e);
            return "error";
        }
    }

    /**
     * 处理关注/取消关注事件
     * @param request 微信消息请求
     * @return 响应消息
     */
    @PostMapping("/event")
    public BaseResponse<String> handleEvent(HttpServletRequest request) {
        try {
            // 读取 XML 消息
            String xmlData = wxPublicService.getRequestBody(request);
            log.info("接收到微信事件消息: {}", xmlData);

            // 解析 XML 消息
            Map<String, String> msgMap = WxMsgUtil.parseXml(xmlData);

            // 检查必要字段是否存在
            if (msgMap.isEmpty()) {
                log.error("解析微信事件消息失败，无法获取消息内容");
                return Result.error(ErrorCode.OPERATION_ERROR);
            }

            String fromUserName = msgMap.get("FromUserName");
            String toUserName = msgMap.get("ToUserName");
            String event = msgMap.get("Event");

            log.info("处理微信事件: fromUserName={}, toUserName={}, event={}", fromUserName, toUserName, event);

            // 处理关注事件
            if ("subscribe".equals(event)) {
                log.info("用户关注公众号: {}", fromUserName);

                // 生成欢迎消息
                String welcomeMsg = "感谢您的关注！\n\n回复【login】获取验证码，完成账号绑定。";
                return Result.success(WxMsgUtil.buildTextMsg(toUserName, fromUserName, welcomeMsg));
            }
            // 处理取消关注事件
            else if ("unsubscribe".equals(event)) {
                log.info("用户取消关注公众号: {}", fromUserName);

                // 可以在这里处理取消关注后的逻辑，比如清除用户数据等
                return Result.success("");
            }

            return Result.success("");

        } catch (Exception e) {
            log.error("处理微信事件异常", e);
            return Result.error(ErrorCode.OPERATION_ERROR);
        }
    }


}