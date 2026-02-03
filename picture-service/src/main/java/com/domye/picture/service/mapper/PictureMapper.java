package com.domye.picture.service.mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Date;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domye.picture.model.entity.picture.Picture;

/**
 * @author Domye
 * @description 针对表【picture(图片)】的数据库操作Mapper
 * @createDate 2025-09-09 22:50:19
 * @Entity com.domye.picture.model.entity.picture.Picture
 */
public interface PictureMapper extends BaseMapper<Picture> {

    List<Picture> findAllByEditTimeAfter(@Param("minEditTime")Date minEditTime);


}




