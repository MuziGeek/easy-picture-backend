package com.muzi.easypicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.muzi.easypicturebackend.model.dto.message.AddMessage;
import com.muzi.easypicturebackend.model.entity.Message;
import com.muzi.easypicturebackend.model.vo.MessageVO;

import java.util.List;

/**
* @author 57242
* @description 针对表【message(留言板表)】的数据库操作Service
* @createDate 2025-02-16 20:13:31
*/
public interface MessageService extends IService<Message> {

    Boolean addMessage(AddMessage addMessage);

    List<MessageVO> getTop500();
}

