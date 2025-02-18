package com.muzi.easypicturebackend.model.vo;

import lombok.Data;

@Data
public class MessageCenterVO {
    /**
     * 未读消息总数
     */
    private long totalUnread;

    /**
     * 未读评论数
     */
    private long unreadComments;

    /**
     * 未读点赞数
     */
    private long unreadLikes;

    /**
     * 未读分享数
     */
    private long unreadShares;
}
