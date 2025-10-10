package com.domye.picture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.picture.dto.PictureEditRequest;
import com.domye.picture.model.picture.dto.PictureQueryRequest;
import com.domye.picture.model.picture.dto.PictureReviewRequest;
import com.domye.picture.model.picture.dto.PictureUploadRequest;
import com.domye.picture.model.picture.entity.Picture;
import com.domye.picture.model.picture.vo.PictureVO;
import com.domye.picture.model.user.entity.User;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
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

    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    public PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    public void validPicture(Picture picture);

    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(Picture picture, User loginUser);

    public void clearPictureFile(Picture oldPicture);


    void deletePicture(Long id, User loginUser);

    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);
}
