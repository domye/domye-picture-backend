package com.domye.picture.service.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.domye.picture.model.entity.comment.Comments;
import com.domye.picture.model.entity.picture.Picture;
import com.domye.picture.model.enums.PictureReviewStatusEnum;
import com.domye.picture.service.mapper.CommentsMapper;
import com.domye.picture.service.mapper.PictureMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图片热度计算定时任务
 * 每小时更新所有图片的热度分数
 * 热度公式：评论数 * 5 + 点赞数 * 3
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PictureHotScoreJob {

    private final PictureMapper pictureMapper;
    private final CommentsMapper commentsMapper;

    /**
     * 热度权重：评论数权重
     */
    private static final int COMMENT_WEIGHT = 5;

    /**
     * 批量更新大小
     */
    private static final int BATCH_SIZE = 500;

    /**
     * 每小时执行一次热度更新
     * cron: 0 0 * * * ? 表示每小时的第0分第0秒执行
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void updateHotScore() {
        log.info("开始更新图片热度分数...");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 查询所有审核通过的图片
            QueryWrapper<Picture> pictureQuery = new QueryWrapper<>();
            pictureQuery.eq("reviewStatus", PictureReviewStatusEnum.PASS.getValue())
                    .select("id");
            List<Picture> pictures = pictureMapper.selectList(pictureQuery);

            if (pictures.isEmpty()) {
                log.info("没有需要更新热度的图片");
                return;
            }

            log.info("共有 {} 张图片需要更新热度", pictures.size());

            // 2. 查询所有图片的评论数
            Map<Long, Integer> commentCountMap = getCommentCountMap();

            // 3. 批量更新热度分数
            int updateCount = 0;
            for (Picture picture : pictures) {
                Long pictureId = picture.getId();
                int commentCount = commentCountMap.getOrDefault(pictureId, 0);

                // 计算热度：评论数 * 5 + 点赞数 * 3
                // TODO: 后续添加点赞数统计
                int hotScore = commentCount * COMMENT_WEIGHT;

                // 更新热度分数
                Picture updatePicture = new Picture();
                updatePicture.setId(pictureId);
                updatePicture.setHotScore(hotScore);
                pictureMapper.updateById(updatePicture);

                updateCount++;

                // 每批次打印进度
                if (updateCount % BATCH_SIZE == 0) {
                    log.info("已更新 {} 张图片的热度", updateCount);
                }
            }

            long endTime = System.currentTimeMillis();
            log.info("热度更新完成，共更新 {} 张图片，耗时 {} ms", updateCount, endTime - startTime);

        } catch (Exception e) {
            log.error("更新图片热度分数失败", e);
        }
    }

    /**
     * 获取图片评论数统计
     * 只统计根评论（parentid 为空的评论）
     *
     * @return 图片ID -> 评论数的映射
     */
    private Map<Long, Integer> getCommentCountMap() {
        Map<Long, Integer> commentCountMap = new HashMap<>();

        // 查询每个图片的根评论数
        QueryWrapper<Comments> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNull("parentid")
                .select("pictureid", "COUNT(*) as replycount")
                .groupBy("pictureid");

        List<Comments> comments = commentsMapper.selectList(queryWrapper);

        for (Comments comment : comments) {
            if (comment.getPictureid() != null) {
                commentCountMap.put(comment.getPictureid(),
                        comment.getReplycount() != null ? comment.getReplycount() : 0);
            }
        }

        return commentCountMap;
    }
}