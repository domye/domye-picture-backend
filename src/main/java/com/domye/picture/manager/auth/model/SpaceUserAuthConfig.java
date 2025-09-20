package com.domye.picture.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SpaceUserAuthConfig implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 权限列表
     */
    private List<SpaceUserPermission> permissions;
    /**
     * 角色列表
     */
    private List<SpaceUserRole> roles;
}
