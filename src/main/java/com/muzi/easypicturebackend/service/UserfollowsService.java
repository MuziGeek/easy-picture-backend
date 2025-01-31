package com.muzi.easypicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzi.easypicturebackend.model.dto.userfollows.UserFollowsAddRequest;
import com.muzi.easypicturebackend.model.dto.userfollows.UserFollowsIsFollowsRequest;
import com.muzi.easypicturebackend.model.dto.userfollows.UserfollowsQueryRequest;
import com.muzi.easypicturebackend.model.entity.Userfollows;
import com.muzi.easypicturebackend.model.vo.FollowersAndFansVO;
import com.muzi.easypicturebackend.model.vo.UserVO;

import java.util.List;

/**
* @author 57242
* @description 针对表【userfollows】的数据库操作Service
* @createDate 2025-01-31 16:27:52
*/
public interface UserfollowsService extends IService<Userfollows> {
    Boolean addUserFollows(UserFollowsAddRequest userFollowsAddRequest);

    Page<UserVO> getFollowOrFanList(UserfollowsQueryRequest userfollowsQueryRequest);

    Boolean findIsFollow(UserFollowsIsFollowsRequest userFollowsIsFollowsRequest);

    List<Long> getFollowList(Long id);

    FollowersAndFansVO getFollowAndFansCount(Long id);
}
