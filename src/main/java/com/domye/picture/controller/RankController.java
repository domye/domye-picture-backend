package com.domye.picture.controller;

import com.domye.picture.common.BaseResponse;
import com.domye.picture.common.Result;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.service.picture.PictureService;
import com.domye.picture.service.rank.RankService;
import com.domye.picture.service.rank.model.dto.UserActivityScoreAddRequest;
import com.domye.picture.service.rank.model.dto.UserActivityScoreQueryRequest;
import com.domye.picture.service.rank.model.vo.UserActiveRankItemVO;
import com.domye.picture.service.user.UserService;
import com.domye.picture.service.user.model.entity.User;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/rank")
public class RankController {
    @Resource
    private UserService userService;
    @Resource
    private RankService rankService;

    @Resource
    private PictureService pictureService;

    @PostMapping("/addActivityScore")
    public BaseResponse<Boolean> addActivityScore(@RequestBody UserActivityScoreAddRequest userActivityScoreAddRequest, HttpServletRequest request) {
        Throw.throwIf(userActivityScoreAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(request);
        Boolean b = rankService.addActivityScore(user, userActivityScoreAddRequest);
        return Result.success(b);
    }


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
