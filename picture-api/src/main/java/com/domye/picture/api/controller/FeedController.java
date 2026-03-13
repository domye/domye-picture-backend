package com.domye.picture.api.controller;

import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.mdc.MdcDot;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.feed.FeedQueryRequest;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.vo.feed.FeedVO;
import com.domye.picture.service.api.feed.FeedService;
import com.domye.picture.service.api.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 信息流控制器
 */
@RestController
@RequestMapping("/feed")
@MdcDot(bizCode = "#feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final UserService userService;

    /**
     * 获取信息流
     *
     * @param feedQueryRequest 查询请求
     * @param request          HTTP请求
     * @return 信息流数据
     */
    @PostMapping("/list")
    @Operation(summary = "获取信息流", description = "获取关注流、推荐流或最新流")
    public BaseResponse<FeedVO> getFeed(@RequestBody FeedQueryRequest feedQueryRequest,
                                        HttpServletRequest request) {
        Throw.throwIf(feedQueryRequest == null, ErrorCode.PARAMS_ERROR);

        // 获取当前登录用户（可选）
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (Exception e) {
            // 未登录用户只能查看公开内容
        }

        FeedVO feedVO = feedService.getFeed(feedQueryRequest, loginUser);
        return Result.success(feedVO);
    }
}