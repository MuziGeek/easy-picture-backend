package com.muzi.easypicturebackend.job;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muzi.easypicturebackend.constant.RedisConstant;
import com.muzi.easypicturebackend.mapper.PostMapper;
import com.muzi.easypicturebackend.model.entity.Post;
import com.muzi.easypicturebackend.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PostCacheWarmUpJob implements CommandLineRunner {

    @Resource
    private PostMapper postMapper;

    @Resource
    private PostService postService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        try {
            log.info("开始帖子数据缓存预热");
            warmUpPostCache();
            warmUpTop100Cache();
            log.info("帖子数据缓存预热完成");
        } catch (Exception e) {
            log.error("帖子数据缓存预热失败", e);
        }
    }

    /**
     * 预热帖子分页数据
     */
    private void warmUpPostCache() throws Exception {
        // 获取最近一周的热门帖子
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1)
                .eq("isDelete", 0)
                .orderByDesc("viewCount", "likeCount", "commentCount")
                .last("LIMIT 1000"); // 限制数量，避免数据太多

        List<Post> posts = postMapper.selectList(queryWrapper);

        // 按每页20条数据进行分页缓存
        int pageSize = 20;
        int totalPages = (posts.size() + pageSize - 1) / pageSize;

        for (int i = 1; i <= Math.min(totalPages, 5); i++) { // 只缓存前5页
            int start = (i - 1) * pageSize;
            int end = Math.min(start + pageSize, posts.size());
            List<Post> pageData = posts.subList(start, end);

            // 构建分页对象
            Page<Post> page = new Page<>(i, pageSize);
            page.setRecords(pageData);
            page.setTotal(posts.size());

            // 填充帖子信息
            pageData.forEach(post -> postService.fillPostInfo(post));

            // 设置缓存
            String cacheKey = RedisConstant.POST_PAGE_CACHE_PREFIX + i + ":" + pageSize;
            stringRedisTemplate.opsForValue().set(
                cacheKey,
                objectMapper.writeValueAsString(page),
                RedisConstant.POST_CACHE_EXPIRE_TIME + RandomUtil.randomInt(0, 300),
                TimeUnit.SECONDS
            );
        }
    }

    /**
     * 预热帖子榜单数据
     */
    private void warmUpTop100Cache() throws Exception {
        // 预热四种榜单：日榜、周榜、月榜、总榜
        for (long i = 1; i <= 4; i++) {
            String cacheKey = RedisConstant.TOP_100_POST_REDIS_KEY_PREFIX + i;

            // 如果缓存已存在，跳过
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cacheKey))) {
                continue;
            }

            List<Post> postList = postService.getTop100Post(i);

            stringRedisTemplate.opsForValue().set(
                cacheKey,
                objectMapper.writeValueAsString(postList),
                RedisConstant.TOP_100_POST_REDIS_KEY_EXPIRE_TIME + RandomUtil.randomInt(0, 300),
                TimeUnit.SECONDS
            );
        }
    }
}
