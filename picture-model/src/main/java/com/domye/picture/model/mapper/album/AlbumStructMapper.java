package com.domye.picture.model.mapper.album;

import com.domye.picture.model.dto.album.AlbumAddRequest;
import com.domye.picture.model.dto.album.AlbumEditRequest;
import com.domye.picture.model.entity.album.Album;
import com.domye.picture.model.vo.album.AlbumVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AlbumStructMapper {
    AlbumVO toVo(Album album);

    Album toAdd(AlbumAddRequest albumAddRequest);

    Album toEntity(AlbumEditRequest albumEditRequest);

}
