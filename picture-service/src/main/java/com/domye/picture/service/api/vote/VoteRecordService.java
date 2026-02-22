package com.domye.picture.service.api.vote;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.dto.vote.VoteRequest;
import com.domye.picture.model.entity.vote.VoteRecord;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author Domye
 * @description 针对表【vote_records(投票记录表)】的数据库操作Service
 * @createDate 2025-10-17 21:15:50
 */
public interface VoteRecordService extends IService<VoteRecord> {


    void submitVote(VoteRequest voteRequest, HttpServletRequest request);

    boolean deleteByActivityId(Long id);
}
