package com.domye.picture.model.mapper;

import com.domye.picture.model.dto.picture.PictureEditRequest;
import com.domye.picture.model.dto.picture.PictureUpdateRequest;
import com.domye.picture.model.entity.picture.Picture;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import cn.hutool.json.JSONUtil;
import java.util.List;


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PictureStructMapper {
    PictureStructMapper INSTANCE = Mappers.getMapper(PictureStructMapper.class);
    
    Picture toEntity(PictureEditRequest request);
    
    void updateEntity(@MappingTarget Picture picture, PictureUpdateRequest request);
    
    default String mapTagsToJson(List<String> tags) {
        if (tags == null) {
            return null;
        }
        return JSONUtil.toJsonStr(tags);
    }
}
