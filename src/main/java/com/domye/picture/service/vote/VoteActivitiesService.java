package com.domye.picture.service.vote;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.service.vote.model.dto.VoteActivitiesAddRequest;
import com.domye.picture.service.vote.model.dto.VoteActivitiesUpdateRequest;
import com.domye.picture.service.vote.model.entity.VoteActivities;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Domye
 * @description 针对表【vote_activities(投票活动表)】的数据库操作Service
 * @createDate 2025-10-17 21:15:50
 */
public interface VoteActivitiesService extends IService<VoteActivities> {


    //新建活动
    Long createVoteActivities(VoteActivitiesAddRequest voteActivitiesAddRequest, HttpServletRequest request);

    //更新活动
    void updateVoteActivities(VoteActivitiesUpdateRequest voteActivitiesUpdateRequest, HttpServletRequest request);
}
