package com.domye.picture.service.vote.option.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.mapper.VoteOptionsMapper;
import com.domye.picture.service.user.UserService;
import com.domye.picture.service.vote.option.VoteOptionService;
import com.domye.picture.service.vote.option.model.dto.VoteOptionAddRequest;
import com.domye.picture.service.vote.option.model.entity.VoteOption;
import com.domye.picture.service.vote.option.model.vo.VoteOptionVO;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Domye
 * @description 针对表【vote_options(投票选项表)】的数据库操作Service实现
 * @createDate 2025-10-17 21:15:50
 */
@Service
public class VoteOptionServiceImpl extends ServiceImpl<VoteOptionsMapper, VoteOption>
        implements VoteOptionService {

    private final UserService userService;

    public VoteOptionServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean addVoteOptions(VoteOptionAddRequest voteOptionAddRequest, HttpServletRequest request) {
//        User user = userService.getLoginUser(request);
        //TODO 鉴权
        VoteOption voteOption = new VoteOption();
        voteOption.setActivityId(voteOptionAddRequest.getActivityId());
        voteOption.setOptionText(voteOptionAddRequest.getOptionText());
        voteOption.setCreateTime(new Date());
        return save(voteOption);
    }

    @Override
    public List<VoteOptionVO> getVoteOptionsList(Long activityId) {
        QueryWrapper<VoteOption> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("activityId", activityId);
        List<VoteOption> voteOptions = this.baseMapper.selectList(queryWrapper);
        List<VoteOptionVO> voteOptionVO = voteOptions.stream().map(VoteOptionVO::objToVo).collect(Collectors.toList());
        return voteOptionVO;
    }
    
    @Override
    public boolean incrementVoteCount(Long optionId, int increment) {
        UpdateWrapper<VoteOption> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", optionId);
        updateWrapper.setSql("vote_count = vote_count + " + increment);
        return update(updateWrapper);
    }
}