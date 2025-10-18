package com.domye.picture.service.vote.activity;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.service.vote.activity.model.dto.VoteActivityAddRequest;
import com.domye.picture.service.vote.activity.model.dto.VoteActivityQueryRequest;
import com.domye.picture.service.vote.activity.model.dto.VoteActivityUpdateRequest;
import com.domye.picture.service.vote.activity.model.entity.VoteActivity;
import com.domye.picture.service.vote.activity.model.vo.VoteActivityVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Domye
 * @description 针对表【vote_activities(投票活动表)】的数据库操作Service
 * @createDate 2025-10-17 21:15:50
 */
public interface VoteActivityService extends IService<VoteActivity> {


    //新建活动
    Long createVoteActivities(VoteActivityAddRequest voteActivityAddRequest, HttpServletRequest request);

    //更新活动
    void updateVoteActivities(VoteActivityUpdateRequest voteActivityUpdateRequest, HttpServletRequest request);

    VoteActivityVO getVoteActivityVO(VoteActivity voteActivity);

    Page<VoteActivityVO> getVoteActivityVOPage(Page<VoteActivity> voteActivitiesPage);

    QueryWrapper<VoteActivity> getQueryWrapper(VoteActivityQueryRequest voteActivityQueryRequest);
}
