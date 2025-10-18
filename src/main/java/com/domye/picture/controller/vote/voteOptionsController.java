package com.domye.picture.controller.vote;

import com.domye.picture.common.BaseResponse;
import com.domye.picture.common.Result;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.service.vote.option.VoteOptionsService;
import com.domye.picture.service.vote.option.model.dto.VoteOptionsAddRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/vate/options")
public class voteOptionsController {
    @Resource
    private VoteOptionsService voteOptionsService;

    @PostMapping("/add")
    public BaseResponse<String> addVoteOptions(VoteOptionsAddRequest voteOptionsAddRequest, HttpServletRequest request) {
        Throw.throwIf(voteOptionsAddRequest == null, ErrorCode.PARAMS_ERROR);
        voteOptionsService.addVoteOptions(voteOptionsAddRequest, request);
        return Result.success("添加成功");
    }

    @PostMapping("/delete")
    public BaseResponse<String> deleteVoteOptions(Long id, HttpServletRequest request) {
        Throw.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        voteOptionsService.removeById(id);
        return Result.success("删除成功");
    }
    
}
