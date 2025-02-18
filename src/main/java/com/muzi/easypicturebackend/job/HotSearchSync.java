package com.muzi.easypicturebackend.job;

/**
 * 热门搜索同步接口
 */
public interface HotSearchSync {

    /**
     * 同步热门搜索数据
     */
    void syncHotSearch();

    /**
     * 预热缓存
     */
    void warmUpCache();
}
