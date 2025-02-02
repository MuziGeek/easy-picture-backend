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
     * 图片ID
     */
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}
