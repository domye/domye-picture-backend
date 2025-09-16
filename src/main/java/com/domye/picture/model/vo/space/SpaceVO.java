package com.domye.picture.model.vo.space;

import cn.hutool.core.bean.BeanUtil;
import com.domye.picture.model.entity.Space;
import com.domye.picture.model.vo.UserVO;
import lombok.Data;

import java.util.Date;


@Data
public class SpaceVO {
    /**
     * id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    private Long totalCount;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 创建用户信息
     */
    private UserVO user;
    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;


    public static Space voToObject(SpaceVO spaceVO) {
        if (spaceVO == null)
            return null;
        Space space = new Space();
        BeanUtil.copyProperties(spaceVO, space);
        return space;
    }

    public static SpaceVO objectToVo(Space space) {
        if (space == null)
            return null;
        SpaceVO spaceVO = new SpaceVO();
        BeanUtil.copyProperties(space, spaceVO);
        return spaceVO;
    }
}