package com.domye.picture.api.controller;


import com.domye.picture.api.service.rank.RankService;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.rank.dto.UserActivityScoreQueryRequest;
import com.domye.picture.model.rank.vo.UserActiveRankItemVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/rank")
public class RankController {
    @Resource
    private RankService rankService;


    /**
     * @param userActivityScoreQueryRequest
     * @param request
     * @return
     */
    @ApiOperation("获取用户活跃排行榜")
    @GetMapping("/UserActivityScore")
    public BaseResponse<List<UserActiveRankItemVO>> getUserActivityScore(UserActivityScoreQueryRequest userActivityScoreQueryRequest, HttpServletRequest request) {
        Throw.throwIf(userActivityScoreQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int value = userActivityScoreQueryRequest.getValue();
        int size = userActivityScoreQueryRequest.getSize();
        // 查询数据库
        List<UserActiveRankItemVO> rank = rankService.queryRankList(value, size);
        // 获取封装类
        return Result.success(rank);
    }

}
