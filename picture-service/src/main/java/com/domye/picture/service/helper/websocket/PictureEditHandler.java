package com.domye.picture.service.helper.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.service.helper.websocket.disruptor.PictureEditEventProducer;
import com.domye.picture.service.helper.websocket.model.PictureEditActionEnum;
import com.domye.picture.service.helper.websocket.model.PictureEditMessageTypeEnum;
import com.domye.picture.service.helper.websocket.model.PictureEditRequestMessage;
import com.domye.picture.service.helper.websocket.model.PictureEditResponseMessage;
import com.domye.picture.service.api.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 图片编辑WebSocket处理器
 * 处理图片编辑相关的WebSocket连接和消息交互
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PictureEditHandler extends TextWebSocketHandler {
    // 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户 ID
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();
    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();
    // 保存每张图片的编辑状态（旋转角度、缩放比例），用于新用户同步
    private final Map<Long, EditState> pictureEditStates = new ConcurrentHashMap<>();
    final PictureEditEventProducer pictureEditEventProducer;
    final UserService userService;

    /**
     * 编辑状态内部类
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class EditState {
        private Integer rotateDegree = 0;   // 旋转角度，默认0
        private Double scaleRatio = 1.0;    // 缩放比例，默认1.0
    }

    /**
     * 连接建立后的处理
     * @param session WebSocket会话
     * @throws Exception 可能抛出的异常
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 保存会话到集合中
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);

        // 构造响应：通知其他用户有人加入
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("%s加入编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        // 广播给同一张图片的用户
        broadcastToPicture(pictureId, pictureEditResponseMessage);

        // 同步当前编辑状态给新用户（单独发送，不广播）
        syncEditStateToNewUser(session, pictureId);
    }

    /**
     * 同步编辑状态给新加入的用户
     */
    private void syncEditStateToNewUser(WebSocketSession session, Long pictureId) throws Exception {
        EditState editState = pictureEditStates.get(pictureId);
        if (editState != null && session.isOpen()) {
            PictureEditResponseMessage syncMessage = new PictureEditResponseMessage();
            syncMessage.setType(PictureEditMessageTypeEnum.SYNC_STATE.getValue());
            syncMessage.setMessage("同步编辑状态");
            syncMessage.setRotateDegree(editState.getRotateDegree());
            syncMessage.setScaleRatio(editState.getScaleRatio());
            
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            objectMapper.registerModule(module);
            
            String json = objectMapper.writeValueAsString(syncMessage);
            session.sendMessage(new TextMessage(json));
        }
    }

    /**
     * 处理文本消息
     * @param session WebSocket会话
     * @param message 接收到的文本消息
     * @throws Exception 可能抛出的异常
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 将消息解析为 PictureEditMessage
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        // 从 Session 属性中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        // 生产消息
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
    }

    /**
     * 连接关闭后的处理
     * @param session WebSocket会话
     * @param status  关闭状态
     * @throws Exception 可能抛出的异常
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long pictureId = (Long) attributes.get("pictureId");
        User user = (User) attributes.get("user");
        // 移除当前用户的编辑状态
        handleExitEditMessage(null, session, user, pictureId);

        // 删除会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }

        // 响应
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("%s离开编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 处理进入编辑消息
     * @param pictureEditRequestMessage 请求消息
     * @param session                   WebSocket会话
     * @param user                      用户信息
     * @param pictureId                 图片ID
     * @throws Exception 可能抛出的异常
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        // 没有用户正在编辑该图片，才能进入编辑
        if (!pictureEditingUsers.containsKey(pictureId)) {
            // 设置当前用户为编辑用户
            pictureEditingUsers.put(pictureId, user.getId());
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("%s开始编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 处理编辑动作消息
     * @param pictureEditRequestMessage 请求消息
     * @param session                   WebSocket会话
     * @param user                      用户信息
     * @param pictureId                 图片ID
     * @throws Exception 可能抛出的异常
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            return;
        }
        // 确认是当前编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 更新编辑状态
            updateEditState(pictureId, actionEnum);
            
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("%s执行%s", user.getUserName(), actionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            // 广播给除了当前客户端之外的其他用户，否则会造成重复编辑
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }

    /**
     * 更新编辑状态
     */
    private void updateEditState(Long pictureId, PictureEditActionEnum actionEnum) {
        EditState editState = pictureEditStates.computeIfAbsent(pictureId, k -> new EditState());
        switch (actionEnum) {
            case ROTATE_LEFT:
                editState.setRotateDegree(editState.getRotateDegree() - 90);
                break;
            case ROTATE_RIGHT:
                editState.setRotateDegree(editState.getRotateDegree() + 90);
                break;
            case ZOOM_IN:
                editState.setScaleRatio(editState.getScaleRatio() + 0.1);
                break;
            case ZOOM_OUT:
                editState.setScaleRatio(Math.max(0.1, editState.getScaleRatio() - 0.1));
                break;
        }
    }

    /**
     * 处理退出编辑消息
     * @param pictureEditRequestMessage 请求消息
     * @param session                   WebSocket会话
     * @param user                      用户信息
     * @param pictureId                 图片ID
     * @throws Exception 可能抛出的异常
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 移除当前用户的编辑状态
            pictureEditingUsers.remove(pictureId);
            // 构造响应，发送退出编辑的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("%s退出编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 向指定图片的所有连接会话广播消息
     * @param pictureId                  图片ID
     * @param pictureEditResponseMessage 要广播的消息
     * @param excludeSession             排除的会话（不向该会话发送消息）
     * @throws Exception 可能抛出的异常
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludeSession) throws Exception {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            // 创建 ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            // 配置序列化：将 Long 类型转为 String，解决丢失精度问题
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance); // 支持 long 基本类型
            objectMapper.registerModule(module);
            // 序列化为 JSON 字符串
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessionSet) {
                // 排除掉的 session 不发送
                if (excludeSession != null && excludeSession.equals(session)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * 向指定图片的所有连接会话广播消息（不排除任何会话）
     * @param pictureId                  图片ID
     * @param pictureEditResponseMessage 要广播的消息
     * @throws Exception 可能抛出的异常
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws Exception {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }
}
