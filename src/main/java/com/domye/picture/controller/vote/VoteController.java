package com.domye.picture.controller.vote;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domye.picture.common.BaseResponse;
import com.domye.picture.common.Result;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.service.vote.ActivityService;
import com.domye.picture.service.vote.VoteRecordService;
import com.domye.picture.service.vote.model.dto.ActivityAddRequest;
import com.domye.picture.service.vote.model.dto.VoteActivityQueryRequest;
import com.domye.picture.service.vote.model.dto.VoteRequest;
import com.domye.picture.service.vote.model.entity.VoteActivity;
import com.domye.picture.service.vote.model.vo.ActivityDetailVO;
import com.domye.picture.service.vote.model.vo.VoteActivityVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/vote/activity")
public class voteActivityController {

    @Resource
    ActivityService activitiesService;
    @Resource
    private VoteRecordService voteRecordService;

    @PostMapping("/create")
    public BaseResponse<Long> addVoteActivities(@RequestBody ActivityAddRequest activityAddRequest, HttpServletRequest request) {

        Throw.throwIf(activityAddRequest == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        Long id = activitiesService.createVoteActivity(activityAddRequest, request);
        return Result.success(id);

    }

    @PostMapping("/vote")
    public BaseResponse<String> addVoteRecord(@RequestBody VoteRequest voteOptionAddRequest, HttpServletRequest request) {
        Throw.throwIf(voteOptionAddRequest == null, ErrorCode.PARAMS_ERROR);
        voteRecordService.submitVote(voteOptionAddRequest);
        return Result.success("添加成功");
    }

    @GetMapping("/detail/{id}")
    public BaseResponse<ActivityDetailVO> getVoteActivities(@PathVariable Long id) {
        ActivityDetailVO activityDetailVO = activitiesService.getActivityDetailVOById(id);
        return Result.success(activityDetailVO);
    }

    @PostMapping("/list/page")
    @ApiOperation(value = "分页获取列表（仅管理员可用）")
    public BaseResponse<Page<VoteActivity>> listVoteActivitiesByPage(@RequestBody VoteActivityQueryRequest voteActivityQueryRequest) {
        long current = voteActivityQueryRequest.getCurrent();
        long size = voteActivityQueryRequest.getPageSize();
        // 查询数据库
        Page<VoteActivity> voteActivitiesPage = activitiesService.page(new Page<>(current, size),
                activitiesService.getQueryWrapper(voteActivityQueryRequest));
        return Result.success(voteActivitiesPage);
    }

    /** 分页获取脱敏后的信息 **/
    @PostMapping("/list/page/vo")
    @ApiOperation(value = "分页获取脱敏后的信息")
    public BaseResponse<Page<VoteActivityVO>> listVoteActivitiesVOByPage(@RequestBody VoteActivityQueryRequest voteActivityQueryRequest) {
        long current = voteActivityQueryRequest.getCurrent();
        long size = voteActivityQueryRequest.getPageSize();
        // 查询数据库
        Page<VoteActivity> voteActivitiesPage = activitiesService.page(new Page<>(current, size),
                activitiesService.getQueryWrapper(voteActivityQueryRequest));
        Page<VoteActivityVO> voteActivityVOPage = activitiesService.getVoteActivityVOPage(voteActivitiesPage);

        return Result.success(voteActivityVOPage);
    }


}
