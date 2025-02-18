package com.muzi.easypicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 帖子附件表
 * @TableName post_attachment
 */
@TableName(value ="post_attachment")
@Data
public class PostAttachment implements Serializable {
    /**
     * 附件ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 类型 1-图片 2-文件
     */
    private Integer type;

    /**
     * 资源URL
     */
    private String url;

    /**
     * 原始文件名
     */
    private String name;

    /**
     * 文件大小(字节)
     */
    private Long size;

    /**
     * 在文章中的位置
     */
    private Integer position;

    /**
     * 在文章中的标识符
     */
    private String marker;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}