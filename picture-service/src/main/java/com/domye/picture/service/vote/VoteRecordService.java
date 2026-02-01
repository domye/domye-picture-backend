package com.domye.picture.service.vote;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.service.vote.model.dto.VoteRequest;
import com.domye.picture.service.vote.model.entity.VoteRecord;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Domye
 * @description 针对表【vote_records(投票记录表)】的数据库操作Service
 * @createDate 2025-10-17 21:15:50
 */
public interface VoteRecordService extends IService<VoteRecord> {


    void submitVote(VoteRequest voteRequest, HttpServletRequest request);

    boolean deleteByActivityId(Long id);
}
