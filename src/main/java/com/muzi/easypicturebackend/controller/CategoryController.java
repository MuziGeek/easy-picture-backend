package com.muzi.easypicturebackend.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.muzi.easypicturebackend.annotation.AuthCheck;
import com.muzi.easypicturebackend.common.BaseResponse;
import com.muzi.easypicturebackend.common.PageRequest;
import com.muzi.easypicturebackend.common.ResultUtils;
import com.muzi.easypicturebackend.constant.UserConstant;
import com.muzi.easypicturebackend.model.entity.Category;
import com.muzi.easypicturebackend.model.vo.CategoryVO;
import com.muzi.easypicturebackend.service.CategoryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {
    @Resource
    private CategoryService categoryService;

    /**
     * 获取所有分类
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<CategoryVO>> listCategoryVO(PageRequest pageRequest) {
        long current = pageRequest.getCurrent();
        long pageSize = pageRequest.getPageSize();
        Page<Category> categorypage = categoryService.page(new Page<>(current, pageSize));
        Page<CategoryVO> categoryVOPage = new Page<>(current, pageSize, categorypage.getTotal());
        List<CategoryVO> categoryVOList = categoryService.listCategoryVO(categorypage.getRecords());
        categoryVOPage.setRecords(categoryVOList);
        return ResultUtils.success(categoryVOPage);
    }

    /**
     * 添加分类
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> addCategory(String categoryName) {
        Category category = new Category();
        category.setCategoryName(categoryName);
        return ResultUtils.success(categoryService.save(category));
    }

    /**
     * 删除分类
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteCategory(Long categoryId) {
        return ResultUtils.success(categoryService.removeById(categoryId));
    }

    /**
     * 查找分类
     */
    @PostMapping("/search")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<CategoryVO>> findCategory(String categoryName) {
        return ResultUtils.success(categoryService.findCategory(categoryName));
    }

}
