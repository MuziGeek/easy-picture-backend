package com.muzi.easypicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @TableName comments
 */
@TableName(value ="comments")
@Data
public class Comments implements Serializable {
    /**
     *
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long commentId;

    /**
     *
     */
    private Long userId;

    /**
     * 评论目标ID
     */
    private Long targetId;

    /**
     * 评论目标类型：1-图片 2-帖子
     */
    private Integer targetType;

    /**
     * 评论目标所属用户ID
     */
    private Long targetUserId;

    /**
     *
     */
    private String content;

    /**
     *
     */
    private Date createTime;

    /**
     *父类
     */
    private Long parentCommentId;

    /**
     *
     */
    private Integer isDelete;

    /**
     *
     */
    private Long likeCount;

    /**
     *
     */
    private Long dislikeCount;

    /**
     * 是否已读（0-未读，1-已读）
     */
    private Integer isRead;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
