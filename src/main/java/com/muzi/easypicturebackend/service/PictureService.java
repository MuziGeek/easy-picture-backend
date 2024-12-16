package com.muzi.easypicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzi.easypicturebackend.model.dto.picture.PictureQueryRequest;
import com.muzi.easypicturebackend.model.dto.picture.PictureReviewRequest;
import com.muzi.easypicturebackend.model.dto.picture.PictureUploadRequest;
import com.muzi.easypicturebackend.model.entity.Picture;
import com.muzi.easypicturebackend.model.entity.User;
import com.muzi.easypicturebackend.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 57242
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2024-12-13 23:14:50
*/
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);


    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    void validPicture(Picture picture);
    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(Picture picture, User loginUser);
}
