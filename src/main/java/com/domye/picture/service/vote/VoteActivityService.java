package com.domye.picture.service.vote;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.service.vote.model.dto.VoteActivityAddRequest;
import com.domye.picture.service.vote.model.dto.VoteActivityQueryRequest;
import com.domye.picture.service.vote.model.entity.VoteActivity;
import com.domye.picture.service.vote.model.vo.VoteActivityDetailVO;
import com.domye.picture.service.vote.model.vo.VoteActivityVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Domye
 * @description 针对表【vote_activities(投票活动表)】的数据库操作Service
 * @createDate 2025-10-17 21:15:50
 */
public interface VoteActivityService extends IService<VoteActivity> {

    Long createVoteActivity(VoteActivityAddRequest voteActivityAddRequest, HttpServletRequest request);


    VoteActivityDetailVO getActivityDetailVOById(Long id);

    QueryWrapper<VoteActivity> getQueryWrapper(VoteActivityQueryRequest voteActivityQueryRequest);

    VoteActivityVO getVoteActivityVO(VoteActivity voteActivity);

    Page<VoteActivityVO> getVoteActivityVOPage(Page<VoteActivity> voteActivitiesPage);
}
