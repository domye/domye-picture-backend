package com.domye.picture.service.vote.option.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.mapper.VoteOptionsMapper;
import com.domye.picture.service.user.UserService;
import com.domye.picture.service.vote.option.VoteOptionsService;
import com.domye.picture.service.vote.option.model.dto.VoteOptionsAddRequest;
import com.domye.picture.service.vote.option.model.dto.VoteOptionsUpdateRequest;
import com.domye.picture.service.vote.option.model.entity.VoteOptions;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

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

    public boolean addVoteOptions(VoteOptionsAddRequest voteOptionsAddRequest, HttpServletRequest request) {
//        User user = userService.getLoginUser(request);
        //TODO 鉴权
        VoteOptions voteOptions = new VoteOptions();
        voteOptions.setActivityId(voteOptionsAddRequest.getActivityId());
        voteOptions.setOptionText(voteOptionsAddRequest.getOptionText());
        voteOptions.setCreateTime(new Date());
        return save(voteOptions);
    }

    public boolean updateVoteOptions(VoteOptionsUpdateRequest voteOptionsUpdateRequest, HttpServletRequest request) {
        //TODO 鉴权
        VoteOptions voteOptions = new VoteOptions();
        voteOptions.setId(voteOptionsUpdateRequest.getOptionId());
        voteOptions.setActivityId(voteOptionsUpdateRequest.getActivityId());
        voteOptions.setOptionText(voteOptionsUpdateRequest.getOptionText());
        return updateById(voteOptions);
    }

}




