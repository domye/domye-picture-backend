package com.domye.picture.controller.vote;

import com.domye.picture.common.BaseResponse;
import com.domye.picture.common.Result;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.service.vote.VoteActivitiesService;
import com.domye.picture.service.vote.model.dto.VoteActivitiesAddRequest;
import com.domye.picture.service.vote.model.dto.VoteActivitiesUpdateRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/vate")
public class voteController {

    private final VoteActivitiesService voteActivitiesService;

    public voteController(VoteActivitiesService voteActivitiesService) {
        this.voteActivitiesService = voteActivitiesService;
    }

    @PostMapping("/activity/add")
    public BaseResponse<Long> addVoteActivities(@RequestBody VoteActivitiesAddRequest voteActivitiesAddRequest, HttpServletRequest request) {

        Throw.throwIf(voteActivitiesAddRequest == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        Long id = voteActivitiesService.createVoteActivities(voteActivitiesAddRequest, request);
        return Result.success(id);

    }

    @PostMapping("/activity/update")
    public BaseResponse<String> updateVoteActivities(@RequestBody VoteActivitiesUpdateRequest voteActivitiesUpdateRequest, HttpServletRequest request) {
        Throw.throwIf(voteActivitiesUpdateRequest == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        voteActivitiesService.updateVoteActivities(voteActivitiesUpdateRequest, request);
        return Result.success("操作成功");
    }

    @PostMapping("/activity/delete")
    public BaseResponse<String> deleteVoteActivities(Long id) {
        Throw.throwIf(id == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        boolean a = voteActivitiesService.removeById(id);
        Throw.throwIf(!a, ErrorCode.PARAMS_ERROR, "删除失败");
        return Result.success("操作成功");
    }
}
