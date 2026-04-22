package com.domye.picture.api.controller;

import com.domye.picture.auth.annotation.AuthCheck;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.DeleteRequest;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.album.AlbumAddRequest;
import com.domye.picture.model.dto.album.AlbumEditRequest;
import com.domye.picture.model.dto.album.AlbumPictureAddRequest;
import com.domye.picture.model.entity.album.Album;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.vo.album.AlbumVO;
import com.domye.picture.service.api.album.AlbumService;
import com.domye.picture.service.api.picture.PictureService;
import com.domye.picture.service.api.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/album")
public class AlbumController {

    final AlbumService albumService;
    final UserService userService;
    final PictureService pictureService;


    /**
     * 创建相册
     *
     * @param albumAddRequest 创建请求
     * @param request         http请求
     * @return 相册id
     */
    @PostMapping("/add")
    @AuthCheck()
    @Operation(summary = "创建相册")
    public BaseResponse<Long> addAlbum(@RequestBody AlbumAddRequest albumAddRequest, HttpServletRequest request) {
        Throw.throwIf(albumAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Long albumId = albumService.addAlbum(albumAddRequest, loginUser);

        pictureService.clearPictureListCache();
        return Result.success(albumId);
    }


    /**
     * 编辑相册
     *
     * @param albumEditRequest 编辑请求
     * @param request          http请求
     * @return 是否编辑成功
     */
    @PostMapping("/edit")
    @Operation(summary = "编辑相册")
    public BaseResponse<Boolean> editAlbum(@RequestBody AlbumEditRequest albumEditRequest, HttpServletRequest request) {
        Throw.throwIf(albumEditRequest == null || albumEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        albumService.editAlbum(albumEditRequest, loginUser);

        pictureService.clearPictureListCache();
        return Result.success(true);
    }


    /**
     * 删除相册
     *
     * @param deleteRequest 删除请求
     * @param request       http请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @Operation(summary = "删除相册")
    public BaseResponse<Boolean> deleteAlbum(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        Throw.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        albumService.deleteAlbum(deleteRequest.getId(), loginUser);

        pictureService.clearPictureListCache();
        return Result.success(true);
    }

    /**
     * 根据id获取相册详情
     *
     * @param id 相册id
     * @return 相册信息
     */
    @GetMapping("/get/vo")
    @Operation(summary = "根据id获取相册详情")
    public BaseResponse<AlbumVO> getAlbumVOById(long id) {
        Throw.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Album album = albumService.getById(id);
        Throw.throwIf(album == null, ErrorCode.NOT_FOUND_ERROR, "相册不存在");
        // 获取封装类
        AlbumVO albumVO = albumService.getAlbumVO(album);
        return Result.success(albumVO);
    }

    /**
     * 添加图片到相册
     *
     * @param request 添加请求
     * @param httpRequest http 请求
     * @return 是否成功
     */
    @PostMapping("/picture/add")
    @AuthCheck
    @Operation(summary = "添加图片到相册")
    public BaseResponse<Boolean> addPicturesToAlbum(@RequestBody AlbumPictureAddRequest request,
                                                    HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        albumService.addPicturesToAlbum(request, loginUser);

        pictureService.clearPictureListCache();
        return Result.success(true);
    }
}
