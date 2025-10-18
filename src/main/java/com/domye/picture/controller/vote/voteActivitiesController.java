package com.domye.picture.controller.vote;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domye.picture.common.BaseResponse;
import com.domye.picture.common.Result;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.service.vote.activity.VoteActivitiesService;
import com.domye.picture.service.vote.activity.model.dto.VoteActivitiesAddRequest;
import com.domye.picture.service.vote.activity.model.dto.VoteActivitiesQueryRequest;
import com.domye.picture.service.vote.activity.model.dto.VoteActivitiesUpdateRequest;
import com.domye.picture.service.vote.activity.model.entity.VoteActivities;
import com.domye.picture.service.vote.activity.model.vo.VoteActivityDetailVO;
import com.domye.picture.service.vote.activity.model.vo.VoteActivityVO;
import com.domye.picture.service.vote.option.VoteOptionsService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/vate/activity")
public class voteActivitiesController {

    private final VoteActivitiesService voteActivitiesService;
    private final VoteOptionsService voteOptionsService;

    public voteActivitiesController(VoteActivitiesService voteActivitiesService, VoteOptionsService voteOptionsService) {
        this.voteActivitiesService = voteActivitiesService;
        this.voteOptionsService = voteOptionsService;
    }

    @PostMapping("/add")
    public BaseResponse<Long> addVoteActivities(@RequestBody VoteActivitiesAddRequest voteActivitiesAddRequest, HttpServletRequest request) {

        Throw.throwIf(voteActivitiesAddRequest == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        Long id = voteActivitiesService.createVoteActivities(voteActivitiesAddRequest, request);
        return Result.success(id);

    }

    @PostMapping("/update")
    public BaseResponse<String> updateVoteActivities(@RequestBody VoteActivitiesUpdateRequest voteActivitiesUpdateRequest, HttpServletRequest request) {
        Throw.throwIf(voteActivitiesUpdateRequest == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        voteActivitiesService.updateVoteActivities(voteActivitiesUpdateRequest, request);
        return Result.success("操作成功");
    }

    @PostMapping("/delete")
    public BaseResponse<String> deleteVoteActivities(Long id) {
        Throw.throwIf(id == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        boolean a = voteActivitiesService.removeById(id);
        Throw.throwIf(!a, ErrorCode.PARAMS_ERROR, "删除失败");
        return Result.success("操作成功");
    }

    @PostMapping("/list/page")
    @ApiOperation(value = "分页获取列表（仅管理员可用）")
    public BaseResponse<Page<VoteActivities>> listVoteActivitiesByPage(@RequestBody VoteActivitiesQueryRequest voteActivitiesQueryRequest) {
        long current = voteActivitiesQueryRequest.getCurrent();
        long size = voteActivitiesQueryRequest.getPageSize();
        // 查询数据库
        Page<VoteActivities> voteActivitiesPage = voteActivitiesService.page(new Page<>(current, size),
                voteActivitiesService.getQueryWrapper(voteActivitiesQueryRequest));
        return Result.success(voteActivitiesPage);
    }

    /** 分页获取脱敏后的信息 **/
    @PostMapping("/list/page/vo")
    @ApiOperation(value = "分页获取脱敏后的信息")
    public BaseResponse<Page<VoteActivityVO>> listVoteActivitiesVOByPage(@RequestBody VoteActivitiesQueryRequest voteActivitiesQueryRequest) {
        long current = voteActivitiesQueryRequest.getCurrent();
        long size = voteActivitiesQueryRequest.getPageSize();
        // 查询数据库
        Page<VoteActivities> voteActivitiesPage = voteActivitiesService.page(new Page<>(current, size),
                voteActivitiesService.getQueryWrapper(voteActivitiesQueryRequest));
        Page<VoteActivityVO> voteActivityVOPage = voteActivitiesService.getVoteActivityVOPage(voteActivitiesPage);

        return Result.success(voteActivityVOPage);
    }

    @GetMapping("/detail/{id}")
    public BaseResponse<VoteActivityDetailVO> getVoteActivityVOById(@PathVariable("id") Long id) {
        Throw.throwIf(id == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        VoteActivities voteActivities = voteActivitiesService.getById(id);
        Throw.throwIf(voteActivities == null, ErrorCode.NOT_FOUND_ERROR, "数据不存在");
        VoteActivityVO voteActivityVO = voteActivitiesService.getVoteActivityVO(voteActivities);
        VoteActivityDetailVO voteActivityDetailVO = new VoteActivityDetailVO();
        BeanUtils.copyProperties(voteActivityVO, voteActivityDetailVO);
        voteActivityDetailVO.setOptions(voteOptionsService.getVoteOptionsList(id));
        return Result.success(voteActivityDetailVO);
    }
}
