package com.domye.picture.controller;

import com.domye.picture.common.BaseResponse;
import com.domye.picture.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return Result.success("ok");
    }
}
