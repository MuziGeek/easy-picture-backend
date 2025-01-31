package com.muzi.easypicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.muzi.easypicturebackend.model.entity.Category;
import com.muzi.easypicturebackend.model.vo.CategoryVO;

import java.util.List;

/**
* @author 57242
* @description 针对表【Category(分类)】的数据库操作Service
* @createDate 2025-01-31 12:18:52
*/
public interface CategoryService extends IService<Category> {
    List<String> listCategory();

    List<CategoryVO> listCategoryVO(List<Category> records);

    CategoryVO getCategoryVO(Category category);

    List<CategoryVO> findCategory(String categoryName);
}
