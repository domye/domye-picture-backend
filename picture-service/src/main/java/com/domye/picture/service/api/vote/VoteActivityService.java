package com.domye.picture.service.api.vote;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.dto.vote.VoteActivityAddRequest;
import com.domye.picture.model.dto.vote.VoteActivityQueryRequest;
import com.domye.picture.model.entity.vote.VoteActivity;
import com.domye.picture.model.vo.vote.VoteActivityDetailVO;
import com.domye.picture.model.vo.vote.VoteActivityVO;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author Domye
 * @description 针对表【vote_activities(投票活动表)】的数据库操作Service
 * @createDate 2025-10-17 21:15:50
 */
public interface VoteActivityService extends IService<VoteActivity> {

    Long createVoteActivity(VoteActivityAddRequest voteActivityAddRequest, HttpServletRequest request);


    VoteActivityDetailVO getActivityDetailVOById(Long id);

    void endActivity(Long id);

    QueryWrapper<VoteActivity> getQueryWrapper(VoteActivityQueryRequest voteActivityQueryRequest);

    VoteActivityVO getVoteActivityVO(VoteActivity voteActivity);

    Page<VoteActivityVO> getVoteActivityVOPage(Page<VoteActivity> voteActivitiesPage);

    void deleteById(Long id);
}
