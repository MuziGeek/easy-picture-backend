package com.muzi.easypicturebackend.constant;

/**
 * Redis 常量
 */
public interface RedisConstant {

    /**
     * 用户签到记录的 Redis key 前缀
     */
    String USER_SIGN_IN_REDIS_KEY_PREFIX = "user:signins";

    /**
     * top100
     */
    String TOP_100_PIC_REDIS_KEY_PREFIX = "top100Picture:";

    /**
     * top100过期时间为1天
     */
    long TOP_100_PIC_REDIS_KEY_EXPIRE_TIME =  24 * 60 * 60;

    /**
     * 公共图库前置
     */
    String PUBLIC_PIC_REDIS_KEY_PREFIX = "easypicture:listPictureVOByPage:";

    /**
     * 帖子top100
     */
    String TOP_100_POST_REDIS_KEY_PREFIX = "top100Post:";

    /**
     * 帖子top100过期时间为1天
     */
    long TOP_100_POST_REDIS_KEY_EXPIRE_TIME = 24 * 60 * 60;

    /**
     * 空间聊天记录缓存前缀
     */
    String SPACE_CHAT_HISTORY_PREFIX = "chat:space:";

    /**
     * 图片聊天记录缓存前缀
     */
    String PICTURE_CHAT_HISTORY_PREFIX = "chat:picture:";

    /**
     * 私聊记录缓存前缀
     */
    String PRIVATE_CHAT_HISTORY_PREFIX = "chat:private:";

    /**
     * 聊天记录缓存过期时间（30分钟）
     */
    long CHAT_HISTORY_EXPIRE_TIME = 30 * 60;

    /**
     * 帖子分页数据缓存前缀
     */
    String POST_PAGE_CACHE_PREFIX = "post:page:";


    /**
     * 帖子缓存过期时间（1小时）
     */
    long POST_CACHE_EXPIRE_TIME = 60 * 60;

    /**
     * 获取用户签到记录的 Redis Key
     * @param year 年份
     * @param userId 用户 id
     * @return 拼接好的 Redis Key
     */
    static String getUserSignInRedisKey(int year, long userId) {
        return String.format("%s:%s:%S", USER_SIGN_IN_REDIS_KEY_PREFIX, year, userId);
    }
}
