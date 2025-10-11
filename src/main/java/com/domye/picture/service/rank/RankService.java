package com.domye.picture.service.rank;

import com.domye.picture.service.rank.model.dto.UserActivityScoreAddRequest;
import com.domye.picture.service.rank.model.vo.UserActiveRankItemVO;
import com.domye.picture.service.user.model.entity.User;

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
