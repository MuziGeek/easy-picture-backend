package com.muzi.easypicturebackend.manager.websocket;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muzi.easypicturebackend.exception.BusinessException;
import com.muzi.easypicturebackend.manager.websocket.disruptor.ChatEventProducer;
import com.muzi.easypicturebackend.model.entity.ChatMessage;
import com.muzi.easypicturebackend.model.entity.User;
import com.muzi.easypicturebackend.service.ChatMessageService;
import com.muzi.easypicturebackend.service.PrivateChatService;
import com.muzi.easypicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatWebSocketServer extends TextWebSocketHandler {

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private ObjectMapper webSocketObjectMapper;

    @Resource
    private UserService userService;

    @Resource
    private PrivateChatService privateChatService;

    @Resource
    @Lazy
    private ChatEventProducer chatEventProducer;

    /**
     * 用户ID -> WebSocket session
     */
    private static final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    /**
     * 图片ID -> Set<WebSocketSession>
     */
    private static final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * 空间ID -> Set<WebSocketSession>
     */
    private static final Map<Long, Set<WebSocketSession>> spaceSessions = new ConcurrentHashMap<>();

    /**
     * 私聊ID -> Set<WebSocketSession>
     */
    private static final Map<Long, Set<WebSocketSession>> privateChatSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            User user = (User) session.getAttributes().get("user");
            Long pictureId = (Long) session.getAttributes().get("pictureId");
            Long spaceId = (Long) session.getAttributes().get("spaceId");
            Long privateChatId = (Long) session.getAttributes().get("privateChatId");

            if (user == null) {
                log.error("用户未登录");
                session.close();
                return;
            }

            // 保存用户session
            userSessions.put(user.getId(), session);

            // 如果是私聊
            if (privateChatId != null) {
                log.info("用户 {} 加入私聊 {}", user.getId(), privateChatId);
                privateChatSessions.computeIfAbsent(privateChatId, k -> ConcurrentHashMap.newKeySet()).add(session);
                sendUserChatHistory(session, privateChatId);
                broadcastOnlineUsers(null, null, privateChatId);
            }
            // 如果是图片聊天室
            else if (pictureId != null) {
                pictureSessions.computeIfAbsent(pictureId, k -> ConcurrentHashMap.newKeySet()).add(session);
                sendPictureChatHistory(session, pictureId);
                broadcastOnlineUsers(pictureId, null, null);  // 修改这里
            }
            // 如果是空间聊天
            else if (spaceId != null) {
                // 检查用户是否有权限加入空间聊天
                if (!chatMessageService.canUserChatInSpace(user.getId(), spaceId)) {
                    log.error("用户不是空间成员");
                    session.close();
                    return;
                }
                spaceSessions.computeIfAbsent(spaceId, k -> ConcurrentHashMap.newKeySet()).add(session);
                sendSpaceChatHistory(session, spaceId);
                broadcastOnlineUsers(null, spaceId, null);  // 修改这里
            }
        } catch (Exception e) {
            log.error("WebSocket连接建立失败", e);
            try {
                session.close();
            } catch (IOException ex) {
                log.error("关闭WebSocket连接失败", ex);
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        try {
            // 使用 ObjectMapper 解析消息
            Map<String, Object> messageMap = webSocketObjectMapper.readValue(message.getPayload(), Map.class);

            // 安全地获取消息类型
            Object typeObj = messageMap.get("type");
            String type = typeObj != null ? typeObj.toString() : null;

            if ("loadMore".equals(type)) {
                handleLoadMoreMessage(session, messageMap);
                return;
            }

            if ("getOnlineUsers".equals(type)) {
                Long pictureId = (Long) session.getAttributes().get("pictureId");
                Long spaceId = (Long) session.getAttributes().get("spaceId");
                Long privateChatId = (Long) session.getAttributes().get("privateChatId");
                broadcastOnlineUsers(pictureId, spaceId, privateChatId);
                return;
            }

            // 处理普通消息
            ChatMessage chatMessage = new ChatMessage();
            User user = (User) session.getAttributes().get("user");

            // 检查必要字段
            String content = (String) messageMap.get("content");
            if (content == null) {
                sendErrorMessage(session, "消息内容不能为空");
                return;
            }
            chatMessage.setContent(content);

            // 确定消息类型和目标ID
            Integer messageType;
            Long targetId;

            if (session.getAttributes().get("privateChatId") != null) {
                messageType = 1;  // 私聊
                targetId = (Long) session.getAttributes().get("privateChatId");
                chatMessage.setPrivateChatId(targetId);
            } else if (session.getAttributes().get("pictureId") != null) {
                messageType = 2;  // 图片聊天室
                targetId = (Long) session.getAttributes().get("pictureId");
                chatMessage.setPictureId(targetId);
            } else if (session.getAttributes().get("spaceId") != null) {
                messageType = 3;  // 空间聊天
                targetId = (Long) session.getAttributes().get("spaceId");
                chatMessage.setSpaceId(targetId);
            } else {
                log.error("无法确定消息类型");
                sendErrorMessage(session, "无法确定消息类型");
                return;
            }
            chatMessage.setType(messageType);

            // 处理回复消息
            Object replyIdObj = messageMap.get("replyId");
            if (replyIdObj != null) {
                Long replyId = Long.valueOf(replyIdObj.toString());
                chatMessage.setReplyId(replyId);
                Object rootIdObj = messageMap.get("rootId");
                chatMessage.setRootId(rootIdObj != null ? Long.valueOf(rootIdObj.toString()) : replyId);
            }

            chatMessage.setSenderId(user.getId());
            chatMessage.setCreateTime(new Date());

            // 使用 Disruptor 异步处理消息
            chatEventProducer.publishEvent(chatMessage, session, user, targetId, messageType);

        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
            sendErrorMessage(session, "消息处理失败");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        Long spaceId = (Long) session.getAttributes().get("spaceId");
        Long privateChatId = (Long) session.getAttributes().get("privateChatId");

        // 移除用户session
        if (user != null) {
            userSessions.remove(user.getId());
        }

        // 如果是私聊，移除私聊session
        if (privateChatId != null) {
            Set<WebSocketSession> sessions = privateChatSessions.get(privateChatId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    privateChatSessions.remove(privateChatId);
                } else {
                    broadcastOnlineUsers(null, null, privateChatId);
                }
            }
        }
        // 如果是图片聊天室，移除图片session
        else if (pictureId != null) {
            Set<WebSocketSession> sessions = pictureSessions.get(pictureId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    pictureSessions.remove(pictureId);
                } else {
                    broadcastOnlineUsers(pictureId, null, null);
                }
            }
        }
        // 如果是空间聊天，移除空间session
        else if (spaceId != null) {
            Set<WebSocketSession> sessions = spaceSessions.get(spaceId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    spaceSessions.remove(spaceId);
                } else {
                    broadcastOnlineUsers(null, spaceId, null);
                }
            }
        }
    }

    /**
     * 发送私聊消息
     */
    private void sendToPrivateChat(ChatMessage message) throws IOException {
        if (message.getPrivateChatId() == null) {
            log.error("privateChatId为空，无法发送消息");
            return;
        }

        Set<WebSocketSession> sessions = privateChatSessions.get(message.getPrivateChatId());
        if (sessions != null) {
            String messageStr = webSocketObjectMapper.writeValueAsString(message);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(messageStr));
                }
            }
        }
    }

    /**
     * 发送图片聊天室消息
     */
    private void sendToPictureRoom(ChatMessage chatMessage) throws IOException {
        if (chatMessage.getPictureId() == null) {
            log.error("pictureId为空，无法发送消息");
            return;
        }

        Set<WebSocketSession> sessions = pictureSessions.get(chatMessage.getPictureId());
        if (sessions != null && !sessions.isEmpty()) {
            String messageStr = webSocketObjectMapper.writeValueAsString(chatMessage);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(messageStr));
                }
            }
        }
    }

    /**
     * 发送空间聊天消息
     */
    private void sendToSpaceRoom(ChatMessage chatMessage) throws IOException {
        Set<WebSocketSession> sessions = spaceSessions.get(chatMessage.getSpaceId());
        if (sessions != null) {
            String messageStr = webSocketObjectMapper.writeValueAsString(chatMessage);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(messageStr));
                }
            }
        }
    }

    /**
     * 发送图片聊天室的历史消息
     */
    private void sendPictureChatHistory(WebSocketSession session, Long pictureId) {
        try {
            // 获取最近的20条消息，消息中会包含发送者信息
            Page<ChatMessage> history = chatMessageService.getPictureChatHistory(pictureId, 1, 20);
            Map<String, Object> response = new HashMap<>();
            response.put("type", "history");
            response.put("messages", history.getRecords());
            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("发送历史消息失败", e);
        }
    }

    /**
     * 发送空间聊天历史记录
     */
    private void sendSpaceChatHistory(WebSocketSession session, Long spaceId) {
        try {
            Page<ChatMessage> history = chatMessageService.getSpaceChatHistory(spaceId, 1, 20);
            Map<String, Object> response = new HashMap<>();
            response.put("type", "history");
            response.put("messages", history.getRecords());
            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("发送空间历史消息失败", e);
        }
    }

    /**
     * 发送私聊历史消息
     */
    private void sendUserChatHistory(WebSocketSession session, Long privateChatId) {
        try {
            // 获取最近的20条消息
            Page<ChatMessage> history = chatMessageService.getPrivateChatHistory(privateChatId, 1L, 20L);

            Map<String, Object> response = new HashMap<>();
            response.put("type", "history");
            response.put("messages", history.getRecords());
            response.put("hasMore", history.getPages() > 1);

            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("发送私聊历史消息失败", e);
        }
    }

    private void sendMorePictureHistory(WebSocketSession session, Long pictureId, Long page) {
        try {
            Page<ChatMessage> history = chatMessageService.getPictureChatHistory(pictureId, page, 20);
            Map<String, Object> response = new HashMap<>();
            response.put("type", "moreHistory");
            response.put("messages", history.getRecords());
            response.put("hasMore", history.getPages() > page);
            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("发送更多历史消息失败", e);
        }
    }

    private void sendMoreUserHistory(WebSocketSession session, Long privateChatId, Long page) {
        try {
            Page<ChatMessage> history = chatMessageService.getPrivateChatHistory(privateChatId, page, 20L);
            Map<String, Object> response = new HashMap<>();
            response.put("type", "moreHistory");
            response.put("messages", history.getRecords());
            response.put("hasMore", history.getPages() > page);
            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("发送更多私聊历史消息失败", e);
        }
    }

    private void handleLoadMoreMessage(WebSocketSession session, Map<String, Object> messageMap) throws IOException {
        // 安全地处理数值类型转换
        Object pictureIdObj = messageMap.get("pictureId");
        Object spaceIdObj = messageMap.get("spaceId");
        Object pageObj = messageMap.get("page");

        Long pictureId = null;
        Long spaceId = null;
        Long page = null;

        // 处理页码
        if (pageObj instanceof Integer) {
            page = Long.valueOf((Integer) pageObj);
        } else if (pageObj instanceof Long) {
            page = (Long) pageObj;
        } else if (pageObj instanceof String) {
            page = Long.parseLong((String) pageObj);
        }
        page = page != null ? page : 1L;

        // 处理图片ID
        if (pictureIdObj instanceof Integer) {
            pictureId = Long.valueOf((Integer) pictureIdObj);
        } else if (pictureIdObj instanceof Long) {
            pictureId = (Long) pictureIdObj;
        } else if (pictureIdObj instanceof String) {
            pictureId = Long.parseLong((String) pictureIdObj);
        }

        // 处理空间ID
        if (spaceIdObj instanceof Integer) {
            spaceId = Long.valueOf((Integer) spaceIdObj);
        } else if (spaceIdObj instanceof Long) {
            spaceId = (Long) spaceIdObj;
        } else if (spaceIdObj instanceof String) {
            spaceId = Long.parseLong((String) spaceIdObj);
        }

        if (pictureId != null) {
            sendMorePictureHistory(session, pictureId, page);
        } else if (spaceId != null) {
            sendMoreSpaceHistory(session, spaceId, page);
        } else {
            Object privateChatIdObj = messageMap.get("privateChatId");
            Long privateChatId = null;

            if (privateChatIdObj instanceof Integer) {
                privateChatId = Long.valueOf((Integer) privateChatIdObj);
            } else if (privateChatIdObj instanceof Long) {
                privateChatId = (Long) privateChatIdObj;
            } else if (privateChatIdObj instanceof String) {
                privateChatId = Long.parseLong((String) privateChatIdObj);
            }

            if (privateChatId != null) {
                sendMoreUserHistory(session, privateChatId, page);
            }
        }
    }

    /**
     * 发送更多空间聊天历史记录
     */
    private void sendMoreSpaceHistory(WebSocketSession session, Long spaceId, Long page) {
        try {
            Page<ChatMessage> history = chatMessageService.getSpaceChatHistory(spaceId, page, 20);
            Map<String, Object> response = new HashMap<>();
            response.put("type", "moreHistory");
            response.put("messages", history.getRecords());
            response.put("hasMore", history.getPages() > page);
            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("发送更多空间历史消息失败", e);
        }
    }

    private void sendErrorMessage(WebSocketSession session, String message) throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("type", "error");
        errorResponse.put("message", message);
        session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(errorResponse)));
    }

    /**
     * 广播在线用户信息
     */
    private void broadcastOnlineUsers(Long pictureId, Long spaceId, Long privateChatId) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "onlineUsers");

            Set<WebSocketSession> targetSessions;
            if (pictureId != null) {
                targetSessions = pictureSessions.get(pictureId);
                // 获取该图片聊天室的在线用户
                Set<User> onlineUsers = new HashSet<>();
                if (targetSessions != null) {
                    for (WebSocketSession session : targetSessions) {
                        User user = (User) session.getAttributes().get("user");
                        if (user != null) {
                            User safeUser = new User();
                            safeUser.setId(user.getId());
                            safeUser.setUserAccount(user.getUserAccount());
                            safeUser.setUserName(user.getUserName());
                            safeUser.setUserAvatar(user.getUserAvatar());
                            safeUser.setUserProfile(user.getUserProfile());
                            safeUser.setUserRole(user.getUserRole());
                            safeUser.setCreateTime(user.getCreateTime());
                            onlineUsers.add(safeUser);
                        }
                    }
                }
                response.put("onlineCount", onlineUsers.size());
                response.put("onlineUsers", onlineUsers);
                response.put("pictureId", pictureId);
            } else if (spaceId != null) {
                targetSessions = spaceSessions.get(spaceId);
                // 获取该空间的在线用户
                Set<User> onlineUsers = new HashSet<>();
                if (targetSessions != null) {
                    for (WebSocketSession session : targetSessions) {
                        User user = (User) session.getAttributes().get("user");
                        if (user != null) {
                            User safeUser = new User();
                            safeUser.setId(user.getId());
                            safeUser.setUserAccount(user.getUserAccount());
                            safeUser.setUserName(user.getUserName());
                            safeUser.setUserAvatar(user.getUserAvatar());
                            safeUser.setUserProfile(user.getUserProfile());
                            safeUser.setUserRole(user.getUserRole());
                            safeUser.setCreateTime(user.getCreateTime());
                            onlineUsers.add(safeUser);
                        }
                    }
                }

                // 获取空间所有成员
                List<User> allMembers = chatMessageService.getSpaceMembers(spaceId);

                // 计算离线用户
                Set<User> offlineUsers = new HashSet<>();
                for (User member : allMembers) {
                    boolean isOnline = onlineUsers.stream()
                            .anyMatch(onlineUser -> onlineUser.getId().equals(member.getId()));
                    if (!isOnline) {
                        offlineUsers.add(member);
                    }
                }

                response.put("onlineCount", onlineUsers.size());
                response.put("offlineCount", offlineUsers.size());
                response.put("totalCount", allMembers.size());
                response.put("onlineUsers", onlineUsers);
                response.put("offlineUsers", offlineUsers);
                response.put("spaceId", spaceId);
            } else if (privateChatId != null) {
                targetSessions = privateChatSessions.get(privateChatId);
                // 获取该私聊的在线用户
                Set<User> onlineUsers = new HashSet<>();
                if (targetSessions != null) {
                    for (WebSocketSession session : targetSessions) {
                        User user = (User) session.getAttributes().get("user");
                        if (user != null) {
                            User safeUser = new User();
                            safeUser.setId(user.getId());
                            safeUser.setUserAccount(user.getUserAccount());
                            safeUser.setUserName(user.getUserName());
                            safeUser.setUserAvatar(user.getUserAvatar());
                            safeUser.setUserProfile(user.getUserProfile());
                            safeUser.setUserRole(user.getUserRole());
                            safeUser.setCreateTime(user.getCreateTime());
                            onlineUsers.add(safeUser);
                        }
                    }
                }
                response.put("onlineCount", onlineUsers.size());
                response.put("onlineUsers", onlineUsers);
                response.put("privateChatId", privateChatId);
            } else {
                return;
            }

            if (targetSessions != null) {
                String messageStr = webSocketObjectMapper.writeValueAsString(response);
                for (WebSocketSession session : targetSessions) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(messageStr));
                    }
                }
            }
        } catch (IOException e) {
            log.error("广播在线用户信息失败", e);
        }
    }

    /**
     * 获取私聊聊天室的会话集合
     */
    public static Set<WebSocketSession> getPrivateChatSessions(Long privateChatId) {
        if (privateChatId == null) {
            return null;
        }
        return privateChatSessions.get(privateChatId);
    }

    /**
     * 发送聊天历史记录
     */
    private void sendChatHistory(WebSocketSession session, List<ChatMessage> history, boolean hasMore) {
        try {
            // 填充所有消息的信息（包括回复消息）
            history.forEach(msg -> chatMessageService.fillMessageInfo(msg));

            Map<String, Object> response = new HashMap<>();
            response.put("type", "history");
            response.put("messages", history);
            response.put("hasMore", hasMore);
            session.sendMessage(new TextMessage(webSocketObjectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("发送历史消息失败", e);
        }
    }

    /**
     * 处理图片聊天室消息
     */
    public void handlePictureChatMessage(ChatMessage chatMessage, WebSocketSession session) throws IOException {
        try {
            if (chatMessage.getPictureId() == null) {
                log.error("图片ID为空，无法处理消息");
                sendErrorMessage(session, "消息处理失败");
                return;
            }

            // 保存消息
            chatMessageService.save(chatMessage);

            // 填充消息信息
            chatMessageService.fillMessageInfo(chatMessage);

            // 填充发送者信息
            User sender = userService.getById(chatMessage.getSenderId());
            chatMessage.setSender(sender);

            // 发送消息
            sendToPictureRoom(chatMessage);
        } catch (Exception e) {
            log.error("处理图片聊天室消息失败", e);
            sendErrorMessage(session, "消息发送失败");
        }
    }

    /**
     * 处理空间聊天消息
     */
    public void handleSpaceChatMessage(ChatMessage chatMessage, WebSocketSession session) throws IOException {
        try {
            // 保存消息
            chatMessageService.save(chatMessage);

            // 填充消息信息
            chatMessageService.fillMessageInfo(chatMessage);

            // 发送消息
            sendToSpaceRoom(chatMessage);
        } catch (Exception e) {
            log.error("处理空间聊天消息失败", e);
            sendErrorMessage(session, "消息发送失败");
        }
    }

    /**
     * 处理私聊消息
     */
    public void handlePrivateChatMessage(ChatMessage chatMessage, WebSocketSession session) throws IOException {
        try {
            User user = (User) session.getAttributes().get("user");

            // 先保存消息
            chatMessageService.save(chatMessage);

            // 使用原有的私聊处理逻辑（更新私聊记录）
            privateChatService.handlePrivateChatMessage(chatMessage, chatMessage.getPrivateChatId(), user);

            // 填充消息信息
            chatMessageService.fillMessageInfo(chatMessage);

            // 填充发送者信息
            chatMessage.setSender(user);

            // 发送消息
            sendToPrivateChat(chatMessage);
        } catch (BusinessException e) {
            log.error("处理私聊消息失败: {}", e.getMessage());
            sendErrorMessage(session, e.getMessage());
        } catch (Exception e) {
            log.error("处理私聊消息失败", e);
            sendErrorMessage(session, "消息发送失败");
        }
    }
}
