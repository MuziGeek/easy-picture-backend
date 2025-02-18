package com.muzi.easypicturebackend.service;

import org.springframework.data.domain.Page;
import com.muzi.easypicturebackend.model.dto.search.SearchRequest;

import java.util.List;

public interface SearchService {
    /**
     * 统一搜索接口
     * @param searchRequest
     * @return
     */
    Page<?> doSearch(SearchRequest searchRequest);

    List<String> getHotSearchKeywords(String type, Integer size);

} 