package com.domye.picture.api.controller.user.filterlist;

import com.domye.picture.api.auth.annotation.AuthCheck;
import com.domye.picture.api.constant.UserConstant;
import com.domye.picture.api.service.user.FilterlistService;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.user.dto.FilterListRequest;
import com.domye.picture.model.user.vo.UserVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.List;

@RestController
@RequestMapping("/filterList")
public class FilterListController implements Serializable {
    @Resource
    private FilterlistService filterlistService;

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
