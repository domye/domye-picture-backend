package com.domye.picture.model.dto.space;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserEditRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 用户 ID
     */
    private Long id;
    /**
     * 空间角色：viewer/editor/admin
     * viewer: 0
     * editor: 1
     * admin: 2
     */
    private String spaceRole;
}
