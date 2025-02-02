package com.muzi.easypicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.muzi.easypicturebackend.common.BaseResponse;
import com.muzi.easypicturebackend.common.ResultUtils;
import com.muzi.easypicturebackend.model.dto.comments.CommentsAddRequest;
import com.muzi.easypicturebackend.model.dto.comments.CommentsDeleteRequest;
import com.muzi.easypicturebackend.model.dto.comments.CommentsLikeRequest;
import com.muzi.easypicturebackend.model.dto.comments.CommentsQueryRequest;
import com.muzi.easypicturebackend.model.vo.CommentsVO;
import com.muzi.easypicturebackend.service.CommentsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/comments")
public class CommentsController {
    @Resource
    private CommentsService commentsService;

    /**
     * 查询指定图片id的评论
     */
    @PostMapping("/query")
    public BaseResponse<Page<CommentsVO>> queryComment(@RequestBody CommentsQueryRequest commentsQueryRequest, HttpServletRequest request) {
        return ResultUtils.success(commentsService.queryComment(commentsQueryRequest, request));
    }

    /**
     * 添加评论
     */
    @PostMapping("/add")
    public BaseResponse<Boolean> addComment(@RequestBody CommentsAddRequest commentsAddRequest, HttpServletRequest request) {
        return ResultUtils.success(commentsService.addComment(commentsAddRequest, request));
    }

    /**
     * 删除评论
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteComment(@RequestBody CommentsDeleteRequest commentsDeleteRequest, HttpServletRequest request) {
        return ResultUtils.success(commentsService.deleteComment(commentsDeleteRequest, request));
    }

    /**
     * 喜欢评论内容
     */
    @PostMapping("/like")
    public BaseResponse<Boolean> likeComment(@RequestBody CommentsLikeRequest commentslikeRequest, HttpServletRequest request) {
        return ResultUtils.success(commentsService.likeComment(commentslikeRequest, request));
    }
}
