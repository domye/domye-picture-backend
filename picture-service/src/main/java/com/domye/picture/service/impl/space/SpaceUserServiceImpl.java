package com.domye.picture.service.impl.space;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.common.exception.BusinessException;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.model.dto.space.SpaceUserAddRequest;
import com.domye.picture.model.dto.space.SpaceUserQueryRequest;
import com.domye.picture.model.entity.space.Space;
import com.domye.picture.model.entity.space.SpaceUser;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.enums.SpaceRoleEnum;
import com.domye.picture.model.vo.space.SpaceUserVO;
import com.domye.picture.model.vo.space.SpaceVO;
import com.domye.picture.model.vo.user.UserVO;
import com.domye.picture.service.mapper.SpaceUserMapper;
import com.domye.picture.service.api.space.SpaceService;
import com.domye.picture.service.api.space.SpaceUserService;
import com.domye.picture.service.api.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Domye
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2025-09-20 10:51:20
 */
@Service
@RequiredArgsConstructor
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {
    final SpaceService spaceService;
    final UserService userService;

    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        //校验参数
        Throw.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);
        validSpaceUser(spaceUser, true);

        // 数据库操作
        boolean result = this.save(spaceUser);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceUserQueryRequest.getId();
        Long userId = spaceUserQueryRequest.getUserId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();


        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        return queryWrapper;
    }

    /**
     * 获取成���封装类（单条）
     * @deprecated 存在 N+1 查询风险，请使用 {@link #getSpaceUserVOList(List)} 批量方法
     */
    @Override
    @Deprecated
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        // 对象转封装类
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        // 关联查询用户信息
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUser(userVO);
        }
        // 关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
            spaceUserVO.setSpace(spaceVO);
        }
        return spaceUserVO;
    }

    //查询封装类
    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {

        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        // 对象列表 => 封装对象列表
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        // 2. 批量查询用户和空间
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));
        // 2. 填充信息
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceUserVO.setUser(userService.getUserVO(user));
            Space space = null;
            if (spaceIdSpaceListMap.containsKey(spaceUserVO.getSpaceId())) {
                space = spaceIdSpaceListMap.get(spaceUserVO.getSpaceId()).get(0);

            }
            spaceUserVO.setSpace(SpaceVO.objectToVo(space));
        });
        return spaceUserVOList;
    }

    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        Throw.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        // 要创建
        if (add) {
            Throw.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            User user = userService.getById(userId);
            Throw.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            Throw.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }

}