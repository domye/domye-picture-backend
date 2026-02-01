package com.domye.picture.api.service.rank;


import com.domye.picture.model.rank.dto.UserActivityScoreAddRequest;
import com.domye.picture.model.rank.vo.UserActiveRankItemVO;
import com.domye.picture.model.user.entity.User;

import java.util.List;

public interface RankService {
    /**
     * 添加活跃分
     * @param user
     * @param userActivityScoreAddRequest
     * @return
     */
    Boolean addActivityScore(User user, UserActivityScoreAddRequest userActivityScoreAddRequest);

    List<UserActiveRankItemVO> queryRankList(int value, int size);
}
