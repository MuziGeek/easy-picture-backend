package com.muzi.easypicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzi.easypicturebackend.model.dto.privatechat.PrivateChatQueryRequest;
import com.muzi.easypicturebackend.model.entity.ChatMessage;
import com.muzi.easypicturebackend.model.entity.PrivateChat;
import com.muzi.easypicturebackend.model.entity.User;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;

public interface PrivateChatService extends IService<PrivateChat> {

    /**
     * 获取用户的私聊列表
     */
    Page<PrivateChat> getUserPrivateChats(long userId, long current, long size);

    /**
     * 创建或更新私聊
     */
    PrivateChat createOrUpdatePrivateChat(long userId, long targetUserId, String lastMessage);

    /**
     * 检查是否是好友关系（双向关注）
     */
    boolean checkIsFriend(long userId, long targetUserId);

    /**
     * 更新聊天类型
     */
    void updateChatType(long userId, long targetUserId, boolean isFriend);

    /**
     * 获取查询条件
     */
    QueryWrapper<PrivateChat> getQueryWrapper(PrivateChatQueryRequest privateChatQueryRequest, User loginUser);

    /**
     * 分页查询
     */
    Page<PrivateChat> page(Page<PrivateChat> page, QueryWrapper<PrivateChat> queryWrapper, HttpServletRequest request);

    /**
     * 获取私聊历史消息
     */
    Page<ChatMessage> getPrivateChatHistory(Long userId, Long targetUserId, Long page, Long size);

    /**
     * 增加用户的未读消息数
     * @param userId 用户ID
     * @param targetUserId 目标用户ID
     * @param isUser 是否增加用户的未读消息数（true增加用户的，false增加目标用户的）
     */
    void incrementUnreadCount(long userId, long targetUserId, boolean isUser);

    /**
     * 清除用户的未读消息数
     * @param userId 用户ID
     * @param targetUserId 目标用户ID
     * @param isUser 是否清除用户的未读消息数（true清除用户的，false清除目标用户的）
     */
    void clearUnreadCount(long userId, long targetUserId, boolean isUser);

    @Transactional(rollbackFor = Exception.class)
    void handlePrivateChatMessage(ChatMessage chatMessage, Long privateChatId, User sender);

    boolean deletePrivateChat(Long privateChatId, User loginUser);

    void updateChatName(Long privateChatId, String chatName, User loginUser);
}
