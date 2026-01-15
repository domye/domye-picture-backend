package com.domye.picture.controller.picture;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domye.picture.auth.annotation.AuthCheck;
import com.domye.picture.common.BaseResponse;
import com.domye.picture.common.DeleteRequest;
import com.domye.picture.common.Result;
import com.domye.picture.constant.UserConstant;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.helper.impl.RedisCache;
import com.domye.picture.manager.auth.SpaceUserAuthManager;
import com.domye.picture.manager.auth.StpKit;
import com.domye.picture.manager.auth.annotation.SaSpaceCheckPermission;
import com.domye.picture.manager.auth.model.SpaceUserPermissionConstant;
import com.domye.picture.manager.mdc.MdcDot;
import com.domye.picture.service.picture.PictureService;
import com.domye.picture.service.picture.model.dto.*;
import com.domye.picture.service.picture.model.entity.Picture;
import com.domye.picture.service.picture.model.enums.PictureReviewStatusEnum;
import com.domye.picture.service.picture.model.vo.PictureTagCategory;
import com.domye.picture.service.picture.model.vo.PictureVO;
import com.domye.picture.service.space.SpaceService;
import com.domye.picture.service.space.model.entity.Space;
import com.domye.picture.service.user.UserService;
import com.domye.picture.service.user.model.entity.User;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.domye.picture.constant.UserConstant.USER_LOGIN_STATE;

@RestController
@RequestMapping("/picture")
@MdcDot(bizCode = "#picture")
public class PictureController {
    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();

    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    @Resource
    private RedisCache redisCache;

    /**
     * 上传图片
     * @param multipartFile        文件
     * @param pictureUploadRequest 上传请求
     * @param request              http请求
     * @return 图片信息
     */
    @PostMapping("/upload")
    @ApiOperation(value = "上传图片")
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
     * @param deleteRequest 删除请求
     * @param request       http请求
     * @return 删除是否成功
     */
    @PostMapping("/delete")
    @ApiOperation(value = "删除图片")
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
     * @param pictureEditRequest 更新请求
     * @param request            http请求
     * @return 更新是否成功
     */
    @PostMapping("/edit")
    @ApiOperation(value = "编辑图片")
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
     * @param pictureUpdateRequest 更新请求
     * @param request              请求
     * @return 更新是否成功
     */
    @PostMapping("/update")
    @ApiOperation(value = "管理员更新图片")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        Throw.throwIf(pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);

        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
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
     * @param id 图片id
     * @return 图片信息
     */
    @GetMapping("/get")
    @ApiOperation(value = "根据id获取图片")
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
     * @param id      图片id
     * @param request http请求
     * @return 脱敏后的图片信息
     */
    @GetMapping("/get/vo")
    @ApiOperation(value = "根据id获取脱敏后的图片信息")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        Throw.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        String remoteAddr = request.getRemoteAddr();
        Entry entry = null;
        try {
            entry = SphU.entry("getPictureVOById", EntryType.IN, 1, remoteAddr);
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
            // 获取封装类
            return Result.success(pictureVO);
        } catch (Throwable t) {
            if (!BlockException.isBlockException(t)) {
                Tracer.trace(t);
                return Result.error(ErrorCode.SYSTEM_ERROR, "系统错误");
            }
            if (t instanceof DegradeException) {
                return Result.success(null);
            }
            return Result.error(ErrorCode.SYSTEM_ERROR, "访问过于频繁，请稍后再试");
        } finally {
            if (entry != null)
                entry.exit(1, remoteAddr);
        }

    }


    /**
     * 分页获取图片列表（仅管理员可用）
     * @param pictureQueryRequest 查询请求
     * @return 图片列表
     */
    @PostMapping("/list/page")
    @ApiOperation(value = "分页获取图片列表（仅管理员可用）")
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
     * @param pictureQueryRequest 查询请求
     * @param request             http请求
     * @return 脱敏后的图片列表
     */
    @PostMapping("/list/page/vo")
    @ApiOperation(value = "分页获取脱敏后的图片列表")
    @SentinelResource(value = "listPictureVOByPage", blockHandler = "handleBlockException", fallback = "handleFallback")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        Throw.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        // 公开图库
        if (spaceId == null) {
            // 普通用户默认只能查看已过审的公开数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        } else {
            // 私有空间
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            Throw.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        }
        // 构建缓存 key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = "DomyePicture:listPictureVOByPage:" + hashKey;

        // 查询缓存
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            // 如果缓存命中，返回结果
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return Result.success(cachedPage);
        }

        cachedValue = (String) redisCache.get(cacheKey);
        if (cachedValue != null) {
            // 如果缓存命中，返回结果
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            LOCAL_CACHE.put(cacheKey, cachedValue);
            return Result.success(cachedPage);
        }

        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);

        // 存入 Redis 缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        // 5 - 10 分钟随机过期，防止雪崩
        Long cacheExpireTime = 300L + RandomUtil.randomLong(0, 300);
        redisCache.put(cacheKey, cacheValue, cacheExpireTime);
        LOCAL_CACHE.put(cacheKey, cacheValue);
        // 返回结果
        return Result.success(pictureVOPage);
    }

    /**
     * 限流
     * @param pictureQueryRequest
     * @param request
     * @param ex
     * @return
     */
    public BaseResponse<Page<PictureVO>> handleBlockException(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                              HttpServletRequest request, BlockException ex) {
        if (ex instanceof DegradeException) {
            return handleFallback(pictureQueryRequest, request, ex);
        }
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后再试");
    }

    //降级操作
    public BaseResponse<Page<PictureVO>> handleFallback(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request, Throwable ex) {
        return Result.success(null);
    }

    /**
     * 获取图片标签分类
     * @return
     */
    @GetMapping("/tag_category")
    @ApiOperation(value = "获取图片标签分类")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "生活", "扫街", "艺术", "旅游", "创意");
        List<String> categoryList = Arrays.asList("人像", "风光", "扫街");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return Result.success(pictureTagCategory);
    }

    /**
     * 图片审核
     * @param pictureReviewRequest 审核请求
     * @param request              http请求
     * @return 审核结果
     */
    @PostMapping("/review")
    @ApiOperation(value = "图片审核")
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
     * @param searchPictureByColorRequest 搜索请求
     * @param request                     http请求
     * @return 搜索结果
     */
    @PostMapping("/search/color")
    @ApiOperation(value = "根据颜色搜索图片")
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
