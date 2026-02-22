package com.domye.picture.api.controller;


import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.rank.UserActivityScoreQueryRequest;
import com.domye.picture.model.vo.rank.UserActiveRankItemVO;
import com.domye.picture.service.api.rank.RankService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/rank")
@RequiredArgsConstructor
public class RankController {

    final RankService rankService;


    /**
     * @param userActivityScoreQueryRequest
     * @param request
     * @return
     */
    @Operation(summary = "获取用户活跃排行榜")
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
