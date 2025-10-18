package com.domye.picture.service.vote.activity.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.mapper.VoteActivitiesMapper;
import com.domye.picture.service.user.UserService;
import com.domye.picture.service.vote.activity.VoteActivityService;
import com.domye.picture.service.vote.activity.model.dto.VoteActivityAddRequest;
import com.domye.picture.service.vote.activity.model.dto.VoteActivityQueryRequest;
import com.domye.picture.service.vote.activity.model.dto.VoteActivityUpdateRequest;
import com.domye.picture.service.vote.activity.model.entity.VoteActivity;
import com.domye.picture.service.vote.activity.model.enums.VoteActivitiesStatusEnum;
import com.domye.picture.service.vote.activity.model.vo.VoteActivityVO;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Domye
 * @description 针对表【vote_activities(投票活动表)】的数据库操作Service实现
 * @createDate 2025-10-17 21:15:50
 */
@Service
public class VoteActivityServiceImpl extends ServiceImpl<VoteActivitiesMapper, VoteActivity>
        implements VoteActivityService {

    private final UserService userService;

    public VoteActivityServiceImpl(UserService userService) {
        this.userService = userService;
    }

    //新建活动
    @Override
    public Long createVoteActivities(VoteActivityAddRequest voteActivityAddRequest, HttpServletRequest request) {
        VoteActivity voteActivity = new VoteActivity();

        voteActivity.setTitle(voteActivityAddRequest.getTitle());
        voteActivity.setDescription(voteActivityAddRequest.getDescription());
        int MaxVotes = voteActivityAddRequest.getMaxVotesPerUser();
        Throw.throwIf(MaxVotes <= 0, ErrorCode.PARAMS_ERROR, "投票次数不能小于等于0");
        voteActivity.setMaxVotesPerUser(MaxVotes);


        Date startTime = voteActivityAddRequest.getStartTime();
        Throw.throwIf(startTime.before(new Date()), ErrorCode.PARAMS_ERROR, "开始时间不能早于当前时间");
        voteActivity.setStartTime(startTime);
        Date endTime = voteActivityAddRequest.getEndTime();
        Throw.throwIf(endTime.before(startTime), ErrorCode.PARAMS_ERROR, "结束时间不能早于开始时间");
        voteActivity.setEndTime(endTime);
        voteActivity.setStatus(VoteActivitiesStatusEnum.IN_PROGRESS.getValue());

        voteActivity.setSpaceId(voteActivityAddRequest.getSpaceId());
        voteActivity.setCreateUser(userService.getLoginUser(request).getId());
        voteActivity.setCreateTime(new Date());
        voteActivity.setUpdateTime(new Date());
        save(voteActivity);
        return voteActivity.getId();
    }

    //更新活动
    @Override
    public void updateVoteActivities(VoteActivityUpdateRequest voteActivityUpdateRequest, HttpServletRequest request) {
        VoteActivity voteActivity = getById(voteActivityUpdateRequest.getId());
        Throw.throwIf(voteActivity == null, ErrorCode.NOT_FOUND_ERROR, "活动不存在");
        voteActivity.setTitle(voteActivityUpdateRequest.getTitle());
        voteActivity.setDescription(voteActivityUpdateRequest.getDescription());
        int MaxVotes = voteActivityUpdateRequest.getMaxVotesPerUser();
        Throw.throwIf(MaxVotes <= 0, ErrorCode.PARAMS_ERROR, "投票次数不能小于等于0");
        voteActivity.setMaxVotesPerUser(MaxVotes);
        voteActivity.setUpdateTime(new Date());
        updateById(voteActivity);
    }


    /**
     * 获取活动封装类
     */
    @Override
    public VoteActivityVO getVoteActivityVO(VoteActivity voteActivity) {
        return VoteActivityVO.objToVo(voteActivity);
    }

    @Override
    public Page<VoteActivityVO> getVoteActivityVOPage(Page<VoteActivity> voteActivitiesPage) {
        List<VoteActivity> voteActivityList = voteActivitiesPage.getRecords();
        Page<VoteActivityVO> voteActivityVOPage = new Page<>(voteActivitiesPage.getCurrent(), voteActivitiesPage.getSize(), voteActivitiesPage.getTotal());
        if (CollUtil.isEmpty(voteActivityList))
            return voteActivityVOPage;

        List<VoteActivityVO> voteActivityVOList = voteActivityList.stream().map(this::getVoteActivityVO).collect(Collectors.toList());
        voteActivityVOPage.setRecords(voteActivityVOList);
        return voteActivityVOPage;
    }

    /**
     * 构造查询条件
     */
    @Override
    public QueryWrapper<VoteActivity> getQueryWrapper(VoteActivityQueryRequest voteActivityQueryRequest) {
        QueryWrapper<VoteActivity> queryWrapper = new QueryWrapper<>();
        if (voteActivityQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = voteActivityQueryRequest.getId();
        String title = voteActivityQueryRequest.getTitle();
        Long createUser = voteActivityQueryRequest.getCreateUser();
        String description = voteActivityQueryRequest.getDescription();
        Date startTime = voteActivityQueryRequest.getStartTime();
        Date endTime = voteActivityQueryRequest.getEndTime();
        Integer status = voteActivityQueryRequest.getStatus();
        Long spaceId = voteActivityQueryRequest.getSpaceId();
        String searchText = voteActivityQueryRequest.getSearchText();
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(
                    qw -> qw.like("title", searchText)
                            .or()
                            .like("description", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(createUser), "createUser", createUser);
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
        } else {
            queryWrapper.isNull("spaceId");
        }
        queryWrapper.like(StrUtil.isNotBlank(title), "title", title);
        queryWrapper.like(StrUtil.isNotBlank(description), "description", description);
        queryWrapper.eq(ObjUtil.isNotEmpty(status), "status", status);
        queryWrapper.ge(ObjUtil.isNotEmpty(startTime), "startTime", startTime);
        queryWrapper.le(ObjUtil.isNotEmpty(endTime), "endTime", endTime);

        return queryWrapper;
    }
}





