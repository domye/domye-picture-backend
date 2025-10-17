package com.domye.picture.service.vote.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.mapper.VoteActivitiesMapper;
import com.domye.picture.service.user.UserService;
import com.domye.picture.service.vote.VoteActivitiesService;
import com.domye.picture.service.vote.model.dto.VoteActivitiesAddRequest;
import com.domye.picture.service.vote.model.dto.VoteActivitiesUpdateRequest;
import com.domye.picture.service.vote.model.entity.VoteActivities;
import com.domye.picture.service.vote.model.enums.VoteActivitiesStatusEnum;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

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

}




