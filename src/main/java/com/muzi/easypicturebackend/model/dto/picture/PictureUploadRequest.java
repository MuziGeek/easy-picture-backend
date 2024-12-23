package com.muzi.easypicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;


@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片 id（用于修改）
     */
    private Long id;
    /**
     * 空间 ID
     */
    private Long spaceId;
    /**
     * 文件地址
     */
    private String fileUrl;
    /**
     * 图片名称
     */
    private String picName;
    /**
     * 标签
     */
    private List<String> tags;
    /**
     * 分类
     */
    private String category;

    /**
     * 原始url
     */
    private String originUrl;

    private static final long serialVersionUID = 1L;
}

