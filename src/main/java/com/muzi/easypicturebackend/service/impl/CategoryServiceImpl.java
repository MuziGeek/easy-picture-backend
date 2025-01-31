package com.muzi.easypicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzi.easypicturebackend.mapper.CategoryMapper;
import com.muzi.easypicturebackend.model.entity.Category;
import com.muzi.easypicturebackend.model.vo.CategoryVO;
import com.muzi.easypicturebackend.service.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
* @author 57242
* @description 针对表【Category(分类)】的数据库操作Service实现
* @createDate 2025-01-31 12:18:52
*/
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category>
    implements CategoryService{


    @Override
    public List<String> listCategory() {
        //从数据库查询分类
        QueryWrapper<Category> queryWrapper2 = new QueryWrapper<>();
        // 指定只查询 name 字段
        queryWrapper2.select("categoryName");
        // 执行查询并将结果转换为 List<String>
        return this.getBaseMapper().selectObjs(queryWrapper2)
                .stream()
                .map(obj -> obj != null ? obj.toString() : null)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> listCategoryVO(List<Category> records) {
        if(CollUtil.isEmpty(records)){
            return null;
        }
        return records.stream().map(this::getCategoryVO).collect(Collectors.toList());
    }

    @Override
    public CategoryVO getCategoryVO(Category category) {
        if(category == null){
            return null;
        }
        CategoryVO categoryVO = new CategoryVO();
        BeanUtils.copyProperties(category, categoryVO);
        return categoryVO;
    }

    @Override
    public List<CategoryVO> findCategory(String categoryName) {
        //查询条件改造
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("categoryName", categoryName);
        List<Category> categoryList = this.baseMapper.selectList(queryWrapper);
        return listCategoryVO(categoryList);
    }
}




