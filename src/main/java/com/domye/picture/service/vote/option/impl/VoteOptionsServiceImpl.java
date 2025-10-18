package com.domye.picture.service.vote.option.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.mapper.VoteOptionsMapper;
import com.domye.picture.service.user.UserService;
import com.domye.picture.service.vote.option.VoteOptionsService;
import com.domye.picture.service.vote.option.model.dto.VoteOptionsAddRequest;
import com.domye.picture.service.vote.option.model.entity.VoteOptions;
import com.domye.picture.service.vote.option.model.vo.VoteOptionsVO;
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
public class VoteOptionsServiceImpl extends ServiceImpl<VoteOptionsMapper, VoteOptions>
        implements VoteOptionsService {

    private final UserService userService;

    public VoteOptionsServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean addVoteOptions(VoteOptionsAddRequest voteOptionsAddRequest, HttpServletRequest request) {
//        User user = userService.getLoginUser(request);
        //TODO 鉴权
        VoteOptions voteOptions = new VoteOptions();
        voteOptions.setActivityId(voteOptionsAddRequest.getActivityId());
        voteOptions.setOptionText(voteOptionsAddRequest.getOptionText());
        voteOptions.setCreateTime(new Date());
        return save(voteOptions);
    }

    @Override
    public List<VoteOptionsVO> getVoteOptionsList(Long activityId) {
        QueryWrapper<VoteOptions> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("activityId", activityId);
        List<VoteOptions> voteOptions = this.baseMapper.selectList(queryWrapper);
        List<VoteOptionsVO> voteOptionsVO = voteOptions.stream().map(VoteOptionsVO::objToVo).collect(Collectors.toList());
        return voteOptionsVO;
    }
}




