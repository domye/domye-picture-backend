package com.domye.picture.api.service.vote.rocketMQ;

import com.domye.picture.api.service.vote.VoteActivityService;
import com.domye.picture.api.service.vote.model.dto.VoteEndRequest;
import com.domye.picture.api.service.vote.model.entity.VoteActivity;
import com.domye.picture.api.service.vote.model.enums.VoteActivitiesStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQListener;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 投票活动结束消息消费者
 */
//@Component
//@RocketMQMessageListener(
//        topic = "vote_end_topic",
//        consumerGroup = "vote-end-consumer-group",
//        messageModel = MessageModel.CLUSTERING
//)
@Slf4j
public class VoteEndConsumer implements RocketMQListener<VoteEndRequest> {

    @Resource
    private VoteActivityService voteActivityService;

    public VoteEndConsumer() {
        log.info("VoteEndConsumer 初始化成功，准备监听投票活动结束消息...");
    }

    @Override
    public void onMessage(VoteEndRequest request) {
        log.info("接收到投票活动结束消息: {}", request);
        try {
            Long activityId = request.getActivityId();
            VoteActivity activity = voteActivityService.getById(activityId);

            if (activity == null) {
                log.error("投票活动不存在，活动ID: {}", activityId);
                return;
            }

            // 检查活动状态，只有进行中的活动才能被设置为已结束
            if (!VoteActivitiesStatusEnum.IN_PROGRESS.getValue().equals(activity.getStatus())) {
                log.warn("投票活动状态不是进行中，无需处理结束逻辑，活动ID: {}, 当前状态: {}",
                        activityId, activity.getStatus());
                return;
            }

            // 更新活动状态为已结束
            activity.setStatus(VoteActivitiesStatusEnum.FINISHED.getValue());
            activity.setUpdateTime(new Date());
            voteActivityService.updateById(activity);

            log.info("投票活动已成功结束，活动ID: {}", activityId);

            // 这里可以添加其他结束后的处理逻辑，例如：
            // 1. 统计投票结果
            // 2. 发送通知给参与者
            // 3. 生成投票报告等

        } catch (Exception e) {
            log.error("处理投票活动结束消息失败", e);
            // 可以考虑重试或发送到死信队列
        }
    }
}
