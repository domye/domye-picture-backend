package com.domye.picture.model.vo.rank;

import lombok.Data;

import java.util.Map;

/**
 * 用户排名信息 VO
 */
@Data
public class UserRankVO {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 各榜单排名和分数
     * key: day/week/month/total
     * value: {rank: xxx, score: xxx}
     */
    private Map<String, Object> ranks;
}