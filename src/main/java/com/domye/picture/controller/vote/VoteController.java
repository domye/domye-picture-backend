package com.domye.picture.controller.vote;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domye.picture.auth.annotation.AuthCheck;
import com.domye.picture.common.BaseResponse;
import com.domye.picture.common.Result;
import com.domye.picture.constant.UserConstant;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.service.vote.VoteActivityService;
import com.domye.picture.service.vote.VoteRecordService;
import com.domye.picture.service.vote.model.dto.VoteActivityAddRequest;
import com.domye.picture.service.vote.model.dto.VoteActivityQueryRequest;
import com.domye.picture.service.vote.model.dto.VoteRequest;
import com.domye.picture.service.vote.model.entity.VoteActivity;
import com.domye.picture.service.vote.model.vo.VoteActivityDetailVO;
import com.domye.picture.service.vote.model.vo.VoteActivityVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/vote/activity")
public class VoteController {

    @Resource
    VoteActivityService activitiesService;
    @Resource
    private VoteRecordService voteRecordService;

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
    //TODO停止投票
    //TODO获取投票结果

}
