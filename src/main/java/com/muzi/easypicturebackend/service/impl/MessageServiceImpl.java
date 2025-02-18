package com.muzi.easypicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzi.easypicturebackend.mapper.MessageMapper;
import com.muzi.easypicturebackend.model.dto.message.AddMessage;
import com.muzi.easypicturebackend.model.entity.Message;
import com.muzi.easypicturebackend.model.vo.MessageVO;
import com.muzi.easypicturebackend.service.MessageService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 57242
* @description 针对表【message(留言板表)】的数据库操作Service实现
* @createDate 2025-02-16 20:13:31
*/
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
    implements MessageService{
    @Override
    public Boolean addMessage(AddMessage addMessage) {
        Message message = new Message();
        message.setContent(addMessage.getContent());
        message.setIp(addMessage.getIp());
        return this.save(message);
    }

    @Override
    public List<MessageVO> getTop500() {
        return this.baseMapper.getTop500();
    }
}





