package com.muzi.easypicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.muzi.easypicturebackend.model.dto.picturelike.PictureLikeRequest;
import com.muzi.easypicturebackend.model.entity.Picturelike;

import java.util.concurrent.CompletableFuture;

/**
* @author 57242
* @description 针对表【pictureLike(点赞表)】的数据库操作Service
* @createDate 2025-01-31 17:58:28
*/
public interface PicturelikeService extends IService<Picturelike> {
    CompletableFuture<Boolean> UserLike(PictureLikeRequest pictureLikeRequest, Long userId);
    CompletableFuture<Boolean> UserShare(String pictureId, Long userId);
}
