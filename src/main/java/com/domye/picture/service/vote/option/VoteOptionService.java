package com.domye.picture.service.vote.option;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.service.vote.option.model.dto.VoteOptionAddRequest;
import com.domye.picture.service.vote.option.model.entity.VoteOption;
import com.domye.picture.service.vote.option.model.vo.VoteOptionVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Domye
 * @description 针对表【vote_options(投票选项表)】的数据库操作Service
 * @createDate 2025-10-17 21:15:50
 */
public interface VoteOptionService extends IService<VoteOption> {

    boolean addVoteOptions(VoteOptionAddRequest voteOptionAddRequest, HttpServletRequest request);

    List<VoteOptionVO> getVoteOptionsList(Long activityId);
    
    /**
     * 增加选项的投票计数
     * @param optionId 选项ID
     * @param increment 增量
     * @return 是否更新成功
     */
    boolean incrementVoteCount(Long optionId, int increment);
}