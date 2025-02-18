package com.muzi.easypicturebackend.model.dto.spaceUser;

import lombok.Data;

import java.io.Serializable;

/**
 * 申请加入空间请求
 */
@Data
public class SpaceUserJoinRequest implements Serializable {

    /**
     * 空间ID
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
