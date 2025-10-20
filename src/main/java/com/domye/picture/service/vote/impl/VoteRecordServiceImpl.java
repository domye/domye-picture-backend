package com.domye.picture.service.vote.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.mapper.VoteRecordsMapper;
import com.domye.picture.service.vote.VoteRecordService;
import com.domye.picture.service.vote.model.dto.VoteRequest;
import com.domye.picture.service.vote.model.entity.VoteActivity;
import com.domye.picture.service.vote.model.entity.VoteRecord;
import com.domye.picture.service.vote.rocketMQ.VoteProducer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * @author Domye
 * @description 针对表【vote_records(投票记录表)】的数据库操作Service实现
 * @createDate 2025-10-17 21:15:50
 */
@Service
public class VoteRecordServiceImpl extends ServiceImpl<VoteRecordsMapper, VoteRecord>
        implements VoteRecordService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private VoteProducer voteProducer;

    @Override
    public void submitVote(VoteRequest voteRequest, HttpServletRequest request) {
        Long activityId = voteRequest.getActivityId();
        Long userId = voteRequest.getUserId();
        Long optionId = voteRequest.getOptionId();

        Throw.throwIf(activityId == null || userId == null || optionId == null, ErrorCode.PARAMS_ERROR);
        String json = stringRedisTemplate.opsForValue().get("vote:activity:" + activityId);
        VoteActivity voteActivity = JSON.parseObject(json, VoteActivity.class);
        Throw.throwIf(voteActivity == null, ErrorCode.PARAMS_ERROR, "投票活动不存在");
        Throw.throwIf(voteActivity.getStatus() != 1, ErrorCode.PARAMS_ERROR, "投票活动未开始或已结束");
        Date now = new Date();
        Throw.throwIf(now.before(voteActivity.getStartTime()) || now.after(voteActivity.getEndTime()), ErrorCode.PARAMS_ERROR, "投票活动未开始或已结束");
        

        // 分布式锁防止重复投票
        String lockKey = String.format("vote:lock:%d:%d", activityId, userId);
        boolean lockAcquired = false;
        try {
            lockAcquired = Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", Duration.ofSeconds(10)));
            Throw.throwIf(!lockAcquired, ErrorCode.SYSTEM_ERROR, "投票过于频繁");

            // 使用Redis记录投票状态
            boolean voteSuccess = recordVote(activityId, userId, optionId);
            Throw.throwIf(!voteSuccess, ErrorCode.OPERATION_ERROR, "您已经投过票了");

            // 发送消息到MQ
            voteProducer.sendVoteMessage(voteRequest);

        } finally {
            if (lockAcquired) {
                stringRedisTemplate.delete(lockKey);
            }
        }
    }

    /**
     * 记录用户投票
     * 使用SET + HASH组合存储
     */
    public boolean recordVote(Long activityId, Long userId, Long optionId) {
        String userSetKey = String.format("vote:users:%d", activityId);
        String countHashKey = String.format("vote:count:%d", activityId);

        // 使用Lua脚本保证原子性
        String luaScript =
                "if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then " +
                        "  return 0 " +  // 已投票
                        "else " +
                        "  redis.call('SADD', KEYS[1], ARGV[1]) " +
                        "  redis.call('HINCRBY', KEYS[2], ARGV[2], 1) " +
                        "  redis.call('EXPIRE', KEYS[1], ARGV[3]) " +
                        "  redis.call('EXPIRE', KEYS[2], ARGV[3]) " +
                        "  return 1 " +   // 投票成功
                        "end";

        List<String> keys = Arrays.asList(userSetKey, countHashKey);
        List<String> args = Arrays.asList(userId.toString(), optionId.toString(), "86400");

        Long result = stringRedisTemplate.execute(
                new DefaultRedisScript<>(luaScript, Long.class), keys, args.toArray());

        return result != null && result == 1;
    }
}