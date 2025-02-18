package com.muzi.easypicturebackend.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.muzi.easypicturebackend.annotation.AuthCheck;
import com.muzi.easypicturebackend.common.BaseResponse;
import com.muzi.easypicturebackend.common.PageRequest;
import com.muzi.easypicturebackend.common.ResultUtils;
import com.muzi.easypicturebackend.constant.UserConstant;
import com.muzi.easypicturebackend.model.entity.Category;
import com.muzi.easypicturebackend.model.vo.CategoryVO;
import com.muzi.easypicturebackend.service.CategoryService;
import org.springframework.web.bind.annotation.*;


import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {
    @Resource
    private CategoryService categoryService;

    /**
     * 分页获取分类列表（管理员）
     *
     * @param pageRequest 分页请求参数
     * @param type        分类类型（可选）
     * @return 分类列表（包含统计信息）
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<CategoryVO>> listCategoryVO(PageRequest pageRequest,
                                                         @RequestParam(required = false) Integer type) {
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        if (type != null) {
            queryWrapper.eq("type", type);
        }

        Page<Category> categoryPage = categoryService.page(
                new Page<>(pageRequest.getCurrent(), pageRequest.getPageSize()),
                queryWrapper
        );

        Page<CategoryVO> categoryVOPage = new Page<>(
                pageRequest.getCurrent(),
                pageRequest.getPageSize(),
                categoryPage.getTotal()
        );

        List<CategoryVO> categoryVOList = categoryService.listCategoryVO(categoryPage.getRecords());
        categoryVOPage.setRecords(categoryVOList);
        return ResultUtils.success(categoryVOPage);
    }

    /**
     * 获取指定类型的分类列表
     *
     * @param type 分类类型（1-图片分类，2-帖子分类）
     * @return 分类名称列表
     */
    @GetMapping("/list/type/{type}")
    public BaseResponse<List<String>> listCategoryByType(@PathVariable Integer type) {
        return ResultUtils.success(categoryService.listCategoryByType(type));
    }

    /**
     * 添加新分类（管理员）
     *
     * @param categoryName 分类名称
     * @param type         分类类型（1-图片分类，2-帖子分类）
     * @return 添加结果
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> addCategory(@RequestParam String categoryName,
                                             @RequestParam Integer type) {
        return ResultUtils.success(categoryService.addCategory(categoryName, type));
    }

    /**
     * 删除分类（管理员）
     *
     * @param categoryId 分类ID
     * @return 删除结果
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteCategory(@RequestParam Long categoryId) {
        return ResultUtils.success(categoryService.removeById(categoryId));
    }

    /**
     * 搜索分类（管理员）
     *
     * @param categoryName 分类名称关键词
     * @param type         分类类型（可选）
     * @return 匹配的分类列表
     */
    @PostMapping("/search")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<CategoryVO>> findCategory(@RequestParam String categoryName,
                                                       @RequestParam(required = false) Integer type) {
        return ResultUtils.success(categoryService.findCategory(categoryName, type));
    }

}
