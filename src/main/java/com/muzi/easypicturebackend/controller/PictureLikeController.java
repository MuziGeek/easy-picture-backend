package com.muzi.easypicturebackend.controller;


import com.muzi.easypicturebackend.common.BaseResponse;
import com.muzi.easypicturebackend.common.ResultUtils;
import com.muzi.easypicturebackend.exception.ErrorCode;
import com.muzi.easypicturebackend.exception.ThrowUtils;
import com.muzi.easypicturebackend.model.dto.picturelike.PictureLikeRequest;
import com.muzi.easypicturebackend.model.entity.User;
import com.muzi.easypicturebackend.service.PicturelikeService;
import com.muzi.easypicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/picturelike")
public class PictureLikeController {
    @Resource
    private PicturelikeService pictureLikeService;

    @Resource
    private UserService userService;

    /**
     * 用户点赞
     */
    @PostMapping("/like")
    public BaseResponse<Boolean> UserLike(@RequestBody PictureLikeRequest pictureLikeRequest, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        Long userId = user.getId();
        try {
            CompletableFuture<Boolean> future = pictureLikeService.UserLike(pictureLikeRequest, userId);
            // 这里不等待结果，直接返回成功
            return ResultUtils.success(true);
        } catch (Exception e) {
            log.error("Error in UserLike controller: ", e);
            return (BaseResponse<Boolean>) ResultUtils.error(ErrorCode.SYSTEM_ERROR, "点赞操作失败");
        }
    }

    /**
     * 用户分享
     */
    @PostMapping("/share/{pictureId}")
    public BaseResponse<Boolean> UserShare(@PathVariable String pictureId, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        Long userId = user.getId();
        try {
            CompletableFuture<Boolean> future = pictureLikeService.UserShare(pictureId, userId);
            // 这里不等待结果，直接返回成功
            return ResultUtils.success(true);
        } catch (Exception e) {
            log.error("Error in UserShare controller: ", e);
            return (BaseResponse<Boolean>) ResultUtils.error(ErrorCode.SYSTEM_ERROR, "分享操作失败");
        }
    }
}
