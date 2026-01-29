package com.domye.picture.api.controller;

import com.domye.picture.core.exception.ErrorCode;
import com.domye.picture.core.exception.Throw;
import com.domye.picture.core.result.BaseResponse;
import com.domye.picture.core.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class health {
    @GetMapping
    public BaseResponse<String> health() {
        return Result.success("success");
    }

    @GetMapping("/test")
    public void test() {
        Throw.throwEx(ErrorCode.NOT_FOUND_ERROR);
    }
}
