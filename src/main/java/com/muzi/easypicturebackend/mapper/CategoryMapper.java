package com.muzi.easypicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzi.easypicturebackend.model.entity.Category;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author 57242
* @description 针对表【Category(分类)】的数据库操作Mapper
* @createDate 2025-01-31 12:18:52
* @Entity com.muzi.easypicturebackend.model.entity.Category
*/
public interface CategoryMapper extends BaseMapper<Category> {
    @Select("select categoryName from category where isDelete = 0 and type = #{type}")
    List<String> listCategoryByType(@Param("type") Integer type);
}




