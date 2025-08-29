package com.domye.picture.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.ThrowUtils;
import com.domye.picture.manager.FileManager;
import com.domye.picture.mapper.PictureMapper;
import com.domye.picture.model.dto.file.UploadPictureResult;
import com.domye.picture.model.dto.picture.PictureUploadRequest;
import com.domye.picture.model.entity.Picture;
import com.domye.picture.model.entity.User;
import com.domye.picture.model.vo.PictureVO;
import com.domye.picture.service.PictureService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author Domye
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-08-29 17:03:47
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    @Resource
    private PictureMapper pictureMapper;
    @Resource
    private FileManager fileManager;

    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        Long pictureId = pictureUploadRequest.getId();
        if (pictureId != null) {
            Picture picture = pictureMapper.selectById(pictureId);
            ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }

        String uploadPathPrefix = String.format("/upload/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);
    }
}




