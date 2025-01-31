package com.muzi.easypicturebackend.model.dto.userfollows;

import com.muzi.easypicturebackend.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 *
 * @TableName userfollows
 */
@Data
public class UserfollowsQueryRequest extends PageRequest implements Serializable {
    /**
     * 关注者的用户 ID
     */
    private Long followerId;
    /**
     * 被关注者的用户 ID
     */
    private Long followingId;
    /**
     * 搜索类型,0为关注，1为粉丝
     */
    private Integer searchType;

    private static final long serialVersionUID = 3L;
}
