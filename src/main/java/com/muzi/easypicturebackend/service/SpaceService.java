package com.muzi.easypicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzi.easypicturebackend.model.dto.space.SpaceAddRequest;
import com.muzi.easypicturebackend.model.dto.space.SpaceQueryRequest;
import com.muzi.easypicturebackend.model.entity.Space;
import com.muzi.easypicturebackend.model.entity.User;
import com.muzi.easypicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 57242
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2024-12-20 21:49:40
*/
public interface SpaceService extends IService<Space> {

    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    SpaceVO getSpaceVO(Space Space, HttpServletRequest request);

    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    void validSpace(Space space, boolean add);

    void fillSpaceBySpaceLevel(Space space);
}
