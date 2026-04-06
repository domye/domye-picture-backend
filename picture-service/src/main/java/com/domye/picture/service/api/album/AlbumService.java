package com.domye.picture.service.api.album;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.dto.album.AlbumAddRequest;
import com.domye.picture.model.dto.album.AlbumEditRequest;
import com.domye.picture.model.dto.album.AlbumPictureAddRequest;
import com.domye.picture.model.entity.album.Album;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.vo.album.AlbumVO;

public interface AlbumService extends IService<Album> {

    Long addAlbum(AlbumAddRequest albumAddRequest, User user) ;

    void editAlbum(AlbumEditRequest albumEditRequest, User loginUser);

    void deleteAlbum(Long id, User loginUser);

    AlbumVO getAlbumVO(Album album);

   void addPicturesToAlbum(AlbumPictureAddRequest request, User loginUser);
}
