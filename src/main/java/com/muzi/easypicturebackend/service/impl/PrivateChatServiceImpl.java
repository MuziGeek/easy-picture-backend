package com.muzi.easypicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzi.easypicturebackend.exception.BusinessException;
import com.muzi.easypicturebackend.exception.ErrorCode;
import com.muzi.easypicturebackend.manager.websocket.ChatWebSocketServer;
import com.muzi.easypicturebackend.mapper.PrivateChatMapper;
import com.muzi.easypicturebackend.model.dto.privatechat.PrivateChatQueryRequest;
import com.muzi.easypicturebackend.model.entity.ChatMessage;
import com.muzi.easypicturebackend.model.entity.PrivateChat;
import com.muzi.easypicturebackend.model.entity.User;
import com.muzi.easypicturebackend.model.entity.Userfollows;
import com.muzi.easypicturebackend.model.vo.UserVO;
import com.muzi.easypicturebackend.service.ChatMessageService;
import com.muzi.easypicturebackend.service.PrivateChatService;
import com.muzi.easypicturebackend.service.UserService;
import com.muzi.easypicturebackend.service.UserfollowsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Set;

@Service
public class PrivateChatServiceImpl extends ServiceImpl<PrivateChatMapper, PrivateChat>
        implements PrivateChatService {

    private static final Logger log = LoggerFactory.getLogger(PrivateChatServiceImpl.class);

    @Lazy
    @Resource
    private UserfollowsService userFollowsService;

    @Resource
    private UserService userService;

    @Resource
    private ChatMessageService chatMessageService;

    @Override
    public Page<PrivateChat> getUserPrivateChats(long userId, long current, long size) {
        QueryWrapper<PrivateChat> queryWrapper = new QueryWrapper<>();
        // 查询当前用户的所有私聊（包括发送和接收的）
        queryWrapper.and(wrap ->
                        wrap.eq("userId", userId)
                                .or()
                                .eq("targetUserId", userId)
                )
                .eq("isDelete", 0)
                .orderByDesc("lastMessageTime");

        Page<PrivateChat> page = this.page(new Page<>(current, size), queryWrapper);

        // 填充目标用户信息并处理未读消息数
        page.getRecords().forEach(chat -> {
            Long targetId;
            // 如果当前用户是接收者，需要交换未读消息数
            if (chat.getTargetUserId().equals(userId)) {
                targetId = chat.getUserId();
                // 交换未读消息数
                Integer temp = chat.getUserUnreadCount();
                chat.setUserUnreadCount(chat.getTargetUserUnreadCount());
                chat.setTargetUserUnreadCount(temp);
                chat.setIsSender(false);  // 设置当前用户是接收者
            } else {
                targetId = chat.getTargetUserId();
                chat.setIsSender(true);   // 设置当前用户是发送者
            }

            User targetUser = userService.getById(targetId);
            if (targetUser != null) {
                UserVO userVO = userService.getUserVO(targetUser);
                chat.setTargetUser(userVO);
            }
        });

        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrivateChat createOrUpdatePrivateChat(long userId, long targetUserId, String content) {
        // 查找现有私聊（检查两个方向）
        QueryWrapper<PrivateChat> queryWrapper = new QueryWrapper<>();
        long finalUserId = userId;
        long finalTargetUserId = targetUserId;
        queryWrapper.and(wrap ->
                wrap.eq("userId", finalUserId).eq("targetUserId", finalTargetUserId)
                        .or()
                        .eq("userId", finalTargetUserId).eq("targetUserId", finalUserId)
        );

        PrivateChat privateChat = this.getOne(queryWrapper);

        if (privateChat == null) {
            // 创建新私聊
            privateChat = new PrivateChat();
            privateChat.setUserId(userId);
            privateChat.setTargetUserId(targetUserId);
            privateChat.setUserUnreadCount(0);
            privateChat.setTargetUserUnreadCount(0);

            // 检查是否互相关注来设置聊天类型
            QueryWrapper<Userfollows> followsQuery = new QueryWrapper<>();
            followsQuery.eq("followerId", userId)
                    .eq("followingId", targetUserId)
                    .eq("followStatus", 1)
                    .eq("isMutual", 1)  // 使用 isMutual 字段，该字段已经表示了双向关注状态
                    .eq("isDelete", 0);

            boolean isFriend = userFollowsService.count(followsQuery) > 0;
            privateChat.setChatType(isFriend ? 1 : 0);
        } else {
            // 如果找到的是反向记录，需要交换用户ID
            if (privateChat.getTargetUserId().equals(userId)) {
                // 交换用户ID
                long temp = userId;
                userId = targetUserId;
                targetUserId = temp;
            }
        }

        // 更新最后消息
        privateChat.setLastMessage(content);
        privateChat.setLastMessageTime(new Date());
        // 增加目标用户的未读消息数
        privateChat.setTargetUserUnreadCount(privateChat.getTargetUserUnreadCount() + 1);

        this.saveOrUpdate(privateChat);

        // 创建聊天消息记录
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSenderId(userId);
        chatMessage.setReceiverId(targetUserId);
        chatMessage.setContent(content);
        chatMessage.setType(1);  // 私聊类型
        chatMessage.setStatus(0);  // 未读状态
        chatMessageService.save(chatMessage);

        return privateChat;
    }


    @Override
    public void incrementUnreadCount(long userId, long targetUserId, boolean isUser) {
        // 查找或创建私聊记录
        QueryWrapper<PrivateChat> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("targetUserId", targetUserId);

        PrivateChat privateChat = this.getOne(queryWrapper);
        if (privateChat == null) {
            // 如果不存在，创建新的私聊记录
            privateChat = new PrivateChat();
            privateChat.setUserId(userId);
            privateChat.setTargetUserId(targetUserId);
            if (isUser) {
                privateChat.setUserUnreadCount(1);
                privateChat.setTargetUserUnreadCount(0);
            } else {
                privateChat.setUserUnreadCount(0);
                privateChat.setTargetUserUnreadCount(1);
            }
            this.save(privateChat);
        } else {
            // 如果存在，增加对应的未读计数
            this.update()
                    .setSql(isUser ?
                            "userUnreadCount = userUnreadCount + 1" :
                            "targetUserUnreadCount = targetUserUnreadCount + 1")
                    .eq("id", privateChat.getId())
                    .update();
        }
    }

    @Override
    public void clearUnreadCount(long userId, long targetUserId, boolean isSender) {
        // 如果当前用户不是发送者，需要交换userId和targetUserId
        if (isSender) {
            long temp = userId;
            userId = targetUserId;
            targetUserId = temp;
        }

        this.update()
                .set("userUnreadCount", 0)  // 清除发送者的未读消息数
                .eq("userId", userId)
                .eq("targetUserId", targetUserId)
                .update();

        // 同时处理可能存在的反向记录
        this.update()
                .set("targetUserUnreadCount", 0)  // 清除接收者的未读消息数
                .eq("userId", targetUserId)
                .eq("targetUserId", userId)
                .update();
    }

    @Override
    public boolean checkIsFriend(long userId, long targetUserId) {
        // 检查是否互相关注
        QueryWrapper<Userfollows> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("followerId", userId)
                .eq("followingId", targetUserId)
                .eq("followStatus", 1)
                .eq("isMutual", 1)
                .eq("isDelete", 0);  // 添加未删除条件

        return userFollowsService.count(queryWrapper) > 0;
    }

    @Override
    public void updateChatType(long userId, long targetUserId, boolean isFriend) {
        // 准备更新的聊天类型
        int chatType = isFriend ? 1 : 0;

        // 同时更新两个方向的记录
        this.update()
                .set("chatType", chatType)
                .and(wrap -> wrap
                        .eq("userId", userId).eq("targetUserId", targetUserId)
                        .or()
                        .eq("userId", targetUserId).eq("targetUserId", userId)
                )
                .update();
    }

    @Override
    public QueryWrapper<PrivateChat> getQueryWrapper(PrivateChatQueryRequest privateChatQueryRequest, User loginUser) {
        QueryWrapper<PrivateChat> queryWrapper = new QueryWrapper<>();
        if (privateChatQueryRequest == null) {
            return queryWrapper;
        }

        // 使用 final 修饰需要在 lambda 中使用的变量
        final Long userId = loginUser.getId();
        final Long targetUserId = privateChatQueryRequest.getTargetUserId();
        final Integer chatType = privateChatQueryRequest.getChatType();
        final String searchText = privateChatQueryRequest.getSearchText();

        // 查询与当前用户相关的聊天记录，并且排除自己和自己的对话
        queryWrapper.and(wrap ->
                wrap.eq("userId", userId).ne("targetUserId", userId)
                        .or()
                        .eq("targetUserId", userId).ne("userId", userId)
        );

        // 如果指定了目标用户，则只查询与该用户的对话
        if (targetUserId != null && targetUserId > 0) {
            queryWrapper.and(wrap ->
                    wrap.eq("targetUserId", targetUserId).eq("userId", userId)
                            .or()
                            .eq("userId", targetUserId).eq("targetUserId", userId)
            );
        }

        // 如果指定了聊天类型（私聊/好友），则按类型筛选
        if (chatType != null) {
            queryWrapper.eq("chatType", chatType);
        }

        // 搜索最后一条消息
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.like("lastMessage", searchText);
        }

        // 未删除
        queryWrapper.eq("isDelete", 0);
        // 按最后消息时间倒序
        queryWrapper.orderByDesc("lastMessageTime");

        return queryWrapper;
    }

    @Override
    public Page<PrivateChat> page(Page<PrivateChat> page, QueryWrapper<PrivateChat> queryWrapper, HttpServletRequest request) {
        Page<PrivateChat> privateChatPage = super.page(page, queryWrapper);
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            log.error("无法获取当前用户信息");
            return privateChatPage;
        }
        final Long currentUserId = loginUser.getId();

        // 填充目标用户信息并处理未读消息数和聊天名称
        if (privateChatPage.getRecords() != null) {
            privateChatPage.getRecords().forEach(chat -> {
                Long targetId;
                String chatName;
                // 如果当前用户是接收者，需要交换未读消息数和使用对应的聊天名称
                if (chat.getTargetUserId().equals(currentUserId)) {
                    targetId = chat.getUserId();
                    // 交换未读消息数
                    Integer temp = chat.getUserUnreadCount();
                    chat.setUserUnreadCount(chat.getTargetUserUnreadCount());
                    chat.setTargetUserUnreadCount(temp);
                    chat.setIsSender(false);  // 设置当前用户是接收者
                    // 使用目标用户的自定义聊天名称
                    chatName = chat.getTargetUserChatName();
                } else {
                    targetId = chat.getTargetUserId();
                    chat.setIsSender(true);   // 设置当前用户是发送者
                    // 使用用户的自定义聊天名称
                    chatName = chat.getUserChatName();
                }

                // 获取目标用户信息
                User targetUser = userService.getById(targetId);
                if (targetUser != null) {
                    UserVO userVO = userService.getUserVO(targetUser);
                    chat.setTargetUser(userVO);
                    // 如果没有自定义聊天名称，使用目标用户的用户名
                    if (StrUtil.isBlank(chatName)) {
                        chatName = userVO.getUserName();
                    }
                    log.info("User info found for targetId: {}", targetId);
                } else {
                    log.warn("User not found for targetId: {}", targetId);
                }

                // 设置显示的聊天名称
                if (chat.getIsSender()) {
                    chat.setUserChatName(chatName);
                } else {
                    chat.setTargetUserChatName(chatName);
                }
            });
        }

        return privateChatPage;
    }

    @Override
    public Page<ChatMessage> getPrivateChatHistory(Long userId, Long targetUserId, Long page, Long size) {
        // 查询消息记录
        QueryWrapper<ChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrap ->
                        wrap.eq("senderId", userId).eq("receiverId", targetUserId)
                                .or()
                                .eq("senderId", targetUserId).eq("receiverId", userId)
                )
                .eq("type", 1)  // 私聊类型
                .eq("isDelete", 0)
                .orderByDesc("createTime");

        Page<ChatMessage> messagePage = chatMessageService.page(new Page<>(page, size), queryWrapper);

        // 填充发送者信息
        messagePage.getRecords().forEach(message -> {
            // 填充发送者信息
            User sender = userService.getById(message.getSenderId());
            sender.setUserPassword(null);
            message.setSender(sender);
        });

        return messagePage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePrivateChatMessage(ChatMessage chatMessage, Long privateChatId, User sender) {
        // 获取私聊记录
        PrivateChat privateChat = this.getById(privateChatId);
        if (privateChat == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "私聊记录不存在");
        }

        // 根据当前用户ID确定接收者ID
        Long receiverId;
        if (privateChat.getUserId().equals(sender.getId())) {
            receiverId = privateChat.getTargetUserId();
            // 获取私聊聊天室在线人数
            Set<WebSocketSession> sessions = ChatWebSocketServer.getPrivateChatSessions(privateChatId);
            boolean bothOnline = sessions != null && sessions.size() == 2;
            // 只有在双方不都在线时才增加未读消息数
            if (!bothOnline) {
                privateChat.setTargetUserUnreadCount(privateChat.getTargetUserUnreadCount() + 1);
            }
        } else if (privateChat.getTargetUserId().equals(sender.getId())) {
            receiverId = privateChat.getUserId();
            // 获取私聊聊天室在线人数
            Set<WebSocketSession> sessions = ChatWebSocketServer.getPrivateChatSessions(privateChatId);
            boolean bothOnline = sessions != null && sessions.size() == 2;
            // 只有在双方不都在线时才增加未读消息数
            if (!bothOnline) {
                privateChat.setUserUnreadCount(privateChat.getUserUnreadCount() + 1);
            }
        } else {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该私聊的参与者");
        }
        chatMessage.setReceiverId(receiverId);
        chatMessage.setSenderId(sender.getId());

        // 更新私聊记录的最后一句内容
        privateChat.setLastMessage(chatMessage.getContent());
        privateChat.setLastMessageTime(new Date());

        // 保存更新
        this.updateById(privateChat);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePrivateChat(Long privateChatId, User loginUser) {
        // 获取私聊记录
        PrivateChat privateChat = this.getById(privateChatId);
        if (privateChat == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "私聊记录不存在");
        }

        // 校验权限，只有私聊参与者才能删除
        if (!privateChat.getUserId().equals(loginUser.getId())
                && !privateChat.getTargetUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该私聊的参与者");
        }

        // 1. 删除私聊记录
        boolean success = this.removeById(privateChatId);

        if (success) {
            // 2. 删除相关的聊天消息
            QueryWrapper<ChatMessage> messageQueryWrapper = new QueryWrapper<>();
            messageQueryWrapper.eq("privateChatId", privateChatId)
                    .eq("type", 1);  // 私聊类型
            chatMessageService.remove(messageQueryWrapper);
        }

        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChatName(Long privateChatId, String chatName, User loginUser) {
        // 获取私聊记录
        PrivateChat privateChat = this.getById(privateChatId);
        if (privateChat == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "私聊记录不存在");
        }

        // 根据当前用户身份更新对应的聊天名称
        if (privateChat.getUserId().equals(loginUser.getId())) {
            privateChat.setUserChatName(chatName);
        } else if (privateChat.getTargetUserId().equals(loginUser.getId())) {
            privateChat.setTargetUserChatName(chatName);
        } else {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该私聊的参与者");
        }

        this.updateById(privateChat);
    }
}
