package com.muzi.easypicturebackend.model.dto.post;

import com.muzi.easypicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
public class PostQueryRequest extends PageRequest implements Serializable {
    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 分类
     */
    private String category;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 审核状态（0-待审核, 1-已发布, 2-拒绝）
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}
