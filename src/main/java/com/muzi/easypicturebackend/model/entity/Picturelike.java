package com.muzi.easypicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 点赞表
 * @TableName pictureLike
 */
@TableName(value ="pictureLike")
@Data
public class Picturelike implements Serializable {
    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 图片 ID
     */
    private Long pictureId;

    /**
     * 用户是否点赞（true 表示点赞，false 表示取消点赞）
     */
    private Integer isLiked;

    /**
     * 
     */
    private Date firstLikeTime;

    /**
     * 最近一次点赞的时间
     */
    private Date lastLikeTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}