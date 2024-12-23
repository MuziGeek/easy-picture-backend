package com.muzi.easypicturebackend.model.dto.picture;

import lombok.Data;

import java.util.List;

@Data
public class PictureUploadByBatchRequest {  
  
    /**  
     * 搜索词  
     */  
    private String searchText;  
  
    /**  
     * 抓取数量  
     */  
    private Integer count = 10;
    /**
     * 名称前缀
     */
    private String namePrefix;
    /**
     * 标签
     */
    private List<String> tags;
    /**
     * 分类
     */
    private String category;

}
