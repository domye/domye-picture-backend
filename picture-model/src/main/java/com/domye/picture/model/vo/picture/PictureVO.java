package com.domye.picture.model.vo.picture;

import cn.hutool.json.JSONUtil;
import com.domye.picture.model.entity.picture.Picture;
import com.domye.picture.model.vo.user.UserVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class PictureVO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
    /**
     * 图片 url
     */
    private String url;
    /**
     * 缩略图 url
     */
    private String thumbnailUrl;
    /**
     * 图片名称
     */
    private String name;
    /**
     * 简介
     */
    private String introduction;
    /**
     * 标签
     */
    private List<String> tags;
    /**
     * 分类
     */
    private String category;
    /**
     * 文件体积
     */
    private Long picSize;
    /**
     * 图片宽度
     */
    private Integer picWidth;
    /**
     * 图片高度
     */
    private Integer picHeight;
    /**
     * 图片比例
     */
    private Double picScale;
    /**
     * 图片格式
     */
    private String picFormat;
    /**
     * 图片主色调
     */
    private String picColor;
    /**
     * 用户 id
     */
    private Long userId;
    /**
     * 空间 id
     */
    private Long spaceId;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 编辑时间
     */
    private Date editTime;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 创建用户信息
     */
    private UserVO user;
    /**
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();

    /**
     * 封装类转对象
     */
    public static Picture voToObj(PictureVO pictureVO) {
        if (pictureVO == null) {
            return null;
        }
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureVO, picture);
        // 类型不同，需要转换
        picture.setTags(JSONUtil.toJsonStr(pictureVO.getTags()));
        return picture;
    }

    /**
     * 对象转封装类
     */
    public static PictureVO objToVo(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVO pictureVO = new PictureVO();
        BeanUtils.copyProperties(picture, pictureVO);
        // 类型不同，需要转换
        pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
        return pictureVO;
    }
}