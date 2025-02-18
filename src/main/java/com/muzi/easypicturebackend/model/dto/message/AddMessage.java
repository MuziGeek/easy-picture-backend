package com.muzi.easypicturebackend.model.dto.message;

import lombok.Data;

import java.io.Serializable;

/**
 * 留言板表
 * @TableName message
 */
@Data
public class AddMessage implements Serializable {

    /**
     * 留言内容
     */
    private String content;

    /**
     * IP地址
     */
    private String ip;

    private static final long serialVersionUID = 1L;
}
