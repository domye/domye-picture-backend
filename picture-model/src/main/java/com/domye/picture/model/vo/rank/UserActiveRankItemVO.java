package com.domye.picture.model.vo.rank;


import com.domye.picture.model.vo.user.UserVO;
import lombok.Data;

@Data
public class UserActiveRankItemVO {
    /**
     * 排名
     */
    private Integer rank;

    /**
     * 评分
     */
    private double score;

    /**
     * 用户
     */
    private UserVO user;
}
