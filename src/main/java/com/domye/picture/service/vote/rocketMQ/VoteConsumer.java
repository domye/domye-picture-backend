package com.domye.picture.service.vote.rocketMQ;

import com.domye.picture.service.vote.activity.VoteActivityService;
import com.domye.picture.service.vote.activity.model.entity.VoteActivity;
import com.domye.picture.service.vote.option.VoteOptionService;
import com.domye.picture.service.vote.option.model.entity.VoteOption;
import com.domye.picture.service.vote.record.VoteRecordService;
import com.domye.picture.service.vote.record.model.dto.VoteRequest;
import com.domye.picture.service.vote.record.model.entity.VoteRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

@Component
@RocketMQMessageListener(
        topic = "vote_topic",
        consumerGroup = "vote-consumer-group",
        messageModel = MessageModel.CLUSTERING
)
@Slf4j
public class VoteConsumer implements RocketMQListener<VoteRequest> {
    
    public VoteConsumer() {
        log.info("VoteConsumer 初始化成功，准备监听投票消息...");
    }
    @Resource
    private VoteRecordService voteRecordService;
    @Resource
    private VoteActivityService voteActivityService;
    @Resource
    private VoteOptionService voteOptionService;


    @Override
    public void onMessage(VoteRequest request) {
        log.info("接收到投票消息: {}", request);
        try {
            // 保存投票记录
            VoteRecord vote = new VoteRecord();
            vote.setActivityId(request.getActivityId());
            vote.setUserId(request.getUserId());
            vote.setOptionId(request.getOptionId());
            vote.setVoteTime(new Date());
            voteRecordService.save(vote);

            // 更新活动总票数
            VoteActivity activity = voteActivityService.getById(request.getActivityId());
            activity.setTotalVotes(activity.getTotalVotes() + 1);
            voteActivityService.updateById(activity);

            // 更新选项票数
            VoteOption option = voteOptionService.getById(request.getOptionId());
            option.setVoteCount(option.getVoteCount() + 1);
            voteOptionService.updateById(option);
            
            log.info("投票消息处理完成: {}", request);

        } catch (Exception e) {
            log.error("处理投票消息失败", e);
            // 可以考虑重试或发送到死信队列
        }
    }
}
