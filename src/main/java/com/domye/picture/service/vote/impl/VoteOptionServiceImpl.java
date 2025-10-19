package com.domye.picture.service.vote.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.mapper.VoteOptionsMapper;
import com.domye.picture.service.vote.VoteOptionService;
import com.domye.picture.service.vote.model.dto.VoteOptionAddRequest;
import com.domye.picture.service.vote.model.entity.VoteOption;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Domye
 * @description 针对表【vote_options(投票选项表)】的数据库操作Service实现
 * @createDate 2025-10-17 21:15:50
 */
@Service
public class VoteOptionServiceImpl extends ServiceImpl<VoteOptionsMapper, VoteOption> implements VoteOptionService {


    @Override
    public List<VoteOption> getVoteOptionsList(Long activityId) {
        Throw.throwIf(activityId == null, ErrorCode.PARAMS_ERROR);
        return list(new QueryWrapper<VoteOption>().eq("activity_id", activityId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addOptions(List<VoteOptionAddRequest> voteOptionAddRequests, Long id) {
        Throw.throwIf(voteOptionAddRequests == null || voteOptionAddRequests.isEmpty(), ErrorCode.PARAMS_ERROR);
        List<VoteOption> voteOptions = voteOptionAddRequests.stream().map(optionAddRequest -> {
            VoteOption voteOption = new VoteOption();
            voteOption.setActivityId(id);
            voteOption.setOptionText(optionAddRequest.getOptionText());
            return voteOption;
        }).collect(Collectors.toList());
        saveBatch(voteOptions);
    }
}