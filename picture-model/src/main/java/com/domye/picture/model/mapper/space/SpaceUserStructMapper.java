package com.domye.picture.model.mapper.space;

import com.domye.picture.model.dto.space.SpaceUserEditRequest;
import com.domye.picture.model.entity.space.SpaceUser;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * SpaceUser 实体转换 Mapper
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SpaceUserStructMapper {

    SpaceUser toEntity(SpaceUserEditRequest request);

}
