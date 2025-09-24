package com.domye.picture.model.vo.space;

import com.domye.picture.model.entity.SpaceUser;
import com.domye.picture.model.vo.UserVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

@Data
public class SpaceUserVO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
    /**
     * 空间 id
     */
    private Long spaceId;
    /**
     * 用户 id
     */
    private Long userId;
    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 用户信息
     */
    private UserVO user;
    /**
     * 空间信息
     */
    private SpaceVO space;

    /**
     * 封装类转对象
     * @param spaceUserVO
     * @return
     */
    public static SpaceUser voToObj(SpaceUserVO spaceUserVO) {
        if (spaceUserVO == null) {
            return null;
        }
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserVO, spaceUser);
        return spaceUser;
    }

    /**
     * 对象转封装类
     * @param spaceUser
     * @return
     */
    public static SpaceUserVO objToVo(SpaceUser spaceUser) {
        if (spaceUser == null) {
            return null;
        }
        SpaceUserVO spaceUserVO = new SpaceUserVO();
        BeanUtils.copyProperties(spaceUser, spaceUserVO);
        return spaceUserVO;
    }
}
