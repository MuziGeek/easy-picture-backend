package com.muzi.easypicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.muzi.easypicturebackend.common.BaseResponse;
import com.muzi.easypicturebackend.common.ResultUtils;
import com.muzi.easypicturebackend.constant.CrawlerConstant;
import com.muzi.easypicturebackend.exception.ErrorCode;
import com.muzi.easypicturebackend.exception.ThrowUtils;
import com.muzi.easypicturebackend.manager.CrawlerManager;
import com.muzi.easypicturebackend.model.dto.comments.CommentsAddRequest;
import com.muzi.easypicturebackend.model.dto.comments.CommentsDeleteRequest;
import com.muzi.easypicturebackend.model.dto.comments.CommentsLikeRequest;
import com.muzi.easypicturebackend.model.dto.comments.CommentsQueryRequest;
import com.muzi.easypicturebackend.model.entity.User;
import com.muzi.easypicturebackend.model.vo.CommentsVO;
import com.muzi.easypicturebackend.service.CommentsService;
import com.muzi.easypicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/comments")
public class CommentsController {
    @Resource
    private CommentsService commentsService;

    @Resource
    private UserService userService;

    @Resource
    private CrawlerManager crawlerManager;

    /**
     * 查询指定图片的评论列表
     * @param commentsQueryRequest 评论查询参数
     * @param request HTTP请求
     * @return 评论列表（分页）
     */
    @PostMapping("/query")
    public BaseResponse<Page<CommentsVO>> queryComment(@RequestBody CommentsQueryRequest commentsQueryRequest, HttpServletRequest request) {
        // 用户权限校验
        User loginUser = userService.getLoginUser(request);
        if (loginUser != null) {
            String userRole = loginUser.getUserRole();
            ThrowUtils.throwIf(userRole.equals(CrawlerConstant.BAN_ROLE),
                    ErrorCode.NO_AUTH_ERROR, "封禁用户禁止获取数据,请联系管理员");
        }

        // 限制爬虫
        long size = commentsQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);
        crawlerManager.detectNormalRequest(request);

        return ResultUtils.success(commentsService.queryComment(commentsQueryRequest, request));
    }

    /**
     * 添加评论
     * @param commentsAddRequest 评论内容请求
     * @param request HTTP请求
     * @return 添加结果
     */
    @PostMapping("/add")
    public BaseResponse<Boolean> addComment(@RequestBody CommentsAddRequest commentsAddRequest, HttpServletRequest request) {
        // 检测高频操作
        crawlerManager.detectFrequentRequest(request);
        return ResultUtils.success(commentsService.addComment(commentsAddRequest, request));
    }

    /**
     * 删除评论
     * @param commentsDeleteRequest 删除评论请求
     * @param request HTTP请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteComment(@RequestBody CommentsDeleteRequest commentsDeleteRequest, HttpServletRequest request) {
        // 检测高频操作
        crawlerManager.detectFrequentRequest(request);
        return ResultUtils.success(commentsService.deleteComment(commentsDeleteRequest, request));
    }

    /**
     * 点赞评论
     * @param commentslikeRequest 评论点赞请求
     * @param request HTTP请求
     * @return 点赞结果
     */
    @PostMapping("/like")
    public BaseResponse<Boolean> likeComment(@RequestBody CommentsLikeRequest commentslikeRequest, HttpServletRequest request) {
        // 检测高频操作
        crawlerManager.detectFrequentRequest(request);
        return ResultUtils.success(commentsService.likeComment(commentslikeRequest, request));
    }

    /**
     * 获取未读评论列表
     * @param request HTTP请求
     * @return 未读评论列表
     */
    @GetMapping("/unread")
    public BaseResponse<List<CommentsVO>> getUnreadComments(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 检测普通请求
        crawlerManager.detectNormalRequest(request);

        List<CommentsVO> unreadComments = commentsService.getAndClearUnreadComments(loginUser.getId());
        return ResultUtils.success(unreadComments);
    }

    /**
     * 获取未读评论数量
     * @param request HTTP请求
     * @return 未读评论数量
     */
    @GetMapping("/unread/count")
    public BaseResponse<Long> getUnreadCommentsCount(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 检测普通请求
        crawlerManager.detectNormalRequest(request);

        return ResultUtils.success(commentsService.getUnreadCommentsCount(loginUser.getId()));
    }

    /**
     * 获取我的评论历史
     * @param commentsQueryRequest 评论查询参数
     * @param request HTTP请求
     * @return 我的评论历史（分页）
     */
    @PostMapping("/my/history")
    public BaseResponse<Page<CommentsVO>> getMyCommentHistory(@RequestBody CommentsQueryRequest commentsQueryRequest,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 限制爬虫
        long size = commentsQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        crawlerManager.detectNormalRequest(request);

        Page<CommentsVO> commentHistory = commentsService.getMyCommentHistory(commentsQueryRequest, loginUser.getId());
        return ResultUtils.success(commentHistory);
    }

    /**
     * 获取评论我的历史
     * @param commentsQueryRequest 评论查询参数
     * @param request HTTP请求
     * @return 评论我的历史（分页）
     */
    @PostMapping("/commented/history")
    public BaseResponse<Page<CommentsVO>> getCommentedHistory(@RequestBody CommentsQueryRequest commentsQueryRequest,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 限制爬虫
        long size = commentsQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        crawlerManager.detectNormalRequest(request);

        Page<CommentsVO> commentHistory = commentsService.getCommentedHistory(commentsQueryRequest, loginUser.getId());
        return ResultUtils.success(commentHistory);
    }
}

