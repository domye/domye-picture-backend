package com.domye.picture.service.impl.space;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.common.constant.SpaceConstant;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.helper.impl.LockService;
import com.domye.picture.model.dto.space.SpaceAddRequest;
import com.domye.picture.model.dto.space.SpaceQueryRequest;
import com.domye.picture.model.entity.space.Space;
import com.domye.picture.model.entity.space.SpaceUser;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.enums.SpaceLevelEnum;
import com.domye.picture.model.enums.SpaceRoleEnum;
import com.domye.picture.model.enums.SpaceTypeEnum;
import com.domye.picture.model.mapper.space.SpaceStructMapper;
import com.domye.picture.model.mapper.user.UserStructMapper;
import com.domye.picture.model.vo.space.SpaceVO;
import com.domye.picture.model.vo.user.UserVO;
import com.domye.picture.service.api.space.SpaceService;
import com.domye.picture.service.api.user.UserService;
import com.domye.picture.service.mapper.SpaceMapper;
import com.domye.picture.service.mapper.SpaceUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Domye
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-09-09 19:12:43
 */
@Service
@RequiredArgsConstructor
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {


    final UserService userService;
    final TransactionTemplate transactionTemplate;
    final LockService lockService;
    final SpaceUserMapper spaceUserMapper;
    final UserStructMapper userStructMapper;
    final SpaceStructMapper spaceStructMapper;

    /**
     * 新增空间
     * @param spaceAddRequest 新增空间请求
     * @param loginUser       登录用户
     * @return 空间id
     */
    @Override
    public Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        Space space = spaceStructMapper.toEntity(spaceAddRequest);
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            space.setSpaceName(SpaceConstant.DEFAULT_SPACE_NAME);
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (space.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 填充数据
        this.fillSpace(space);
        // 数据校验
        this.validSpace(space, true);

        // 权限校验
        Long userId = loginUser.getId();
        space.setUserId(userId);
        Throw.throwIf(!userService.isAdmin(loginUser) && space.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue(), ErrorCode.NO_AUTH_ERROR, SpaceConstant.NO_PERMISSION_CREATE_SPACE);
        // 使用分布式锁
        String lockKey = SpaceConstant.SPACE_CREATE_LOCK_PREFIX + userId;

        // 执行事务操作
        Long newSpaceId = lockService.executeWithLock(lockKey, (int) SpaceConstant.LOCK_WAIT_TIME, TimeUnit.SECONDS, () -> transactionTemplate.execute(status -> {
            boolean exists = this.lambdaQuery()
                    .eq(Space::getUserId, userId)
                    .eq(Space::getSpaceType, spaceAddRequest.getSpaceType())
                    .exists();
            Throw.throwIf(exists, ErrorCode.OPERATION_ERROR, SpaceConstant.SPACE_ALREADY_EXISTS);

            // 写入数据库
            boolean result = this.save(space);
            Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
            if (SpaceTypeEnum.TEAM.getValue() == spaceAddRequest.getSpaceType()) {
                SpaceUser spaceUser = new SpaceUser();
                spaceUser.setSpaceId(space.getId());
                spaceUser.setUserId(userId);
                spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                result = spaceUserMapper.insert(spaceUser) > 0;
                Throw.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
            }
            // 返回新写入的数据 id
            return space.getId();
        }));

        return Optional.ofNullable(newSpaceId).orElse(-1L);
    }

    /**
     * 填充空间容量
     * @param space 空间
     */
    @Override
    public void fillSpace(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 检验空间信息
     * @param space 空间
     * @param add   增加&修改
     */
    @Override
    public void validSpace(Space space, boolean add) {
        Throw.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 要创建
        if (add) {
            Throw.throwIf(spaceName == null, ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            Throw.throwIf(spaceLevel == null, ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            Throw.throwIf(spaceType == null, ErrorCode.PARAMS_ERROR, "空间类型不能为空");
        }
        // 修改数据时，如果要改空间级别
        Throw.throwIf(spaceLevel != null && spaceLevelEnum == null, ErrorCode.PARAMS_ERROR, "空间级别不存在");
        Throw.throwIf(spaceType != null && spaceTypeEnum == null, ErrorCode.PARAMS_ERROR, "空间类型不存在");
        Throw.throwIf(StrUtil.isNotBlank(spaceName) && spaceName.length() > 30, ErrorCode.PARAMS_ERROR, "空间名称过长");

    }

    /**
     * 用户空间鉴权
     * @param loginUser 登录用户
     * @param space     当前空间
     */
    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        Long oldUserId = space.getUserId();
        if (oldUserId == null) {
            // 公共图库，仅管理员可操作
            Throw.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);

        } else {
            // 私有空间，仅用户和管理员可操作
            Throw.throwIf(!oldUserId.equals(loginUser.getId()) && !userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        }
    }

    /**
     * 删除空间
     * @param id        空间id
     * @param loginUser 登录用户
     */
    @Override
    public void deleteSpace(long id, User loginUser) {
        Space oldSpace = this.getById(id);
        Throw.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或者管理员可删除
        this.checkSpaceAuth(loginUser, oldSpace);
        // 操作数据库
        boolean result = this.removeById(id);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 获取空间封装类
     * @param space   空间
     * @param request http请求
     * @return 脱敏后的spaceVO
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = spaceStructMapper.toVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userStructMapper.toUserVo(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    /**
     * 获取空间分页封装类
     * @param spacePage 空间分页
     * @param request   http请求
     * @return 封装后的spaceVO分页
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        // 从分页对象中获取记录列表
        List<Space> spaceList = spacePage.getRecords();
        // 创建新的视图对象分页，保持分页参数一致
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        // 如果记录列表为空，直接返回空分页对象
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceStructMapper.toVoList(spaceList);
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userStructMapper.toUserVo(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    /**
     * 构造空间查询条件
     * @param spaceQueryRequest 查询请求
     * @return 查询条件
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        Integer spaceType = spaceQueryRequest.getSpaceType();


        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }
}
