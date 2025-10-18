package com.domye.picture.service.vote.record.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.mapper.VoteRecordsMapper;
import com.domye.picture.service.vote.record.VoteRecordService;
import com.domye.picture.service.vote.record.model.dto.VoteEventRequest;
import com.domye.picture.service.vote.record.model.dto.VoteRequest;
import com.domye.picture.service.vote.record.model.entity.VoteRecord;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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
    private RabbitTemplate rabbitTemplate;

    @Override
    public void submitVote(VoteRequest request) {
        Long activityId = request.getActivityId();
        Long userId = request.getUserId();
        Long optionId = request.getOptionId();
        //TODO基础检验
        //TODO反作弊
        //分布式锁防止重复投票
        String lockKey = String.format("vote:lock:%d:%d", activityId, userId);
        boolean lockAcquired = false;
        try {
            lockAcquired = Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", Duration.ofSeconds(10)));
            Throw.throwIf(!lockAcquired, ErrorCode.SYSTEM_ERROR, "投票过于频繁");
            //原子锁操作
            boolean voteSuccess = recordVote(activityId, userId, optionId);
            Throw.throwIf(!voteSuccess, ErrorCode.OPERATION_ERROR, "您已经投过票了");

            // 异步持久化到数据库
            VoteEventRequest vote = new VoteEventRequest();
            vote.setActivityId(activityId);
            vote.setUserId(userId);
            vote.setOptionId(optionId);
            vote.setVoteTime(new Date());

            // 发送到消息队列异步处理
            rabbitTemplate.convertAndSend("vote.record.exchange",
                    "vote.record.save", vote);

            //实时统计更新
            vote.setIncrement(1);
            rabbitTemplate.convertAndSend("vote.statistics.exchange",
                    "vote.statistics.update",
                    vote);
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