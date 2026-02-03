package com.domye.picture.model.dto.space;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * ID
     */
    private Long id;
    /**
     * 空间 ID
     */
    private Long spaceId;
    /**
     * 用户 ID
     */
    private Long userId;
    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;
}
