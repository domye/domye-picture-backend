package com.domye.picture.service.api.feed;

import com.domye.picture.model.dto.feed.FeedQueryRequest;
import com.domye.picture.model.vo.feed.FeedVO;
import com.domye.picture.model.entity.user.User;

/**
 * 信息流服务接口
 */
public interface FeedService {

    /**
     * 获取信息流
     *
     * @param feedQueryRequest 查询请求
     * @param loginUser        当前登录用户
     * @return 信息流数据
     */
    FeedVO getFeed(FeedQueryRequest feedQueryRequest, User loginUser);
}