package com.muzi.easypicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

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
    @TableId(type = IdType.AUTO)
    private Long commentId;

    /**
     * 
     */
    private Long userId;

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
     * 0表示顶级
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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}