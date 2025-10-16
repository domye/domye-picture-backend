package com.domye.picture.controller.user.wxlogin;

import com.domye.picture.manager.wxlogin.BaseWxMsgResVo;
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
import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/wx/message")
public class WxMessageController {

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
     * 接收微信消息
     */
    @PostMapping(consumes = {"application/xml", "text/xml"},
            produces = "application/xml;charset=utf-8")
    public BaseWxMsgResVo receiveMessage(HttpServletRequest request, WxReceiveMessageRequest wxReceiveMessageRequest) throws IOException {
        String signature = wxReceiveMessageRequest.getSignature();
        String timestamp = wxReceiveMessageRequest.getTimestamp();
        String nonce = wxReceiveMessageRequest.getNonce();

        log.info("接收到微信消息请求: signature={}, timestamp={}, nonce={}", signature, timestamp, nonce);

        // 1. 验证签名
        if (!wxPublicService.checkSignature(signature, timestamp, nonce)) {
            log.error("微信消息签名验证失败");
            // 签名验证失败，返回错误响应
            BaseWxMsgResVo errorResponse = new BaseWxMsgResVo();
            errorResponse.setToUserName("");
            errorResponse.setFromUserName("");
            errorResponse.setCreateTime(System.currentTimeMillis() / 1000);
            errorResponse.setMsgType("text");
            errorResponse.setContent("签名验证失败");
            return errorResponse;
        }


        // 2. 读取 XML 消息
        String xmlData = wxPublicService.getRequestBody(request);

        // 3. 解析 XML 消息
        Map<String, String> msgMap = WxMsgUtil.parseXml(xmlData);

        // 检查必要字段是否存在
        if (msgMap.isEmpty()) {
            log.error("解析微信消息失败，无法获取消息内容");
            // 返回错误消息，而不是null，以确保微信服务器收到响应
            BaseWxMsgResVo errorResponse = new BaseWxMsgResVo();
            errorResponse.setToUserName("");
            errorResponse.setFromUserName("");
            errorResponse.setCreateTime(System.currentTimeMillis() / 1000);
            errorResponse.setMsgType("text");
            errorResponse.setContent("消息格式错误，请重试。");
            return errorResponse;
        }

        // 4. 处理消息
        BaseWxMsgResVo response = wxPublicService.handleMessage(msgMap, request);

        // 确保响应不为null，如果为null则返回空响应
        if (response == null) {
            log.warn("处理消息返回null，将返回默认响应");
            BaseWxMsgResVo defaultResponse = new BaseWxMsgResVo();
            defaultResponse.setToUserName("");
            defaultResponse.setFromUserName("");
            defaultResponse.setCreateTime(System.currentTimeMillis() / 1000);
            defaultResponse.setMsgType("text");
            defaultResponse.setContent("系统处理异常，请稍后重试。");
            return defaultResponse;
        }

        log.info("准备返回微信消息响应: {}", response);
        return response;


    }


}