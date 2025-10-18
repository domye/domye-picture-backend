package com.domye.picture.service.vote.option;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.service.vote.option.model.dto.VoteOptionsAddRequest;
import com.domye.picture.service.vote.option.model.entity.VoteOptions;
import com.domye.picture.service.vote.option.model.vo.VoteOptionsVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Domye
 * @description 针对表【vote_options(投票选项表)】的数据库操作Service
 * @createDate 2025-10-17 21:15:50
 */
public interface VoteOptionsService extends IService<VoteOptions> {

    boolean addVoteOptions(VoteOptionsAddRequest voteOptionsAddRequest, HttpServletRequest request);

    List<VoteOptionsVO> getVoteOptionsList(Long activityId);
}
