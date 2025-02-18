package com.muzi.easypicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzi.easypicturebackend.model.entity.Message;
import com.muzi.easypicturebackend.model.vo.MessageVO;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author 57242
* @description 针对表【message(留言板表)】的数据库操作Mapper
* @createDate 2025-02-16 20:13:31
* @Entity com.muzi.easypicturebackend.model.entity.Message
*/
public interface MessageMapper extends BaseMapper<Message> {
    @Select("select id,content,createTime from message order by createTime desc limit 500")
    List<MessageVO> getTop500();
}




