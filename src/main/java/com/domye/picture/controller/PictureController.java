package com.domye.picture.controller;

import com.domye.picture.annotation.AuthCheck;
import com.domye.picture.common.BaseResponse;
import com.domye.picture.common.ResultUtils;
import com.domye.picture.constant.UserConstant;
import com.domye.picture.model.dto.picture.PictureUploadRequest;
import com.domye.picture.model.entity.User;
import com.domye.picture.model.vo.PictureVO;
import com.domye.picture.service.PictureService;
import com.domye.picture.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;

    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }
}
