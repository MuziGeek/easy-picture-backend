package com.muzi.easypicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天消息表
 * @TableName chat_message
 */
@TableName(value ="chat_message")
@Data
public class ChatMessage implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发送者id
     */
    private Long senderId;

    /**
     * 接收者id
     */
    private Long receiverId;

    /**
     * 图片id
     */
    private Long pictureId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型 1-私聊 2-图片聊天室
     */
    private Integer type;

    /**
     * 状态 0-未读 1-已读
     */
    private Integer status;

    /**
     * 回复的消息id
     */
    private Long replyId;

    /**
     * 会话根消息id
     */
    private Long rootId;

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
     * 空间id
     */
    private Long spaceId;

    /**
     * 私聊ID
     */
    private Long privateChatId;

    /**
     * 回复的消息内容（非数据库字段）
     */
    @TableField(exist = false)
    private ChatMessage replyMessage;

    /**
     * 发送者信息（非数据库字段）
     */
    @TableField(exist = false)
    private User sender;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}