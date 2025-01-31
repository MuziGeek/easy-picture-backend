package com.muzi.easypicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.muzi.easypicturebackend.model.entity.Tag;
import com.muzi.easypicturebackend.model.vo.TagVO;

import java.util.List;

/**
* @author 57242
* @description 针对表【Tag(标签)】的数据库操作Service
* @createDate 2025-01-31 12:19:51
*/
public interface TagService extends IService<Tag> {
    List<String> listTag();

    TagVO getTagVO(Tag tag);

    List<TagVO> listTagVOByPage(List<Tag> records);

    Boolean addTag(String tagName);

    Boolean deleteTag(Long id);

    List<TagVO> searchTag(String tagName);
}
