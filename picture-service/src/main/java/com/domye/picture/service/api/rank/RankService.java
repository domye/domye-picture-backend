package com.domye.picture.service.api.rank;


import com.domye.picture.model.dto.rank.UserActivityScoreAddRequest;
import com.domye.picture.model.vo.rank.UserActiveRankItemVO;
import com.domye.picture.model.entity.user.User;

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
