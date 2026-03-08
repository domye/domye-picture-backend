package com.domye.picture.service.api.rank;


import com.domye.picture.model.dto.rank.UserActivityScoreAddRequest;
import com.domye.picture.model.vo.rank.UserActiveRankItemVO;
import com.domye.picture.model.vo.rank.UserRankVO;
import com.domye.picture.model.entity.user.User;

import java.util.List;

public interface RankService {
    /**
     * 添加活跃分
     * @param user 用户
     * @param userActivityScoreAddRequest 活跃分请求
     * @return 是否成功
     */
    Boolean addActivityScore(User user, UserActivityScoreAddRequest userActivityScoreAddRequest);

    /**
     * 查询排行榜列表
     * @param value 排行榜类型（1:日榜 2:周榜 3:月榜 4:总榜）
     * @param size 查询数量
     * @return 排行榜列表
     */
    List<UserActiveRankItemVO> queryRankList(int value, int size);

    /**
     * 获取用户在各榜单的排名
     * @param userId 用户ID
     * @return 用户排名信息
     */
    UserRankVO getUserRank(Long userId);
}
