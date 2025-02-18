package com.muzi.easypicturebackend.mapper;

import com.muzi.easypicturebackend.model.entity.HotSearch;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
* @author 57242
* @description 针对表【hot_search(热门搜索记录表)】的数据库操作Mapper
* @createDate 2025-02-16 20:12:43
* @Entity com.muzi.easypicturebackend.model.entity.HotSearch
*/
public interface HotSearchMapper extends BaseMapper<HotSearch> {
    /**
     * 批量插入或更新热门搜索
     */
    void batchInsertOrUpdate(@Param("list") List<HotSearch> hotSearchList);

    /**
     * 获取指定时间之后的热门搜索
     */
    List<HotSearch> getHotSearchAfter(@Param("type") String type,
                                      @Param("startTime") Date startTime,
                                      @Param("limit") Integer limit);
}




