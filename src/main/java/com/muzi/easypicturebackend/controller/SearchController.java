package com.muzi.easypicturebackend.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.muzi.easypicturebackend.common.BaseResponse;
import com.muzi.easypicturebackend.common.ResultUtils;
import com.muzi.easypicturebackend.exception.BusinessException;
import com.muzi.easypicturebackend.exception.ErrorCode;
import com.muzi.easypicturebackend.model.dto.search.SearchRequest;
import com.muzi.easypicturebackend.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/search")
@Slf4j
public class SearchController {

    @Resource
    private SearchService searchService;

    @PostMapping("/all")
    public BaseResponse<Page<?>> searchAll(@RequestBody SearchRequest searchRequest) {
        return ResultUtils.success((Page<?>) searchService.doSearch(searchRequest));
    }

    /**
     * 获取热门搜索关键词
     * @param type 搜索类型 (picture/user/post/space)
     * @param size 返回数量，默认9个
     * @return 热门搜索关键词列表
     */
    @GetMapping("/hot")
    public BaseResponse<List<String>> getHotSearchKeywords(
            @RequestParam(required = true) String type,
            @RequestParam(required = false, defaultValue = "9") Integer size) {
        // 参数校验
        if (StringUtils.isBlank(type)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索类型不能为空");
        }
        if (size <= 0 || size > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "size必须在1-100之间");
        }

        // 校验搜索类型
        if (!type.matches("^(picture|user|post|space)$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的搜索类型");
        }

        return ResultUtils.success(searchService.getHotSearchKeywords(type, size));
    }
} 