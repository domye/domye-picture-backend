package com.domye.picture.service.impl.picture;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.common.constant.PictureConstant;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.helper.ColorSimilarUtils;
import com.domye.picture.common.helper.impl.RedisCache;
import com.domye.picture.model.dto.picture.PictureEditRequest;
import com.domye.picture.model.dto.picture.PictureQueryRequest;
import com.domye.picture.model.dto.picture.PictureReviewRequest;
import com.domye.picture.model.dto.picture.PictureUploadRequest;
import com.domye.picture.model.dto.rank.UserActivityScoreAddRequest;
import com.domye.picture.model.entity.picture.Picture;
import com.domye.picture.model.entity.space.Space;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.enums.PictureReviewStatusEnum;
import com.domye.picture.model.vo.picture.PictureVO;
import com.domye.picture.model.vo.user.UserVO;
import com.domye.picture.service.helper.upload.CosManager;
import com.domye.picture.service.helper.upload.FileManager;
import com.domye.picture.service.helper.upload.UploadPictureResult;
import com.domye.picture.service.mapper.PictureMapper;
import com.domye.picture.service.api.picture.PictureService;
import com.domye.picture.service.api.rank.RankService;
import com.domye.picture.service.api.space.SpaceService;
import com.domye.picture.service.api.user.FilterlistService;
import com.domye.picture.service.api.user.UserService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Domye
 * @description 针对表【picture(图片)】的数据库操作 Service 实现
 * @createDate 2025-08-29 17:03:47
 */
@Service
@RequiredArgsConstructor
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    final PictureMapper pictureMapper;
    final FileManager fileManager;
    final CosManager cosManager;
    final UserService userService;
    final SpaceService spaceService;
    final TransactionTemplate transactionTemplate;
    final RankService rankService;
    final FilterlistService filterlistService;
    final RedisCache redisCache;
    final Cache<String, String> pictureListLocalCache;

    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        Throw.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        Throw.throwIf(filterlistService.isInFilterList(loginUser.getId(), 0L, 0L), ErrorCode.NO_AUTH_ERROR, "用户已被禁止该操作");
        Long pictureId = pictureUploadRequest.getId();
        Picture oldPicture = null;
        if (pictureId != null) {
            oldPicture = pictureMapper.selectById(pictureId);
            Throw.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        Long spaceId = resolveAndValidateSpaceId(pictureUploadRequest, oldPicture);
        String uploadPathPrefix = buildUploadPathPrefix(spaceId, loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        Picture picture = buildPictureEntity(uploadPictureResult, loginUser, pictureId, spaceId);
        persistPictureData(picture, pictureId, loginUser, spaceId);
        return PictureVO.objToVo(picture);
    }

    private Long resolveAndValidateSpaceId(PictureUploadRequest pictureUploadRequest, Picture oldPicture) {
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (oldPicture != null) {
            if (spaceId == null) {
                spaceId = oldPicture.getSpaceId();
            } else {
                Throw.throwIf(ObjUtil.notEqual(spaceId, oldPicture.getSpaceId()), ErrorCode.PARAMS_ERROR, "空间 id 不一致");
            }
        }
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            Throw.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            Throw.throwIf(space.getTotalCount() >= space.getMaxCount(), ErrorCode.OPERATION_ERROR, "空间条数不足");
            Throw.throwIf(space.getTotalSize() >= space.getMaxSize(), ErrorCode.OPERATION_ERROR, "空间大小不足");
        }
        return spaceId;
    }

    private String buildUploadPathPrefix(Long spaceId, Long userId) {
        if (spaceId == null) {
            return String.format(PictureConstant.PUBLIC_PATH_PREFIX, userId);
        }
        return String.format(PictureConstant.SPACE_PATH_PREFIX, spaceId);
    }

    private Picture buildPictureEntity(UploadPictureResult uploadPictureResult, User loginUser, Long pictureId, Long spaceId) {
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setSpaceId(spaceId);
        picture.setUserId(loginUser.getId());
        fillReviewParams(picture, loginUser);
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        return picture;
    }

    private void persistPictureData(Picture picture, Long pictureId, User loginUser, Long spaceId) {
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            Throw.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            if (spaceId != null) {
                // 使用 set() 方法安全更新，避免 SQL 注入风险
                // 注意：set() 会覆盖值，需要先查询当前值或使用原生 SQL
                Space space = spaceService.getById(spaceId);
                Throw.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .set(Space::getTotalSize, space.getTotalSize() + picture.getPicSize())
                        .set(Space::getTotalCount, space.getTotalCount() + 1)
                        .update();
                Throw.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            UserActivityScoreAddRequest userActivityScoreAddRequest = new UserActivityScoreAddRequest();
            userActivityScoreAddRequest.setPictureId(pictureId);
            userActivityScoreAddRequest.setUploadPicture(true);
            rankService.addActivityScore(loginUser, userActivityScoreAddRequest);
            return true;
        });
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        String searchText = pictureQueryRequest.getSearchText();
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like(PictureConstant.FIELD_NAME, searchText).or().like(PictureConstant.FIELD_INTRODUCTION, searchText));
        }
        Long id = pictureQueryRequest.getId();
        Long userId = pictureQueryRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id)
                .eq(ObjUtil.isNotEmpty(userId), "userId", userId)
                .like(StrUtil.isNotBlank(pictureQueryRequest.getName()), PictureConstant.FIELD_NAME, pictureQueryRequest.getName())
                .like(StrUtil.isNotBlank(pictureQueryRequest.getIntroduction()), PictureConstant.FIELD_INTRODUCTION, pictureQueryRequest.getIntroduction())
                .like(StrUtil.isNotBlank(pictureQueryRequest.getPicFormat()), PictureConstant.FIELD_PIC_FORMAT, pictureQueryRequest.getPicFormat())
                .like(StrUtil.isNotBlank(pictureQueryRequest.getReviewMessage()), PictureConstant.FIELD_REVIEW_MESSAGE, pictureQueryRequest.getReviewMessage())
                .eq(StrUtil.isNotBlank(pictureQueryRequest.getCategory()), PictureConstant.FIELD_CATEGORY, pictureQueryRequest.getCategory())
                .eq(ObjUtil.isNotEmpty(pictureQueryRequest.getPicWidth()), PictureConstant.FIELD_PIC_WIDTH, pictureQueryRequest.getPicWidth())
                .eq(ObjUtil.isNotEmpty(pictureQueryRequest.getPicHeight()), PictureConstant.FIELD_PIC_HEIGHT, pictureQueryRequest.getPicHeight())
                .eq(ObjUtil.isNotEmpty(pictureQueryRequest.getPicSize()), PictureConstant.FIELD_PIC_SIZE, pictureQueryRequest.getPicSize())
                .eq(ObjUtil.isNotEmpty(pictureQueryRequest.getPicScale()), PictureConstant.FIELD_PIC_SCALE, pictureQueryRequest.getPicScale())
                .eq(ObjUtil.isNotEmpty(pictureQueryRequest.getReviewStatus()), PictureConstant.FIELD_REVIEW_STATUS, pictureQueryRequest.getReviewStatus())
                .eq(ObjUtil.isNotEmpty(pictureQueryRequest.getReviewerId()), PictureConstant.FIELD_REVIEWER_ID, pictureQueryRequest.getReviewerId())
                .ge(ObjUtil.isNotEmpty(pictureQueryRequest.getStartEditTime()), PictureConstant.FIELD_EDIT_TIME, pictureQueryRequest.getStartEditTime())
                .lt(ObjUtil.isNotEmpty(pictureQueryRequest.getEndEditTime()), PictureConstant.FIELD_EDIT_TIME, pictureQueryRequest.getEndEditTime());
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq(PictureConstant.FIELD_SPACE_ID, spaceId);
        } else {
            queryWrapper.isNull(PictureConstant.FIELD_SPACE_ID);
        }
        List<String> tags = pictureQueryRequest.getTags();
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.apply("JSON_CONTAINS(tags, JSON_QUOTE({0}))", tag);
            }
        }
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        PictureVO pictureVO = PictureVO.objToVo(picture);
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = userIdUserListMap.containsKey(userId) ? userIdUserListMap.get(userId).get(0) : null;
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        Throw.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        Throw.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            Throw.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            Throw.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long pictureId = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        Throw.throwIf(pictureId == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum), ErrorCode.NO_AUTH_ERROR);
        Picture oldPicture = this.getById(pictureId);
        Throw.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        Throw.throwIf(oldPicture.getReviewStatus().equals(reviewStatus), ErrorCode.PARAMS_ERROR, "请勿重复审核");
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(oldPicture, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        updatePicture.setReviewStatus(reviewStatus);
        boolean result = this.updateById(updatePicture);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery().eq(Picture::getUrl, pictureUrl).count();
        if (count > 1) {
            return;
        }
        cosManager.deleteObject(oldPicture.getUrl());
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    @Override
    public void deletePicture(Long id, User loginUser) {
        Throw.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Throw.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        Picture oldPicture = this.getById(id);
        Throw.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        transactionTemplate.execute(status -> {
            boolean result = this.removeById(id);
            Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
            if (oldPicture.getSpaceId() != null) {
                // 使用 set() 方法安全更新，避免 SQL 注入风险
                Space space = spaceService.getById(oldPicture.getSpaceId());
                Throw.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, oldPicture.getSpaceId())
                        .set(Space::getTotalSize, space.getTotalSize() - oldPicture.getPicSize())
                        .set(Space::getTotalCount, space.getTotalCount() - 1)
                        .update();
                Throw.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
        this.clearPictureFile(oldPicture);
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        picture.setEditTime(new Date());
        this.validPicture(picture);
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        Throw.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        this.fillReviewParams(picture, loginUser);
        boolean result = this.updateById(picture);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        Throw.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        Throw.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        Space space = spaceService.getById(spaceId);
        Throw.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        Throw.throwIf(!space.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        Color oldColor = Color.decode(picColor);
        List<Picture> sortedList = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    String picColorStr = picture.getPicColor();
                    if (StrUtil.isBlank(picColorStr)) {
                        return Double.MAX_VALUE;
                    }
                    Color newColor = Color.decode(picColorStr);
                    return -ColorSimilarUtils.calculateSimilarity(oldColor, newColor);
                }))
                .limit(PictureConstant.MAX_COLOR_SEARCH_RESULTS)
                .collect(Collectors.toList());
        return sortedList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
    }

    @Override
    public Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        Throw.throwIf(size > PictureConstant.MAX_PAGE_SIZE, ErrorCode.PARAMS_ERROR);
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        }
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = "DomyePicture:listPictureVOByPage:" + hashKey;
        String cachedValue = pictureListLocalCache.getIfPresent(cacheKey);
        if (cachedValue != null) {
            return JSONUtil.toBean(cachedValue, Page.class);
        }
        cachedValue = (String) redisCache.get(cacheKey);
        if (cachedValue != null) {
            pictureListLocalCache.put(cacheKey, cachedValue);
            return JSONUtil.toBean(cachedValue, Page.class);
        }
        Page<Picture> picturePage = this.page(new Page<>(current, size), this.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = this.getPictureVOPage(picturePage, request);
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        Long cacheExpireTime = 300L + RandomUtil.randomLong(0, 300);
        redisCache.put(cacheKey, cacheValue, cacheExpireTime);
        pictureListLocalCache.put(cacheKey, cacheValue);
        return pictureVOPage;
    }
}
