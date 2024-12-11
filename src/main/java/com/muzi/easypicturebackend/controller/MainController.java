package com.muzi.easypicturebackend.controller;

import com.muzi.easypicturebackend.common.BaseResponse;
import com.muzi.easypicturebackend.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }


    /**
     * 健康检查
     */
    @GetMapping("/health111")
    public BaseResponse<String> health111() {
        return ResultUtils.success("ok");
    }
}



