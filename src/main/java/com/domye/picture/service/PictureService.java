package com.domye.picture.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.dto.picture.PictureUploadRequest;
import com.domye.picture.model.entity.Picture;
import com.domye.picture.model.entity.User;
import com.domye.picture.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Domye
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-08-29 17:03:47
 */
public interface PictureService extends IService<Picture> {
    PictureVO uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);
}
