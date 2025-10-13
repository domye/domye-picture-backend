package com.domye.picture.controller.rank;

import com.domye.picture.common.BaseResponse;
import com.domye.picture.common.Result;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.service.rank.RankService;
import com.domye.picture.service.rank.model.dto.UserActivityScoreQueryRequest;
import com.domye.picture.service.rank.model.vo.UserActiveRankItemVO;
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
