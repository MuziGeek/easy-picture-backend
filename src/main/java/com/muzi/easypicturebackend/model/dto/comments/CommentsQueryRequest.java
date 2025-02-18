package com.muzi.easypicturebackend.model.dto.comments;


import com.muzi.easypicturebackend.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * 评论查询请求
 */
@Data
public class CommentsQueryRequest extends PageRequest implements Serializable {


    /**
     * 评论目标ID
     */
    private Long targetId;

    /**
     * 评论目标类型：1-图片 2-帖子，默认为1(图片)
     */
    private Integer targetType ;

    private static final long serialVersionUID = 1L;
}
