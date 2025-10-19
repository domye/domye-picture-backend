package com.domye.picture.service.vote.old;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.service.vote.model.dto.OptionAddRequest;
import com.domye.picture.service.vote.model.entity.VoteOption;

import java.util.List;

/**
 * @author Domye
 * @description 针对表【vote_options(投票选项表)】的数据库操作Service
 * @createDate 2025-10-17 21:15:50
 */
public interface VoteOptionService extends IService<VoteOption> {
    void addOptions(List<OptionAddRequest> optionAddRequests, Long id);
}