package com.domye.picture.service.vote.activity.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.mapper.VoteActivitiesMapper;
import com.domye.picture.service.user.UserService;
import com.domye.picture.service.vote.activity.VoteActivitiesService;
import com.domye.picture.service.vote.activity.model.dto.VoteActivitiesAddRequest;
import com.domye.picture.service.vote.activity.model.dto.VoteActivitiesQueryRequest;
import com.domye.picture.service.vote.activity.model.dto.VoteActivitiesUpdateRequest;
import com.domye.picture.service.vote.activity.model.entity.VoteActivities;
import com.domye.picture.service.vote.activity.model.enums.VoteActivitiesStatusEnum;
import com.domye.picture.service.vote.activity.model.vo.VoteActivityVO;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Domye
 * @description 针对表【vote_activities(投票活动表)】的数据库操作Service实现
 * @createDate 2025-10-17 21:15:50
 */
@Service
public class VoteActivitiesServiceImpl extends ServiceImpl<VoteActivitiesMapper, VoteActivities>
        implements VoteActivitiesService {

    private final UserService userService;

    public VoteActivitiesServiceImpl(UserService userService) {
        this.userService = userService;
    }

    //新建活动
    @Override
    public Long createVoteActivities(VoteActivitiesAddRequest voteActivitiesAddRequest, HttpServletRequest request) {
        VoteActivities voteActivities = new VoteActivities();

        voteActivities.setTitle(voteActivitiesAddRequest.getTitle());
        voteActivities.setDescription(voteActivitiesAddRequest.getDescription());
        int MaxVotes = voteActivitiesAddRequest.getMaxVotesPerUser();
        Throw.throwIf(MaxVotes <= 0, ErrorCode.PARAMS_ERROR, "投票次数不能小于等于0");
        voteActivities.setMaxVotesPerUser(MaxVotes);


        Date startTime = voteActivitiesAddRequest.getStartTime();
        Throw.throwIf(startTime.before(new Date()), ErrorCode.PARAMS_ERROR, "开始时间不能早于当前时间");
        voteActivities.setStartTime(startTime);
        Date endTime = voteActivitiesAddRequest.getEndTime();
        Throw.throwIf(endTime.before(startTime), ErrorCode.PARAMS_ERROR, "结束时间不能早于开始时间");
        voteActivities.setEndTime(endTime);
        voteActivities.setStatus(VoteActivitiesStatusEnum.IN_PROGRESS.getValue());

        voteActivities.setSpaceId(voteActivitiesAddRequest.getSpaceId());
        voteActivities.setCreateUser(userService.getLoginUser(request).getId());
        voteActivities.setCreateTime(new Date());
        voteActivities.setUpdateTime(new Date());
        save(voteActivities);
        return voteActivities.getId();
    }

    //更新活动
    @Override
    public void updateVoteActivities(VoteActivitiesUpdateRequest voteActivitiesUpdateRequest, HttpServletRequest request) {
        VoteActivities voteActivities = getById(voteActivitiesUpdateRequest.getId());
        Throw.throwIf(voteActivities == null, ErrorCode.NOT_FOUND_ERROR, "活动不存在");
        voteActivities.setTitle(voteActivitiesUpdateRequest.getTitle());
        voteActivities.setDescription(voteActivitiesUpdateRequest.getDescription());
        int MaxVotes = voteActivitiesUpdateRequest.getMaxVotesPerUser();
        Throw.throwIf(MaxVotes <= 0, ErrorCode.PARAMS_ERROR, "投票次数不能小于等于0");
        voteActivities.setMaxVotesPerUser(MaxVotes);
        voteActivities.setUpdateTime(new Date());
        updateById(voteActivities);
    }


    /**
     * 获取活动封装类
     */
    @Override
    public VoteActivityVO getVoteActivityVO(VoteActivities voteActivities) {
        return VoteActivityVO.objToVo(voteActivities);
    }

    @Override
    public Page<VoteActivityVO> getVoteActivityVOPage(Page<VoteActivities> voteActivitiesPage) {
        List<VoteActivities> voteActivitiesList = voteActivitiesPage.getRecords();
        Page<VoteActivityVO> voteActivityVOPage = new Page<>(voteActivitiesPage.getCurrent(), voteActivitiesPage.getSize(), voteActivitiesPage.getTotal());
        if (CollUtil.isEmpty(voteActivitiesList))
            return voteActivityVOPage;

        List<VoteActivityVO> voteActivityVOList = voteActivitiesList.stream().map(this::getVoteActivityVO).collect(Collectors.toList());
        voteActivityVOPage.setRecords(voteActivityVOList);
        return voteActivityVOPage;
    }

    /**
     * 构造查询条件
     */
    @Override
    public QueryWrapper<VoteActivities> getQueryWrapper(VoteActivitiesQueryRequest voteActivitiesQueryRequest) {
        QueryWrapper<VoteActivities> queryWrapper = new QueryWrapper<>();
        if (voteActivitiesQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = voteActivitiesQueryRequest.getId();
        String title = voteActivitiesQueryRequest.getTitle();
        Long createUser = voteActivitiesQueryRequest.getCreateUser();
        String description = voteActivitiesQueryRequest.getDescription();
        Date startTime = voteActivitiesQueryRequest.getStartTime();
        Date endTime = voteActivitiesQueryRequest.getEndTime();
        Integer status = voteActivitiesQueryRequest.getStatus();
        Long spaceId = voteActivitiesQueryRequest.getSpaceId();
        String searchText = voteActivitiesQueryRequest.getSearchText();
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(
                    qw -> qw.like("title", searchText)
                            .or()
                            .like("description", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(createUser), "createUser", createUser);
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
        } else {
            queryWrapper.isNull("spaceId");
        }
        queryWrapper.like(StrUtil.isNotBlank(title), "title", title);
        queryWrapper.like(StrUtil.isNotBlank(description), "description", description);
        queryWrapper.eq(ObjUtil.isNotEmpty(status), "status", status);
        queryWrapper.ge(ObjUtil.isNotEmpty(startTime), "startTime", startTime);
        queryWrapper.le(ObjUtil.isNotEmpty(endTime), "endTime", endTime);

        return queryWrapper;
    }
}





