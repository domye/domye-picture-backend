package com.domye.picture.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domye.picture.auth.SpaceUserAuthManager;
import com.domye.picture.auth.annotation.AuthCheck;
import com.domye.picture.auth.annotation.SaSpaceCheckPermission;
import com.domye.picture.auth.model.SpaceUserPermissionConstant;
import com.domye.picture.common.auth.StpKit;
import com.domye.picture.common.constant.PictureConstant;
import com.domye.picture.common.constant.UserConstant;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.mdc.MdcDot;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.DeleteRequest;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.picture.*;
import com.domye.picture.model.entity.picture.Picture;
import com.domye.picture.model.entity.space.Space;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.mapper.PictureStructMapper;
import com.domye.picture.model.vo.picture.PictureTagCategory;
import com.domye.picture.model.vo.picture.PictureVO;
import com.domye.picture.service.api.picture.PictureService;
import com.domye.picture.service.api.space.SpaceService;
import com.domye.picture.service.api.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.domye.picture.common.constant.UserConstant.USER_LOGIN_STATE;

@RestController
@RequestMapping("/picture")
@MdcDot(bizCode = "#picture")
@RequiredArgsConstructor
public class PictureController {

    final PictureService pictureService;
    final UserService userService;
    final SpaceService spaceService;
    final SpaceUserAuthManager spaceUserAuthManager;
    final PictureStructMapper pictureStructMapper;


    /**
     * 上传图片
     *
     * @param multipartFile        文件
     * @param pictureUploadRequest 上传请求
     * @param request              http请求
     * @return 图片信息
     */
    @PostMapping("/upload")
    @Operation(summary = "上传图片")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return Result.success(pictureVO);
    }


    /**
     * 删除图片
     *
     * @param deleteRequest 删除请求
     * @param request       http请求
     * @return 删除是否成功
     */
    @PostMapping("/delete")
    @Operation(summary = "删除图片")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {

        Throw.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        pictureService.deletePicture(id, loginUser);
        return Result.success(true);
    }

    /**
     * 编辑图片
     *
     * @param pictureEditRequest 更新请求
     * @param request            http请求
     * @return 更新是否成功
     */
    @PostMapping("/edit")
    @Operation(summary = "编辑图片")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {

        Throw.throwIf(pictureEditRequest == null || pictureEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);
        // 在此处将实体类和 DTO 进行转换
        pictureService.editPicture(pictureEditRequest, loginUser);
        return Result.success(true);
    }

    /**
     * 更新图片
     *
     * @param pictureUpdateRequest 更新请求
     * @param request              请求
     * @return 更新是否成功
     */
    @PostMapping("/update")
    @Operation(summary = "管理员更新图片")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        Throw.throwIf(pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);

        // 将实体类和 DTO 进行转换
        Picture picture = pictureStructMapper.toEntity(pictureUpdateRequest);
        // 数据校验
        pictureService.validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        Throw.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return Result.success(true);
    }


    /**
     * 根据id获取图片
     *
     * @param id 图片id
     * @return 图片信息
     */
    @GetMapping("/get")
    @Operation(summary = "根据id获取图片")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id) {
        Throw.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        Throw.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return Result.success(picture);
    }

    /**
     * 根据id获取图片（封装类）
     *
     * @param id      图片id
     * @param request http请求
     * @return 脱敏后的图片信息
     */
    @GetMapping("/get/vo")
    @Operation(summary = "根据id获取脱敏后的图片信息")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        Throw.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        Throw.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间的图片，需要校验权限
        Space space = null;
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            Throw.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            space = spaceService.getById(spaceId);
            Throw.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 获取权限列表
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        if (userObj != null) {
            User loginUser = (User) userObj;
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            pictureVO.setPermissionList(permissionList);
        }
        return Result.success(pictureVO);
    }


    /**
     * 分页获取图片列表（仅管理员可用）
     *
     * @param pictureQueryRequest 查询请求
     * @return 图片列表
     */
    @PostMapping("/list/page")
    @Operation(summary = "分页获取图片列表（仅管理员可用）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return Result.success(picturePage);
    }

    /**
     * 分页获取脱敏后的图片列表
     *
     * @param pictureQueryRequest 查询请求
     * @param request             http请求
     * @return 脱敏后的图片列表
     */
    @PostMapping("/list/page/vo")
    @Operation(summary = "分页获取脱敏后的图片列表")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        // 空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            Throw.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        }
        // 调用 Service 层缓存方法
        Page<PictureVO> pictureVOPage = pictureService.listPictureVOByPageWithCache(pictureQueryRequest, request);
        return Result.success(pictureVOPage);
    }


    /**
     * 获取图片标签分类
     *
     * @return
     */
    @GetMapping("/tag_category")
    @Operation(summary = "获取图片标签分类")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        pictureTagCategory.setTagList(PictureConstant.DEFAULT_TAG_LIST);
        pictureTagCategory.setCategoryList(PictureConstant.DEFAULT_CATEGORY_LIST);
        return Result.success(pictureTagCategory);
    }

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 审核请求
     * @param request              http请求
     * @return 审核结果
     */
    @PostMapping("/review")
    @Operation(summary = "图片审核")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        Throw.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return Result.success(true);
    }

    /**
     * 根据颜色搜索图片
     *
     * @param searchPictureByColorRequest 搜索请求
     * @param request                     http请求
     * @return 搜索结果
     */
    @PostMapping("/search/color")
    @Operation(summary = "根据颜色搜索图片")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        Throw.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> result = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return Result.success(result);
    }
}
