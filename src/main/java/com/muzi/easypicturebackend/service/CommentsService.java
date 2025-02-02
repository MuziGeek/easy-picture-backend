package com.muzi.easypicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzi.easypicturebackend.model.dto.comments.CommentsAddRequest;
import com.muzi.easypicturebackend.model.dto.comments.CommentsDeleteRequest;
import com.muzi.easypicturebackend.model.dto.comments.CommentsLikeRequest;
import com.muzi.easypicturebackend.model.dto.comments.CommentsQueryRequest;
import com.muzi.easypicturebackend.model.entity.Comments;
import com.muzi.easypicturebackend.model.vo.CommentsVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 57242
* @description 针对表【comments】的数据库操作Service
* @createDate 2025-01-31 17:58:40
*/
public interface CommentsService extends IService<Comments> {
    Boolean addComment(CommentsAddRequest commentsAddRequest, HttpServletRequest request);


    Boolean deleteComment(CommentsDeleteRequest commentsDeleteRequest, HttpServletRequest request);


    Page<CommentsVO> queryComment(CommentsQueryRequest commentsQueryRequest, HttpServletRequest request);

    Boolean likeComment(CommentsLikeRequest commentslikeRequest, HttpServletRequest request);
}
