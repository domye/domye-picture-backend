package com.domye.picture.controller.vote;

import com.domye.picture.common.BaseResponse;
import com.domye.picture.common.Result;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.service.vote.option.VoteOptionService;
import com.domye.picture.service.vote.record.VoteRecordService;
import com.domye.picture.service.vote.record.model.dto.VoteRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/vate/record")
public class voteRecordController {
    @Resource
    private VoteOptionService voteOptionService;
    @Autowired
    private VoteRecordService voteRecordService;

    @PostMapping("/add")
    public BaseResponse<String> addVoteRecord(@RequestBody VoteRequest voteOptionAddRequest, HttpServletRequest request) {
        Throw.throwIf(voteOptionAddRequest == null, ErrorCode.PARAMS_ERROR);
        voteRecordService.submitVote(voteOptionAddRequest);
        return Result.success("添加成功");
    }

}
