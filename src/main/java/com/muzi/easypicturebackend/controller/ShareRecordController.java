package com.muzi.easypicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.muzi.easypicturebackend.common.BaseResponse;
import com.muzi.easypicturebackend.common.ResultUtils;
import com.muzi.easypicturebackend.exception.ErrorCode;
import com.muzi.easypicturebackend.exception.ThrowUtils;
import com.muzi.easypicturebackend.model.dto.share.ShareQueryRequest;
import com.muzi.easypicturebackend.model.dto.share.ShareRequest;
import com.muzi.easypicturebackend.model.entity.User;
import com.muzi.easypicturebackend.model.vo.ShareRecordVO;
import com.muzi.easypicturebackend.service.ShareRecordService;
import com.muzi.easypicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/share")
@Slf4j
public class ShareRecordController {

    @Resource
    private ShareRecordService shareRecordService;

    @Resource
    private UserService userService;

    /**
     * 通用分享接口
     */
    @PostMapping("/do")
    public BaseResponse<Boolean> doShare(@RequestBody ShareRequest shareRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        try {
            CompletableFuture<Boolean> future = shareRecordService.doShare(shareRequest, loginUser.getId());
            return ResultUtils.success(true);
        } catch (Exception e) {
            log.error("Error in doShare controller: ", e);
            return ResultUtils.success(false);
        }
    }

    /**
     * 获取分享状态
     */
    @GetMapping("/status/{targetType}/{targetId}")
    public BaseResponse<Boolean> getShareStatus(
            @PathVariable("targetType") Integer targetType,
            @PathVariable("targetId") Long targetId,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        boolean isShared = shareRecordService.isContentShared(targetId, targetType, loginUser.getId());
        return ResultUtils.success(isShared);
    }

    /**
     * 获取未读分享消息
     */
    @GetMapping("/unread")
    public BaseResponse<List<ShareRecordVO>> getUnreadShares(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        List<ShareRecordVO> unreadShares = shareRecordService.getAndClearUnreadShares(loginUser.getId());
        return ResultUtils.success(unreadShares);
    }

    /**
     * 获取用户被分享历史
     */
    @PostMapping("/history")
    public BaseResponse<Page<ShareRecordVO>> getShareHistory(@RequestBody ShareQueryRequest shareQueryRequest,
                                                             HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 限制爬虫
        long size = shareQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<ShareRecordVO> shareHistory = shareRecordService.getUserShareHistory(shareQueryRequest, loginUser.getId());
        return ResultUtils.success(shareHistory);
    }

    /**
     * 获取我的分享历史
     */
    @PostMapping("/my/history")
    public BaseResponse<Page<ShareRecordVO>> getMyShareHistory(@RequestBody ShareQueryRequest shareQueryRequest,
                                                               HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 限制爬虫
        long size = shareQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<ShareRecordVO> shareHistory = shareRecordService.getMyShareHistory(shareQueryRequest, loginUser.getId());
        return ResultUtils.success(shareHistory);
    }

    @GetMapping("/unread/count")
    public BaseResponse<Long> getUnreadSharesCount(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        return ResultUtils.success(shareRecordService.getUnreadSharesCount(loginUser.getId()));
    }
}
