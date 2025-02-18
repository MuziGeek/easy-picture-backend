package com.muzi.easypicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzi.easypicturebackend.model.dto.spaceUser.SpaceUserAddRequest;
import com.muzi.easypicturebackend.model.dto.spaceUser.SpaceUserAuditRequest;
import com.muzi.easypicturebackend.model.dto.spaceUser.SpaceUserJoinRequest;
import com.muzi.easypicturebackend.model.dto.spaceUser.SpaceUserQueryRequest;
import com.muzi.easypicturebackend.model.entity.SpaceUser;
import com.muzi.easypicturebackend.model.entity.User;
import com.muzi.easypicturebackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 57242
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-01-29 11:05:01
*/
public interface SpaceUserService extends IService<SpaceUser> {


    /**
     * 创建空间成员
     *
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 校验空间成员
     *
     * @param spaceUser
     * @param add       是否为创建时检验
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 获取空间成员包装类（单条）
     *
     * @param spaceUser
     * @param request
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间成员包装类（列表）
     *
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 获取查询对象
     *
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    boolean isSpaceMember(long userId, long spaceId);

    List<User> getSpaceMembers(long spaceId);

    /**
     * 审核空间成员申请
     * @param spaceUserAuditRequest 审核请求
     * @param loginUser 当前登录用户
     * @return 是否审核成功
     */
    boolean auditSpaceUser(SpaceUserAuditRequest spaceUserAuditRequest, User loginUser);

    /**
     * 申请加入空间
     * @param spaceUserJoinRequest 申请请求
     * @param loginUser 当前登录用户
     * @return 是否申请成功
     */
    boolean joinSpace(SpaceUserJoinRequest spaceUserJoinRequest, User loginUser);
}
