package com.domye.picture.service.helper.websocket.model;

import com.domye.picture.model.vo.user.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditResponseMessage {

    /**
     * 消息类型，例如 "INFO", "ERROR", "ENTER_EDIT", "EXIT_EDIT", "EDIT_ACTION"
     */
    private String type;

    /**
     * 信息
     */
    private String message;

    /**
     * 执行的编辑动作
     */
    private String editAction;

    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * 当前旋转角度（用于状态同步）
     */
    private Integer rotateDegree;

    /**
     * 当前缩放比例（用于状态同步）
     */
    private Double scaleRatio;
}
