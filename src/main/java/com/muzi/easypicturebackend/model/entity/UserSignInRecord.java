package com.muzi.easypicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户签到记录表
 * @TableName user_sign_in_record
 */
@TableName(value ="user_sign_in_record")
@Data
public class UserSignInRecord implements Serializable {
    /**
     * id
     */
    @TableId
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 年份
     */
    private Integer year;

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

    /**
     * 签到数据位图(366天/8≈46字节)
     */
    private byte[] signInData;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}