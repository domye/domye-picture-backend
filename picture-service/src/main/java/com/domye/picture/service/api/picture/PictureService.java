package com.domye.picture.service.api.picture;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.dto.picture.*;
import com.domye.picture.model.entity.picture.Picture;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.vo.picture.PictureVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author Domye
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-08-29 17:03:47
 */
public interface PictureService extends IService<Picture> {
    PictureVO uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    void validPicture(Picture picture);

    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(Picture picture, User loginUser);

    void clearPictureFile(Picture oldPicture);


    void deletePicture(Long id, User loginUser);

    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    void updatePicture(PictureUpdateRequest pictureUpdateRequest, User loginUser);

    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);
}
