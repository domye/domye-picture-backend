package com.domye.picture.service.impl.vote;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.helper.impl.RedisCache;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.mapper.vote.VoteStructMapper;
import com.domye.picture.service.mapper.VoteActivitiesMapper;
import com.domye.picture.service.api.user.UserService;
import com.domye.picture.service.api.vote.VoteActivityService;
import com.domye.picture.service.api.vote.VoteOptionService;
import com.domye.picture.service.api.vote.VoteRecordService;
import com.domye.picture.model.dto.vote.VoteActivityAddRequest;
import com.domye.picture.model.dto.vote.VoteActivityQueryRequest;
import com.domye.picture.model.dto.vote.VoteOptionAddRequest;
import com.domye.picture.model.entity.vote.VoteActivity;
import com.domye.picture.model.entity.vote.VoteOption;
import com.domye.picture.model.enums.VoteActivitiesStatusEnum;
import com.domye.picture.model.vo.vote.VoteActivityDetailVO;
import com.domye.picture.model.vo.vote.VoteActivityVO;
import com.domye.picture.model.vo.vote.VoteOptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

import static com.domye.picture.common.constant.VoteConstant.*;

/**
 * @author Domye
 * @description 针对表【vote_activities(投票活动表)】的数据库操作Service实现
 * @createDate 2025-10-17 21:15:50
 */
@Service
@RequiredArgsConstructor
public class VoteActivityServiceImpl extends ServiceImpl<VoteActivitiesMapper, VoteActivity>
        implements VoteActivityService {

    final UserService userService;
    final VoteOptionService voteOptionService;
    final VoteRecordService voteRecordService;
    final RedisCache redisCache;
    final VoteStructMapper voteStructMapper;

    /**
     * 创建新活动
     * @param voteActivityAddRequest 活动信息
     * @param request                请求
     * @return 活动id
     */
    @Override
    public Long createVoteActivity(VoteActivityAddRequest voteActivityAddRequest, HttpServletRequest request) {
        VoteActivity voteActivity = new VoteActivity();
        Date now = new Date();
        User user = userService.getLoginUser(request);
        Throw.throwIf(user == null, ErrorCode.NO_AUTH_ERROR);
        voteActivity.setCreateUser(user.getId());

        String title = voteActivityAddRequest.getTitle();
        Throw.throwIf(StrUtil.isBlank(title), ErrorCode.PARAMS_ERROR);
        voteActivity.setTitle(title);
        voteActivity.setDescription(voteActivityAddRequest.getDescription());

        Date startTime = voteActivityAddRequest.getStartTime();
        Date endTime = voteActivityAddRequest.getEndTime();
        Throw.throwIf(startTime == null || endTime == null, ErrorCode.PARAMS_ERROR, "时间不能为空");
        Throw.throwIf(startTime.before(now) || endTime.before(now), ErrorCode.PARAMS_ERROR, "时间不能早于当前时间");
        Throw.throwIf(startTime.after(endTime), ErrorCode.PARAMS_ERROR, "开始时间不能晚于结束时间");

        voteActivity.setStartTime(startTime);
        voteActivity.setEndTime(endTime);

        voteActivity.setStatus(VoteActivitiesStatusEnum.IN_PROGRESS.getValue());
        voteActivity.setCreateTime(now);
        voteActivity.setUpdateTime(now);
        save(voteActivity);
        Long id = voteActivity.getId();
        List<VoteOptionAddRequest> voteOptionAddRequests = voteActivityAddRequest.getOptions();
        voteOptionService.addOptions(voteOptionAddRequests, id);
        redisCache.put(VOTE_ACTIVITY_KEY + id, voteActivity);

        // 发送延迟消息，在活动结束时自动处理
//        VoteEndRequest voteEndRequest = new VoteEndRequest();
//        voteEndRequest.setActivityId(id);

        // 计算延迟时间（毫秒）
//        long delayMillis = endTime.getTime() - System.currentTimeMillis();
//        voteProducer.sendVoteEndDelayMessage(voteEndRequest, delayMillis);
        return id;
    }

    /**
     * 获取活动详情信息
     * @param id 活动id
     * @return 活动详情信息
     */
    @Override
    public VoteActivityDetailVO getActivityDetailVOById(Long id) {
        //从redis中获取
        String jsonStr = (String) redisCache.get(VOTE_ACTIVITY_DETAIL_KEY + id);
        VoteActivityDetailVO voteActivityDetailVO = null;
        List<VoteOptionVO> optionsVO = null;
        //如果redis中没有，则从数据库中获取
        if (StrUtil.isBlank(jsonStr)) {
            // 创建封装类
            String json = (String) redisCache.get(VOTE_ACTIVITY_KEY + id);
            VoteActivity voteActivity = JSON.parseObject(json, VoteActivity.class);
            Throw.throwIf(voteActivity == null, ErrorCode.NOT_FOUND_ERROR);
            voteActivityDetailVO = voteStructMapper.toDetailVo(voteActivity);

            // 获取选项
            List<VoteOption> options = voteOptionService.getVoteOptionsList(id);
            Throw.throwIf(options == null, ErrorCode.NOT_FOUND_ERROR);
            optionsVO = voteStructMapper.toOptionVoList(options);

            voteActivityDetailVO.setOptions(optionsVO);

            //将数据存入redis
            redisCache.put(VOTE_ACTIVITY_DETAIL_KEY + id, voteActivityDetailVO);
        } else {
            //如果redis中有，则直接解析
            voteActivityDetailVO = JSON.parseObject(jsonStr, VoteActivityDetailVO.class);
            optionsVO = voteActivityDetailVO.getOptions();
        }

        //从redis中读取选项票数
        Map<Object, Object> optionHash = redisCache.getHash(VOTE_COUNT_KEY + id);


        if (CollUtil.isEmpty(optionsVO) || CollUtil.isEmpty(optionHash)) {
            return voteActivityDetailVO;
        }

        Long totalVotes = optionsVO.stream()
                .mapToLong(option -> {
                    Object countObj = optionHash.get(option.getId().toString());
                    long count = countObj != null ? Long.parseLong(countObj.toString()) : 0L;
                    option.setVoteCount(count);
                    return count;
                })
                .sum();

        voteActivityDetailVO.setTotalVotes(totalVotes);
        return voteActivityDetailVO;

    }

    @Override
    public void endActivity(Long id) {
        VoteActivity voteActivity = getById(id);
        Throw.throwIf(voteActivity == null, ErrorCode.NOT_FOUND_ERROR);
        Throw.throwIf(!Objects.equals(voteActivity.getStatus(), VoteActivitiesStatusEnum.IN_PROGRESS.getValue()), ErrorCode.PARAMS_ERROR, "活动已结束");
        voteActivity.setStatus(VoteActivitiesStatusEnum.FINISHED.getValue());
        updateById(voteActivity);
        redisCache.remove(VOTE_ACTIVITY_KEY + id);
    }


    /**
     * @param voteActivityQueryRequest 查询条件
     * @return 查询构造器
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
        queryWrapper.like(StrUtil.isNotBlank(title), "title", title);
        queryWrapper.like(StrUtil.isNotBlank(description), "description", description);
        queryWrapper.eq(ObjUtil.isNotEmpty(status), "status", status);
        queryWrapper.ge(ObjUtil.isNotEmpty(startTime), "startTime", startTime);
        queryWrapper.le(ObjUtil.isNotEmpty(endTime), "endTime", endTime);

        return queryWrapper;
    }


    /**
     * 获取活动封装类
     */
    @Override
    public VoteActivityVO getVoteActivityVO(VoteActivity voteActivity) {
        return voteStructMapper.toVo(voteActivity);
    }

    /**
     * 获取活动详情封装类
     * @param voteActivitiesPage 活动列表
     * @return 活动详情封装类
     */
    @Override
    public Page<VoteActivityVO> getVoteActivityVOPage(Page<VoteActivity> voteActivitiesPage) {
        List<VoteActivity> voteActivityList = voteActivitiesPage.getRecords();
        Page<VoteActivityVO> voteActivityVOPage = new Page<>(voteActivitiesPage.getCurrent(), voteActivitiesPage.getSize(), voteActivitiesPage.getTotal());
        if (CollUtil.isEmpty(voteActivityList))
            return voteActivityVOPage;

        List<VoteActivityVO> voteActivityVOList = voteActivityList.stream().map(this::getVoteActivityVO).collect(Collectors.toList());
        voteActivityVOPage.setRecords(voteActivityVOList);
        Collections.reverse(voteActivityVOList);
        return voteActivityVOPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        VoteActivity voteActivity = getById(id);
        Throw.throwIf(voteActivity == null, ErrorCode.NOT_FOUND_ERROR);
        boolean a = voteRecordService.deleteByActivityId(id);
        boolean b = voteOptionService.deleteOptions(id);
        Throw.throwIf(!a || !b, ErrorCode.SYSTEM_ERROR, "删除失败");
        removeById(id);
        redisCache.remove(VOTE_ACTIVITY_KEY + id);
    }

}





