package com.muzi.easypicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzi.easypicturebackend.model.entity.ChatMessage;
import com.muzi.easypicturebackend.model.entity.User;

import java.util.List;

/**
* @author 57242
* @description 针对表【chat_message(聊天消息表)】的数据库操作Service
* @createDate 2025-02-16 20:11:36
*/
public interface ChatMessageService extends IService<ChatMessage> {
    /**
     * 获取与指定用户的聊天记录
     */
    Page<ChatMessage> getUserChatHistory(long userId, long otherUserId, long current, long size);

    /**
     * 获取指定图片的聊天记录
     */
    Page<ChatMessage> getPictureChatHistory(long pictureId, long current, long size);

    /**
     * 将消息标记为已读
     */
    void markAsRead(long receiverId, long senderId);

    /**
     * 获取消息的回复列表
     */
    List<ChatMessage> getMessageReplies(long messageId);

    /**
     * 获取消息的完整会话
     */
    List<ChatMessage> getMessageThread(long messageId);

    /**
     * 发送回复消息
     */
    ChatMessage reply(ChatMessage message, long replyToMessageId);

    /**
     * 获取指定空间的聊天记录
     */
    Page<ChatMessage> getSpaceChatHistory(long spaceId, long current, long size);

    /**
     * 检查用户是否有权限在指定空间发送消息
     */
    boolean canUserChatInSpace(long userId, long spaceId);

    /**
     * 获取空间的所有成员
     */
    List<User> getSpaceMembers(long spaceId);

    /**
     * 获取私聊历史消息
     * @param privateChatId 私聊ID
     * @param current 当前页
     * @param size 每页大小
     * @return 消息分页数据
     */
    Page<ChatMessage> getPrivateChatHistory(long privateChatId, long current, long size);

    void fillMessageInfo(ChatMessage message);
}
