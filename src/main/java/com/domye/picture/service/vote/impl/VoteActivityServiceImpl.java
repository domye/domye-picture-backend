package com.domye.picture.service.vote.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.mapper.VoteActivitiesMapper;
import com.domye.picture.service.user.UserService;
import com.domye.picture.service.user.model.entity.User;
import com.domye.picture.service.vote.VoteActivityService;
import com.domye.picture.service.vote.VoteOptionService;
import com.domye.picture.service.vote.model.dto.VoteActivityAddRequest;
import com.domye.picture.service.vote.model.dto.VoteActivityQueryRequest;
import com.domye.picture.service.vote.model.dto.VoteOptionAddRequest;
import com.domye.picture.service.vote.model.entity.VoteActivity;
import com.domye.picture.service.vote.model.entity.VoteOption;
import com.domye.picture.service.vote.model.enums.VoteActivitiesStatusEnum;
import com.domye.picture.service.vote.model.vo.VoteActivityDetailVO;
import com.domye.picture.service.vote.model.vo.VoteActivityVO;
import com.domye.picture.service.vote.model.vo.VoteOptionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.domye.picture.constant.VoteConstant.*;

/**
 * @author Domye
 * @description 针对表【vote_activities(投票活动表)】的数据库操作Service实现
 * @createDate 2025-10-17 21:15:50
 */
@Service
public class VoteActivityServiceImpl extends ServiceImpl<VoteActivitiesMapper, VoteActivity>
        implements VoteActivityService {
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserService userService;
    @Resource
    private VoteOptionService voteOptionService;

    @Override
    @Transactional
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
        stringRedisTemplate.opsForValue().set(VOTE_ACTIVITY_KEY + id, JSON.toJSONString(voteActivity));

        //TODO rocketmq设计定时消息处理
        return id;
    }

    @Override
    public VoteActivityDetailVO getActivityDetailVOById(Long id) {
        //从redis中获取
        String jsonStr = stringRedisTemplate.opsForValue().get(VOTE_ACTIVITY_DETAIL_KEY + id);
        VoteActivityDetailVO voteActivityDetailVO = null;
        List<VoteOptionVO> optionsVO = null;
        //如果redis中没有，则从数据库中获取
        if (StrUtil.isBlank(jsonStr)) {
            // 创建封装类
            String json = stringRedisTemplate.opsForValue().get(VOTE_ACTIVITY_KEY + id);
            VoteActivity voteActivity = JSON.parseObject(json, VoteActivity.class);
            Throw.throwIf(voteActivity == null, ErrorCode.NOT_FOUND_ERROR);
            voteActivityDetailVO = new VoteActivityDetailVO();
            BeanUtils.copyProperties(voteActivity, voteActivityDetailVO);

            // 获取选项
            List<VoteOption> options = voteOptionService.getVoteOptionsList(id);
            Throw.throwIf(options == null, ErrorCode.NOT_FOUND_ERROR);
            optionsVO = options.stream().map(option -> {
                        VoteOptionVO voteOptionVO = new VoteOptionVO();
                        BeanUtils.copyProperties(option, voteOptionVO);
                        return voteOptionVO;
                    }
            ).collect(Collectors.toList());

            voteActivityDetailVO.setOptions(optionsVO);

            //将数据存入redis
            stringRedisTemplate.opsForValue().set(VOTE_ACTIVITY_DETAIL_KEY + id, JSON.toJSONString(voteActivityDetailVO));
        } else {
            //如果redis中有，则直接解析
            voteActivityDetailVO = JSON.parseObject(jsonStr, VoteActivityDetailVO.class);
            optionsVO = voteActivityDetailVO.getOptions();
        }

        //从redis中读取选项票数
        Map<Object, Object> optionHash = stringRedisTemplate.opsForHash().entries(VOTE_COUNT_KEY + id);


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

}





