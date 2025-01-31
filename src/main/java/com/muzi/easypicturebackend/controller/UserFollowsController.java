package com.muzi.easypicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.muzi.easypicturebackend.common.BaseResponse;
import com.muzi.easypicturebackend.common.ResultUtils;
import com.muzi.easypicturebackend.exception.ErrorCode;
import com.muzi.easypicturebackend.exception.ThrowUtils;
import com.muzi.easypicturebackend.model.dto.picture.PictureQueryRequest;
import com.muzi.easypicturebackend.model.dto.userfollows.UserFollowsAddRequest;
import com.muzi.easypicturebackend.model.dto.userfollows.UserFollowsIsFollowsRequest;
import com.muzi.easypicturebackend.model.dto.userfollows.UserfollowsQueryRequest;
import com.muzi.easypicturebackend.model.entity.Picture;
import com.muzi.easypicturebackend.model.enums.PictureReviewStatusEnum;
import com.muzi.easypicturebackend.model.vo.FollowersAndFansVO;
import com.muzi.easypicturebackend.model.vo.PictureVO;
import com.muzi.easypicturebackend.model.vo.UserVO;
import com.muzi.easypicturebackend.service.PictureService;
import com.muzi.easypicturebackend.service.UserfollowsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/userfollows")
public class UserFollowsController {
   @Resource
   private UserfollowsService userfollowsService;
   @Resource
   private PictureService pictureService;

   /**
    * 关注、取关
    */
   @PostMapping("/adduserfollows")
   public BaseResponse<Boolean> addUserFollows(@RequestBody UserFollowsAddRequest userFollowsAddRequest){
      return ResultUtils.success(userfollowsService.addUserFollows(userFollowsAddRequest));
   }

   /**
    * 查找是否关注
    */
   @PostMapping("/findisfollow")
   public BaseResponse<Boolean> findIsFollow(@RequestBody UserFollowsIsFollowsRequest userFollowsIsFollowsRequest){
      return ResultUtils.success(userfollowsService.findIsFollow(userFollowsIsFollowsRequest));
   }

   /**
    * 得到关注,粉丝列表
    */
   @PostMapping("/getfolloworfanlist")
   public BaseResponse<Page<UserVO>> getFollowOrFanList(@RequestBody UserfollowsQueryRequest userfollowsQueryRequest){
      return ResultUtils.success(userfollowsService.getFollowOrFanList(userfollowsQueryRequest));
   }

   /**
    * 得到关注和粉丝数量
    */
   @PostMapping("/getfollowandfanscount/{id}")
   public BaseResponse<FollowersAndFansVO> getFollowAndFansCount(@PathVariable Long id){
      return ResultUtils.success(userfollowsService.getFollowAndFansCount(id));

   }

   /**
    * 得到关注或者粉丝的公共的图片数据
    */
   @PostMapping("/getfolloworfanpicture")
   public BaseResponse<Page<PictureVO>> getFollowOrFanPicture(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request){
      long current = pictureQueryRequest.getCurrent();
      long size = pictureQueryRequest.getPageSize();
      // 限制爬虫
      ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);


      ThrowUtils.throwIf(pictureQueryRequest.getUserId() == null, ErrorCode.PARAMS_ERROR, "用户id不能为空");
      pictureQueryRequest.setUserId(pictureQueryRequest.getUserId());
      pictureQueryRequest.setNullSpaceID(true);
      pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

      // 查询数据库
      Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
              pictureService.getQueryWrapper(pictureQueryRequest));
      // 获取封装类
      return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
   }
}
