package com.domye.picture.api.controller;


import com.domye.picture.auth.annotation.AuthCheck;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.rank.UserActivityScoreQueryRequest;
import com.domye.picture.model.vo.rank.UserActiveRankItemVO;
import com.domye.picture.model.vo.rank.UserRankVO;
import com.domye.picture.service.api.rank.RankService;
import com.domye.picture.service.api.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rank")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;
    private final UserService userService;

    /**
     * 获取用户活跃排行榜
     *
     * @param userActivityScoreQueryRequest 查询请求
     * @return 排行榜列表
     */
    @Operation(summary = "获取用户活跃排行榜")
    @GetMapping("/UserActivityScore")
    public BaseResponse<List<UserActiveRankItemVO>> getUserActivityScore(
            UserActivityScoreQueryRequest userActivityScoreQueryRequest) {
        Throw.throwIf(userActivityScoreQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int value = userActivityScoreQueryRequest.getValue();
        int size = userActivityScoreQueryRequest.getSize();
        List<UserActiveRankItemVO> rank = rankService.queryRankList(value, size);
        return Result.success(rank);
    }

    /**
     * 获取当前用户在各榜单的排名
     *
     * @param request HTTP请求
     * @return 用户排名信息
     */
    @Operation(summary = "获取我的排名")
    @GetMapping("/myRank")
    @AuthCheck
    public BaseResponse<UserRankVO> getMyRank(HttpServletRequest request) {
        Long userId = userService.getLoginUser(request).getId();
        UserRankVO userRankVO = rankService.getUserRank(userId);
        return Result.success(userRankVO);
    }
}
