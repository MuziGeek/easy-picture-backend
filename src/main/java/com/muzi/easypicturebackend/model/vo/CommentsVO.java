package com.muzi.easypicturebackend.model.vo;

import com.muzi.easypicturebackend.common.PageRequest;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 *
 * @TableName comments
 */
@Data
public class CommentsVO extends PageRequest implements Serializable {
    /**
     *
     */
    private Long commentId;

    /**
     *
     */
    private CommentUserVO commentUser;

    /**
     *
     */
    private Long pictureId;

    /**
     *
     */
    private String content;

    /**
     *
     */
    private Date createTime;

    /**
     *
     */
    private Long parentCommentId;

    /**
     *
     */
    private Long likeCount;

    /**
     *
     */
    private Long dislikeCount;

    /**
     * 子评论列表
     */
    private List<CommentsVO> children;
}
