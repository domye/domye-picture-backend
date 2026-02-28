package com.domye.picture.model.mapper.space;

import com.domye.picture.model.dto.space.SpaceAddRequest;
import com.domye.picture.model.dto.space.SpaceEditRequest;
import com.domye.picture.model.dto.space.SpaceUpdateRequest;
import com.domye.picture.model.entity.space.Space;
import com.domye.picture.model.vo.space.SpaceVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * Space 实体转换 Mapper
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SpaceStructMapper {

    SpaceVO toVo(Space space);

    List<SpaceVO> toVoList(List<Space> spaces);

    Space toEntity(SpaceAddRequest request);

    Space toEntity(SpaceUpdateRequest request);

    Space toEntity(SpaceEditRequest request);
}
