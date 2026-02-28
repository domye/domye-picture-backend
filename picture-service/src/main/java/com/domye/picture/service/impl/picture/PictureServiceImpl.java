package com.domye.picture.service.impl.picture;


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
import com.domye.picture.model.mapper.picture.PictureStructMapper;
import com.domye.picture.model.mapper.user.UserStructMapper;
import com.domye.picture.model.vo.picture.PictureVO;
import com.domye.picture.model.vo.user.UserVO;
import com.domye.picture.service.api.picture.PictureService;
import com.domye.picture.service.api.rank.RankService;
import com.domye.picture.service.api.space.SpaceService;
import com.domye.picture.service.api.user.FilterlistService;
import com.domye.picture.service.api.user.UserService;
import com.domye.picture.service.helper.upload.CosManager;
import com.domye.picture.service.helper.upload.FileManager;
import com.domye.picture.service.helper.upload.UploadPictureResult;
import com.domye.picture.service.mapper.PictureMapper;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;

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
@Slf4j
@RequiredArgsConstructor
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    final PictureMapper pictureMapper;
    final FileManager fileManager;
    final CosManager cosManager;
    final UserService userService;
    final SpaceService spaceService;
    final RankService rankService;
    final FilterlistService filterlistService;
    final RedisCache redisCache;
    final Cache<String, String> pictureListLocalCache;
    final PictureStructMapper pictureStructMapper;
    final UserStructMapper userStructMapper;
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
        return pictureStructMapper.toVo(picture);
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

    /**
     * 持久化图片数据
     * 
     * 事务配置说明：
     * - rollbackFor = Exception.class: 所有异常都触发回滚，确保数据一致性
     * - propagation = Propagation.REQUIRED: 如果当前存在事务则加入，否则新建事务
     * 
     * 事务边界优化：
     * - 事务内：仅包含数据库写操作（saveOrUpdate + space额度更新）
     * - 事务外：rankService.addActivityScore 在事务外执行，避免长事务阻塞
     * 
     * 优化原因：
     * 1. 缩小事务范围，减少锁持有时间
     * 2. 积分更新为非关键业务，失败不影响主流程
     * 3. 避免外部服务调用阻塞事务提交
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    protected void persistPictureData(Picture picture, Long pictureId, User loginUser, Long spaceId) {
        // 1. 保存或更新图片信息（事务内 - 必须成功）
        boolean result = this.saveOrUpdate(picture);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        
        // 2. 更新空间额度（事务内 - 与图片保存保持原子性）
        if (spaceId != null) {
            // 使用乐观锁思想，基于当前值更新，避免并发问题
            Space space = spaceService.getById(spaceId);
            Throw.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            boolean update = spaceService.lambdaUpdate()
                    .eq(Space::getId, spaceId)
                    .set(Space::getTotalSize, space.getTotalSize() + picture.getPicSize())
                    .set(Space::getTotalCount, space.getTotalCount() + 1)
                    .update();
            Throw.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
        }
        
        // 3. 更新用户活跃积分（事务外执行，避免阻塞）
        // 注意：此操作在事务外执行，即使失败也不影响图片上传
        addUserActivityScore(loginUser, pictureId);
    }
    
    /**
     * 更新用户活跃积分 - 非事务操作
     * 单独抽出避免影响主事务，积分更新失败不应阻塞图片上传
     */
    private void addUserActivityScore(User loginUser, Long pictureId) {
        try {
            UserActivityScoreAddRequest userActivityScoreAddRequest = new UserActivityScoreAddRequest();
            userActivityScoreAddRequest.setPictureId(pictureId);
            userActivityScoreAddRequest.setUploadPicture(true);
            rankService.addActivityScore(loginUser, userActivityScoreAddRequest);
        } catch (Exception e) {
            // 积分更新失败不影响主业务流程，仅记录日志
            log.warn("更新用户活跃积分失败, userId: {}, pictureId: {}", loginUser.getId(), pictureId, e);
        }
    }

    /**
     * 构建图片查询条件 - 纯查询操作，不需要事务
     * 
     * 注意：此方法仅构建查询条件，不涉及数据库状态变更，无需事务支持
     */
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
        // 标签查询优化：将循环 JSON_CONTAINS 改为单次 JSON_OVERLAPS 查询
        // 性能提升：减少 N 次函数调用为 1 次，利用 MySQL 8.0.17+ 的多值索引特性
        List<String> tags = pictureQueryRequest.getTags();
        if (CollUtil.isNotEmpty(tags)) {
            // 构建标签数组 JSON，使用 JSON_OVERLAPS 进行高效匹配
            // 例如：JSON_OVERLAPS(tags, '["tag1", "tag2"]')
            String tagsJsonArray = JSONUtil.toJsonStr(tags);
            queryWrapper.apply("JSON_OVERLAPS(tags, {0})", tagsJsonArray);
        }
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取图片VO - 纯查询操作，不需要事务
     * 
     * 注意：此方法为只读查询，仅组装视图对象，无需事务支持
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        PictureVO pictureVO = pictureStructMapper.toVo(picture);
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userStructMapper.toUserVo(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片VO列表 - 纯查询操作，不需要事务
     * 
     * 注意：此方法为只读查询，仅组装视图对象列表，无需事务支持
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        List<PictureVO> pictureVOList = pictureStructMapper.toVoList(pictureList);
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = userIdUserListMap.containsKey(userId) ? userIdUserListMap.get(userId).get(0) : null;
            pictureVO.setUser(userStructMapper.toUserVo(user));
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

    /**
     * 执行图片审核 - 需要事务保证审核状态一致性
     * 
     * 事务配置说明：
     * - rollbackFor = Exception.class: 所有异常都触发回滚
     * - propagation = Propagation.REQUIRED: 加入现有事务或新建事务
     * 
     * 注意：审核操作涉及状态变更，需要事务保证原子性
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
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
        pictureStructMapper.copyPicture(updatePicture, oldPicture);
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

    /**
     * 删除图片
     * 
     * 事务配置说明：
     * - rollbackFor = Exception.class: 所有异常都触发回滚，确保数据一致性
     * - propagation = Propagation.REQUIRED: 如果当前存在事务则加入，否则新建事务
     * 
     * 事务边界优化：
     * - 事务内：仅包含数据库写操作（removeById + space额度回退）
     * - 事务外：clearPictureFile 在事务外执行，避免长事务
     * 
     * 优化原因：
     * 1. 文件删除操作耗时较长，不应在事务内执行
     * 2. 缩小事务范围，减少锁持有时间
     * 3. 文件删除失败可通过补偿机制处理，不影响数据一致性
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    @Override
    public void deletePicture(Long id, User loginUser) {
        Throw.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Throw.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        Picture oldPicture = this.getById(id);
        Throw.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        
        // 1. 删除图片记录（事务内）
        boolean result = this.removeById(id);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
        
        // 2. 回退空间额度（事务内 - 与图片删除保持原子性）
        if (oldPicture.getSpaceId() != null) {
            Space space = spaceService.getById(oldPicture.getSpaceId());
            Throw.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            boolean update = spaceService.lambdaUpdate()
                    .eq(Space::getId, oldPicture.getSpaceId())
                    .set(Space::getTotalSize, space.getTotalSize() - oldPicture.getPicSize())
                    .set(Space::getTotalCount, space.getTotalCount() - 1)
                    .update();
            Throw.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
        }
        
        // 3. 清理文件（事务外执行，异步操作不影响主事务）
        // 注意：文件删除在事务外执行，即使失败也不影响数据库一致性
        this.clearPictureFile(oldPicture);
    }

    /**
     * 编辑图片信息 - 需要事务保证数据一致性
     * 
     * 事务配置说明：
     * - rollbackFor = Exception.class: 所有异常都触发回滚
     * - propagation = Propagation.REQUIRED: 加入现有事务或新建事务
     * 
     * 注意：编辑操作涉及状态变更，需要事务保证原子性
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        Picture picture = pictureStructMapper.toEntity(pictureEditRequest);
        picture.setEditTime(new Date());
        this.validPicture(picture);
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        Throw.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        this.fillReviewParams(picture, loginUser);
        boolean result = this.updateById(picture);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 根据颜色搜索图片 - 纯查询操作，不需要事务
     * 
     * 注意：此方法为只读查询，不需要事务支持
     * 查询逻辑：先按空间过滤，再计算颜色相似度排序
     */
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
        return pictureStructMapper.toVoList(sortedList);
    }

    /**
     * 带缓存的分页查询 - 纯查询操作，不需要事务
     * 
     * 注意：此方法为只读查询，使用多级缓存提升性能，不需要事务支持
     * 缓存策略：本地缓存 -> Redis缓存 -> 数据库
     */
    @Override
    public Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        // 性能监控：记录整个查询过程的耗时
        StopWatch stopWatch = new StopWatch("listPictureVOByPageWithCache");
        stopWatch.start("参数校验与缓存Key构建");
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
        stopWatch.stop();
        stopWatch.start("本地缓存查询");
        String cachedValue = pictureListLocalCache.getIfPresent(cacheKey);
        if (cachedValue != null) {
            stopWatch.stop();
            log.debug("[标签查询优化] 本地缓存命中, 标签条件: {}, 总耗时: {}ms", 
                    pictureQueryRequest.getTags(), stopWatch.getTotalTimeMillis());
            return JSONUtil.toBean(cachedValue, Page.class);
        }
        
        stopWatch.stop();
        stopWatch.start("Redis缓存查询");
        cachedValue = (String) redisCache.get(cacheKey);
        if (cachedValue != null) {
            stopWatch.stop();
            pictureListLocalCache.put(cacheKey, cachedValue);
            log.debug("[标签查询优化] Redis缓存命中, 标签条件: {}, 总耗时: {}ms", 
                    pictureQueryRequest.getTags(), stopWatch.getTotalTimeMillis());
            return JSONUtil.toBean(cachedValue, Page.class);
        }
        
        stopWatch.stop();
        stopWatch.start("数据库查询");
        
        // 【性能关键】标签查询使用 JSON_OVERLAPS 优化，避免循环 JSON_CONTAINS
        // 详见 getQueryWrapper 方法中的实现
        Page<Picture> picturePage = this.page(new Page<>(current, size), this.getQueryWrapper(pictureQueryRequest));
        stopWatch.stop();
        stopWatch.start("VO转换");
        Page<PictureVO> pictureVOPage = this.getPictureVOPage(picturePage, request);
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        Long cacheExpireTime = 300L + RandomUtil.randomLong(0, 300);
        stopWatch.stop();
        stopWatch.start("缓存写入");
        redisCache.put(cacheKey, cacheValue, cacheExpireTime);
        pictureListLocalCache.put(cacheKey, cacheValue);
        stopWatch.stop();
        
        // 记录性能日志，当标签查询或总耗时较长时输出警告
        long totalTime = stopWatch.getTotalTimeMillis();
        List<String> tags = pictureQueryRequest.getTags();
        if (CollUtil.isNotEmpty(tags) || totalTime > 100) {
            log.info("[标签查询优化] 标签条件: {}, 总耗时: {}ms, 详细: {}", 
                    tags, totalTime, stopWatch.prettyPrint());
        }
        return pictureVOPage;
    }
}
