package com.domye.picture.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domye.picture.auth.annotation.AuthCheck;
import com.domye.picture.common.constant.UserConstant;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.vote.*;
import com.domye.picture.service.api.vote.VoteActivityService;
import com.domye.picture.service.api.vote.VoteRecordService;
import com.domye.picture.model.entity.vote.VoteActivity;
import com.domye.picture.model.vo.vote.VoteActivityDetailVO;
import com.domye.picture.model.vo.vote.VoteActivityVO;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/vote/activity")
@RequiredArgsConstructor
public class VoteController {

    final  VoteActivityService activitiesService;
    final VoteRecordService voteRecordService;

    @PostMapping("/create")
    @ApiOperation(value = "创建投票活动")
    public BaseResponse<Long> addVoteActivities(@RequestBody VoteActivityAddRequest voteActivityAddRequest, HttpServletRequest request) {

        Throw.throwIf(voteActivityAddRequest == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        Long id = activitiesService.createVoteActivity(voteActivityAddRequest, request);
        return Result.success(id);

    }

    @ApiOperation(value = "提交投票")
    @PostMapping("/vote")
    @AuthCheck(mustRole = UserConstant.USER_LOGIN_STATE)
    public BaseResponse<String> addVoteRecord(@RequestBody VoteRequest voteOptionAddRequest, HttpServletRequest request) {
        Throw.throwIf(voteOptionAddRequest == null, ErrorCode.PARAMS_ERROR);
        voteRecordService.submitVote(voteOptionAddRequest, request);
        return Result.success("添加成功");
    }

    @ApiOperation(value = "结束投票")
    @PostMapping("/end")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> endVoteActivities(@RequestBody VoteEndRequest voteEndRequest, HttpServletRequest request) {
        Throw.throwIf(voteEndRequest == null, ErrorCode.PARAMS_ERROR);
        activitiesService.endActivity(voteEndRequest.getActivityId());
        return Result.success("添加成功");
    }

    @ApiOperation(value = "删除投票")
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> deleteVoteActivities(@RequestBody VoteActivityDeleteRequest voteActivityDeleteRequest) {
        Throw.throwIf(voteActivityDeleteRequest == null, ErrorCode.PARAMS_ERROR);
        activitiesService.deleteById(voteActivityDeleteRequest.getActivityId());
        return Result.success("删除成功");
    }


    @ApiOperation(value = "获取投票活动详情")
    @GetMapping("/detail/{id}")
    public BaseResponse<VoteActivityDetailVO> getVoteActivities(@PathVariable Long id) {
        VoteActivityDetailVO voteActivityDetailVO = activitiesService.getActivityDetailVOById(id);
        return Result.success(voteActivityDetailVO);
    }

    @PostMapping("/list/page")
    @ApiOperation(value = "分页获取列表（仅管理员可用）")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
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


    //TODO获取投票结果

}
