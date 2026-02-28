package com.domye.picture.model.mapper.picture;

import cn.hutool.json.JSONUtil;
import com.domye.picture.model.dto.picture.PictureEditRequest;
import com.domye.picture.model.dto.picture.PictureUpdateRequest;
import com.domye.picture.model.entity.picture.Picture;
import com.domye.picture.model.vo.picture.PictureVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PictureStructMapper {

    Picture toEntity(PictureEditRequest request);

    @Mapping(target = "tags", expression = "java(mapJsonToTags(picture.getTags()))")
    PictureVO toVo(Picture picture);

    List<PictureVO> toVoList(List<Picture> pictures);

    default List<String> mapJsonToTags(String tagsJson) {
        if (tagsJson == null) {
            return null;
        }
        return JSONUtil.toList(tagsJson, String.class);
    }

    default String mapTagsToJson(List<String> tags) {
        if (tags == null) {
            return null;
        }
        return JSONUtil.toJsonStr(tags);
    }

    default Picture toEntity(PictureUpdateRequest request) {
        if (request == null) {
            return null;
        }
        Picture picture = new Picture();
        picture.setId(request.getId());
        picture.setName(request.getName());
        picture.setIntroduction(request.getIntroduction());
        picture.setCategory(request.getCategory());
        picture.setTags(mapTagsToJson(request.getTags()));
        picture.setSpaceId(request.getSpaceId());
        return picture;
    }

    void copyPicture(@MappingTarget Picture target, Picture source);
}
