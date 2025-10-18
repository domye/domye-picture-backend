package com.domye.picture.controller.vote;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domye.picture.common.BaseResponse;
import com.domye.picture.common.Result;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.service.vote.activity.VoteActivityService;
import com.domye.picture.service.vote.activity.model.dto.VoteActivityAddRequest;
import com.domye.picture.service.vote.activity.model.dto.VoteActivityQueryRequest;
import com.domye.picture.service.vote.activity.model.dto.VoteActivityUpdateRequest;
import com.domye.picture.service.vote.activity.model.entity.VoteActivity;
import com.domye.picture.service.vote.activity.model.vo.VoteActivityDetailVO;
import com.domye.picture.service.vote.activity.model.vo.VoteActivityVO;
import com.domye.picture.service.vote.option.VoteOptionService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/vate/activity")
public class voteActivitiesController {

    private final VoteActivityService voteActivitiesService;
    private final VoteOptionService voteOptionService;

    public voteActivitiesController(VoteActivityService voteActivitiesService, VoteOptionService voteOptionService) {
        this.voteActivitiesService = voteActivitiesService;
        this.voteOptionService = voteOptionService;
    }

    @PostMapping("/add")
    public BaseResponse<Long> addVoteActivities(@RequestBody VoteActivityAddRequest voteActivityAddRequest, HttpServletRequest request) {

        Throw.throwIf(voteActivityAddRequest == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        Long id = voteActivitiesService.createVoteActivities(voteActivityAddRequest, request);
        return Result.success(id);

    }

    @PostMapping("/update")
    public BaseResponse<String> updateVoteActivities(@RequestBody VoteActivityUpdateRequest voteActivityUpdateRequest, HttpServletRequest request) {
        Throw.throwIf(voteActivityUpdateRequest == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        voteActivitiesService.updateVoteActivities(voteActivityUpdateRequest, request);
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
    public BaseResponse<Page<VoteActivity>> listVoteActivitiesByPage(@RequestBody VoteActivityQueryRequest voteActivityQueryRequest) {
        long current = voteActivityQueryRequest.getCurrent();
        long size = voteActivityQueryRequest.getPageSize();
        // 查询数据库
        Page<VoteActivity> voteActivitiesPage = voteActivitiesService.page(new Page<>(current, size),
                voteActivitiesService.getQueryWrapper(voteActivityQueryRequest));
        return Result.success(voteActivitiesPage);
    }

    /** 分页获取脱敏后的信息 **/
    @PostMapping("/list/page/vo")
    @ApiOperation(value = "分页获取脱敏后的信息")
    public BaseResponse<Page<VoteActivityVO>> listVoteActivitiesVOByPage(@RequestBody VoteActivityQueryRequest voteActivityQueryRequest) {
        long current = voteActivityQueryRequest.getCurrent();
        long size = voteActivityQueryRequest.getPageSize();
        // 查询数据库
        Page<VoteActivity> voteActivitiesPage = voteActivitiesService.page(new Page<>(current, size),
                voteActivitiesService.getQueryWrapper(voteActivityQueryRequest));
        Page<VoteActivityVO> voteActivityVOPage = voteActivitiesService.getVoteActivityVOPage(voteActivitiesPage);

        return Result.success(voteActivityVOPage);
    }

    @GetMapping("/detail/{id}")
    public BaseResponse<VoteActivityDetailVO> getVoteActivityVOById(@PathVariable("id") Long id) {
        Throw.throwIf(id == null, ErrorCode.PARAMS_ERROR, "参数不能为空");
        VoteActivity voteActivity = voteActivitiesService.getById(id);
        Throw.throwIf(voteActivity == null, ErrorCode.NOT_FOUND_ERROR, "数据不存在");
        VoteActivityVO voteActivityVO = voteActivitiesService.getVoteActivityVO(voteActivity);
        VoteActivityDetailVO voteActivityDetailVO = new VoteActivityDetailVO();
        BeanUtils.copyProperties(voteActivityVO, voteActivityDetailVO);
        voteActivityDetailVO.setOptions(voteOptionService.getVoteOptionsList(id));
        return Result.success(voteActivityDetailVO);
    }
}
