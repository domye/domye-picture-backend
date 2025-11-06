package com.domye.picture.service.vote;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.service.vote.model.dto.VoteOptionAddRequest;
import com.domye.picture.service.vote.model.entity.VoteOption;

import java.util.List;

/**
 * @author Domye
 * @description 针对表【vote_options(投票选项表)】的数据库操作Service
 * @createDate 2025-10-17 21:15:50
 */
public interface VoteOptionService extends IService<VoteOption> {

    List<VoteOption> getVoteOptionsList(Long activityId);

    void addOptions(List<VoteOptionAddRequest> voteOptionAddRequests, Long id);

    boolean deleteOptions(Long activityId);
}