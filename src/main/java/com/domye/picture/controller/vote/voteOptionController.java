package com.domye.picture.controller.vote;

import com.domye.picture.common.BaseResponse;
import com.domye.picture.common.Result;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.service.vote.option.VoteOptionService;
import com.domye.picture.service.vote.option.model.dto.VoteOptionAddRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/vate/options")
public class voteOptionController {
    @Resource
    private VoteOptionService voteOptionService;

    @PostMapping("/add")
    public BaseResponse<String> addVoteOptions(VoteOptionAddRequest voteOptionAddRequest, HttpServletRequest request) {
        Throw.throwIf(voteOptionAddRequest == null, ErrorCode.PARAMS_ERROR);
        voteOptionService.addVoteOptions(voteOptionAddRequest, request);
        return Result.success("添加成功");
    }

    @PostMapping("/delete")
    public BaseResponse<String> deleteVoteOptions(Long id, HttpServletRequest request) {
        Throw.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        voteOptionService.removeById(id);
        return Result.success("删除成功");
    }

}
