package com.domye.picture.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.exception.BusinessException;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.manager.CosManager;
import com.domye.picture.manager.FileManager;
import com.domye.picture.mapper.PictureMapper;
import com.domye.picture.model.dto.file.UploadPictureResult;
import com.domye.picture.model.dto.picture.PictureEditRequest;
import com.domye.picture.model.dto.picture.PictureQueryRequest;
import com.domye.picture.model.dto.picture.PictureReviewRequest;
import com.domye.picture.model.dto.picture.PictureUploadRequest;
import com.domye.picture.model.entity.Picture;
import com.domye.picture.model.entity.Space;
import com.domye.picture.model.entity.User;
import com.domye.picture.model.enums.PictureReviewStatusEnum;
import com.domye.picture.model.vo.PictureVO;
import com.domye.picture.model.vo.UserVO;
import com.domye.picture.service.PictureService;
import com.domye.picture.service.UserService;
import com.domye.picture.utils.ColorSimilarUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

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
    @Resource
    private CosManager cosManager;
    @Resource
    private UserService userService;
    @Resource
    private SpaceServiceImpl spaceService;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        Throw.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        Long pictureId = pictureUploadRequest.getId();
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            Throw.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 必须空间创建人（管理员）才能上传
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }
        if (pictureId != null) {
            Picture oldPicture = pictureMapper.selectById(pictureId);
            Throw.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了 spaceId，必须和原有图片一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
                }
            }
        }

        String uploadPathPrefix;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
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
            Picture oldPicture = pictureMapper.selectById(pictureId);
            Throw.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            Throw.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            if (finalSpaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                Throw.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return PictureVO.objToVo(picture);
        });
        return PictureVO.objToVo(picture);
    }


    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
//        Boolean nullSpaceId = pictureQueryRequest.getNullSpaceId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            // and (name like "%xxx%" or introduction like "%xxx%")
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                            .or()
                            .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
        } else {
            queryWrapper.isNull("spaceId");
        }
//        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            /* and (tag like "%\"Java\"%" and like "%\"Python\"%") */
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 获取图片视图对象分页信息
     * @param picturePage 图片分页对象
     * @param request     HTTP请求对象
     * @return 返回封装后的图片视图对象分页信息
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        // 从分页对象中获取记录列表
        List<Picture> pictureList = picturePage.getRecords();
        // 创建新的视图对象分页，保持分页参数一致
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        // 如果记录列表为空，直接返回空分页对象
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        Throw.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
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
        if (pictureId == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Picture oldPicture = this.getById(pictureId);
        Throw.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 已是该状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }

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
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        cosManager.deleteObject(oldPicture.getUrl());
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }


    @Override
    public void deletePicture(Long id, User loginUser) {
        Throw.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Throw.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = this.getById(id);
        Throw.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(id);
            Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 更新空间的使用额度，释放额度
            if (oldPicture.getSpaceId() != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, oldPicture.getSpaceId())
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                Throw.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
        // 异步清理文件
        this.clearPictureFile(oldPicture);
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        Throw.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        Throw.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        Throw.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        Space space = spaceService.getById(spaceId);
        Throw.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!loginUser.getId().equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        Color oldColor = Color.decode(picColor);
        List<Picture> sortedList = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    //获取该photo的值
                    String picColorStr = picture.getPicColor();
                    //没有主色调放最后
                    if (StrUtil.isBlank(picColorStr)) {
                        return Double.MAX_VALUE;
                    }
                    Color newColor = Color.decode(picColorStr);
                    return -ColorSimilarUtils.calculateSimilarity(oldColor, newColor);
                    //计算相似值，越大越相似
                }))
                .limit(20)
                .collect(Collectors.toList());
        return sortedList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }
}




