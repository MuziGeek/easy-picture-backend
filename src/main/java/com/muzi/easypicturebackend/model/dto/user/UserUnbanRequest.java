package com.muzi.easypicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserUnbanRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 操作类型：true-解禁，false-封禁
     */
    private Boolean isUnban;
}
