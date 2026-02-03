package com.domye.picture.api.controller;

import com.domye.picture.auth.annotation.AuthCheck;
import com.domye.picture.common.constant.UserConstant;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.user.FilterListRequest;
import com.domye.picture.model.vo.user.UserVO;
import com.domye.picture.service.user.FilterlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.List;

@RestController
@RequestMapping("/filterList")
@RequiredArgsConstructor
public class FilterListController implements Serializable {
    final FilterlistService filterlistService;

    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> addFilterList(@RequestBody FilterListRequest request) {
        Throw.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Long userId = request.getUserId();
        Long type = request.getType();
        Long mode = request.getMode();
        if (userId == null || type == null || mode == null)
            Throw.throwEx(ErrorCode.PARAMS_ERROR);
        filterlistService.addUserToFilterList(userId, type, mode);
        return Result.success("添加成功");
    }

    @PostMapping("/remove")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> removeFilterList(@RequestBody FilterListRequest request) {
        Throw.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Long userId = request.getUserId();
        Long type = request.getType();
        Long mode = request.getMode();

        filterlistService.removeUserFromFilterList(userId, type, mode);
        if (userId == null || type == null || mode == null)
            Throw.throwEx(ErrorCode.PARAMS_ERROR);
        return Result.success("移除成功");
    }

    @GetMapping("/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<UserVO>> getFilterList(FilterListRequest request) {
        Throw.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Long type = request.getType();
        Long mode = request.getMode();
        if (type == null || mode == null)
            Throw.throwEx(ErrorCode.PARAMS_ERROR);
        return Result.success(filterlistService.queryAllFilterListUsers(type, mode));

    }
}
