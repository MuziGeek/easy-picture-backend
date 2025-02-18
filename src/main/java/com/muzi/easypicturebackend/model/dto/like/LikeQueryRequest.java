package com.muzi.easypicturebackend.model.dto.like;

import com.muzi.easypicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
public class LikeQueryRequest extends PageRequest implements Serializable {
    /**
     * 目标类型：1-图片 2-帖子
     */
    private Integer targetType;


    private static final long serialVersionUID = 1L;
}
