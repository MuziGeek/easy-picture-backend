package com.muzi.easypicturebackend.mapper;

import com.muzi.easypicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author 57242
* @description 针对表【picture(图片)】的数据库操作Mapper
* @createDate 2024-12-13 23:14:50
* @Entity com.muzi.easypicturebackend.model.entity.Picture
*/
public interface PictureMapper extends BaseMapper<Picture> {

    List<Picture> getTop100PictureByYear();

    List<Picture> getTop100PictureByMonth();

    List<Picture> getTop100PictureByWeek();
}




