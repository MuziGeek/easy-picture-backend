package com.muzi.easypicturebackend.job;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.muzi.easypicturebackend.mapper.PictureMapper;
import com.muzi.easypicturebackend.mapper.PostMapper;
import com.muzi.easypicturebackend.mapper.SpaceMapper;
import com.muzi.easypicturebackend.mapper.UserMapper;
import com.muzi.easypicturebackend.model.entity.Picture;
import com.muzi.easypicturebackend.model.entity.Post;
import com.muzi.easypicturebackend.model.entity.Space;
import com.muzi.easypicturebackend.model.entity.User;
import com.muzi.easypicturebackend.model.entity.es.EsSpace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ElasticSearchSyncJob implements CommandLineRunner {

    private static final String PICTURE_INDEX = "picture";
    private static final String USER_INDEX = "user";
    private static final String POST_INDEX = "post";
    private static final String SPACE_INDEX = "space";

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private PostMapper postMapper;

    @Resource
    private SpaceMapper spaceMapper;

    // 记录上次同步时间
    private Date lastSyncTime = new Date();

    /**
     * 应用启动时执行全量同步
     */
    @Override
    public void run(String... args) {
        fullSync();
    }

    /**
     * 全量同步
     */
    public void fullSync() {
        try {
            log.info("开始全量同步数据到ES");
            // 同步图片数据
            fullSyncPictures();
            // 同步用户数据
            fullSyncUsers();
            // 同步帖子数据
            fullSyncPosts();
            // 同步空间数据
            fullSyncSpaces();
            log.info("全量同步数据到ES完成");
        } catch (Exception e) {
            log.error("全量同步数据到ES失败", e);
        }
    }

    /**
     * 每10分钟执行一次图片增量同步，随机延迟0-30秒
     */
    @Scheduled(fixedDelay = 600000)
    public void incrementalSyncPictures() {
        try {
            // 随机延迟0-30秒
            Thread.sleep(RandomUtil.randomInt(0, 30) * 1000);
            log.info("开始增量同步图片数据到ES");
            Date currentTime = new Date();
            incrementalSyncPictures(lastSyncTime, currentTime);
            lastSyncTime = currentTime;
            log.info("增量同步图片数据到ES完成");
        } catch (Exception e) {
            log.error("增量同步图片数据到ES失败", e);
        }
    }

    /**
     * 每10分钟执行一次用户增量同步，固定延迟2分钟，随机延迟0-30秒
     */
    @Scheduled(fixedDelay = 600000, initialDelay = 120000)
    public void incrementalSyncUsers() {
        try {
            // 随机延迟0-30秒
            Thread.sleep(RandomUtil.randomInt(0, 30) * 1000L);
            log.info("开始增量同步用户数据到ES");
            Date currentTime = new Date();
            incrementalSyncUsers(lastSyncTime, currentTime);
            lastSyncTime = currentTime;
            log.info("增量同步用户数据到ES完成");
        } catch (Exception e) {
            log.error("增量同步用户数据到ES失败", e);
        }
    }

    /**
     * 每10分钟执行一次帖子增量同步，固定延迟4分钟，随机延迟0-30秒
     */
    @Scheduled(fixedDelay = 600000, initialDelay = 240000)
    public void incrementalSyncPosts() {
        try {
            // 随机延迟0-30秒
            Thread.sleep(RandomUtil.randomInt(0, 30) * 1000L);
            log.info("开始增量同步帖子数据到ES");
            Date currentTime = new Date();
            incrementalSyncPosts(lastSyncTime, currentTime);
            lastSyncTime = currentTime;
            log.info("增量同步帖子数据到ES完成");
        } catch (Exception e) {
            log.error("增量同步帖子数据到ES失败", e);
        }
    }

    /**
     * 全量同步空间数据
     */
    private void fullSyncSpaces() {
        // 分页查询所有未删除的空间
        long current = 1;
        long size = 100;
        long total;
        do {
            Page<Space> spacePage = spaceMapper.selectPage(
                    new Page<>(current, size),
                    new QueryWrapper<Space>().eq("isDelete", 0)
            );
            total = spacePage.getTotal();

            List<Space> spaceList = spacePage.getRecords();
            if (!spaceList.isEmpty()) {
                List<IndexQuery> queries = spaceList.stream()
                        .map(space -> {
                            EsSpace esSpace = new EsSpace();
                            BeanUtils.copyProperties(space, esSpace);
                            return new IndexQueryBuilder()
                                    .withId(space.getId().toString())
                                    .withObject(esSpace)
                                    .build();
                        })
                        .collect(Collectors.toList());

                elasticsearchRestTemplate.bulkIndex(queries, IndexCoordinates.of(SPACE_INDEX));
                log.info("同步空间数据到ES, 第{}页, 数量: {}", current, spaceList.size());
            }
            current++;
        } while ((current - 1) * size < total);

        // 清理已删除的数据
        cleanDeletedSpaces();
    }

    /**
     * 增量同步空间数据
     */
    private void incrementalSyncSpaces(Date startTime, Date endTime) {
        // 查询这段时间内更新的未删除数据
        List<Space> spaces = spaceMapper.selectList(new QueryWrapper<Space>()
                .ge("updateTime", startTime)
                .le("updateTime", endTime)
                .eq("isDelete", 0));

        if (!spaces.isEmpty()) {
            List<IndexQuery> queries = spaces.stream()
                    .map(space -> {
                        EsSpace esSpace = new EsSpace();
                        BeanUtils.copyProperties(space, esSpace);
                        return new IndexQueryBuilder()
                                .withId(space.getId().toString())
                                .withObject(esSpace)
                                .build();
                    })
                    .collect(Collectors.toList());

            elasticsearchRestTemplate.bulkIndex(queries, IndexCoordinates.of(SPACE_INDEX));
            log.info("增量同步空间数据到ES, 数量: {}", spaces.size());
        }

        // 处理这段时间内被删除的数据
        List<Space> deletedSpaces = spaceMapper.selectList(new QueryWrapper<Space>()
                .ge("updateTime", startTime)
                .le("updateTime", endTime)
                .eq("isDelete", 1));

        if (!deletedSpaces.isEmpty()) {
            deletedSpaces.forEach(space ->
                    elasticsearchRestTemplate.delete(space.getId().toString(), IndexCoordinates.of(SPACE_INDEX))
            );
            log.info("从ES删除已删除的空间, 数量: {}", deletedSpaces.size());
        }
    }

    /**
     * 每10分钟执行一次空间增量同步，固定延迟4分钟，随机延迟0-30秒
     */
    @Scheduled(fixedDelay = 600000, initialDelay = 240000)
    public void incrementalSyncSpaces() {
        try {
            // 随机延迟0-30秒
            Thread.sleep(RandomUtil.randomInt(0, 30) * 1000L);
            log.info("开始增量同步空间数据到ES");
            Date currentTime = new Date();
            incrementalSyncSpaces(lastSyncTime, currentTime);
            lastSyncTime = currentTime;
            log.info("增量同步空间数据到ES完成");
        } catch (Exception e) {
            log.error("增量同步空间数据到ES失败", e);
        }
    }

    /**
     * 全量同步图片数据
     */
    private void fullSyncPictures() {
        // 分批查询所有未删除的图片数据
        int batchSize = 1000;
        QueryWrapper<Picture> wrapper = new QueryWrapper<Picture>()
                .eq("isDelete", 0);
        long total = pictureMapper.selectCount(wrapper);
        long pages = (total + batchSize - 1) / batchSize;

        for (int i = 0; i < pages; i++) {
            List<Picture> pictures = pictureMapper.selectList(
                    wrapper.last("limit " + i * batchSize + "," + batchSize)
            );

            if (pictures.isEmpty()) {
                continue;
            }

            // 构建索引请求
            List<IndexQuery> queries = pictures.stream()
                    .map(picture -> new IndexQueryBuilder()
                            .withId(picture.getId().toString())
                            .withObject(picture)
                            .build())
                    .collect(Collectors.toList());

            // 批量保存到ES
            elasticsearchRestTemplate.bulkIndex(queries, IndexCoordinates.of(PICTURE_INDEX));
            log.info("同步图片数据到ES: 第{}批, 数量{}", i + 1, pictures.size());
        }

        // 清理已删除的数据
        cleanDeletedPictures();
    }

    /**
     * 全量同步用户数据
     */
    private void fullSyncUsers() {
        // 分批查询所有未删除的用户数据
        int batchSize = 1000;
        QueryWrapper<User> wrapper = new QueryWrapper<User>()
                .eq("isDelete", 0);
        long total = userMapper.selectCount(wrapper);
        long pages = (total + batchSize - 1) / batchSize;

        for (int i = 0; i < pages; i++) {
            List<User> users = userMapper.selectList(
                    wrapper.last("limit " + i * batchSize + "," + batchSize)
            );

            if (users.isEmpty()) {
                continue;
            }

            // 构建索引请求
            List<IndexQuery> queries = users.stream()
                    .map(user -> new IndexQueryBuilder()
                            .withId(user.getId().toString())
                            .withObject(user)
                            .build())
                    .collect(Collectors.toList());

            // 批量保存到ES
            elasticsearchRestTemplate.bulkIndex(queries, IndexCoordinates.of(USER_INDEX));
            log.info("同步用户数据到ES: 第{}批, 数量{}", i + 1, users.size());
        }

        // 清理已删除的数据
        cleanDeletedUsers();
    }

    /**
     * 全量同步帖子数据
     */
    private void fullSyncPosts() {
        // 分批查询所有未删除的帖子
        long current = 1;
        long size = 1000;
        long total;

        do {
            Page<Post> postPage = postMapper.selectPage(
                    new Page<>(current, size),
                    new QueryWrapper<Post>().eq("isDelete", 0)
            );

            List<Post> posts = postPage.getRecords();
            total = postPage.getTotal();

            if (!posts.isEmpty()) {
                List<IndexQuery> queries = posts.stream()
                        .map(post -> new IndexQueryBuilder()
                                .withId(post.getId().toString())
                                .withObject(post)
                                .build()
                        )
                        .collect(Collectors.toList());

                elasticsearchRestTemplate.bulkIndex(queries, IndexCoordinates.of(POST_INDEX));
            }

            current++;
        } while ((current - 1) * size < total);

        log.info("全量同步帖子数据到ES完成");
    }

    /**
     * 增量同步图片数据
     */
    private void incrementalSyncPictures(Date startTime, Date endTime) {
        // 查询这段时间内更新的未删除数据
        List<Picture> pictures = pictureMapper.selectList(new QueryWrapper<Picture>()
                .ge("updateTime", startTime)
                .le("updateTime", endTime)
                .eq("isDelete", 0));

        if (!pictures.isEmpty()) {
            List<IndexQuery> queries = pictures.stream()
                    .map(picture -> new IndexQueryBuilder()
                            .withId(picture.getId().toString())
                            .withObject(picture)
                            .build())
                    .collect(Collectors.toList());

            elasticsearchRestTemplate.bulkIndex(queries, IndexCoordinates.of(PICTURE_INDEX));
            log.info("增量同步图片数据到ES, 数量: {}", pictures.size());
        }

        // 处理这段时间内被删除的数据
        List<Picture> deletedPictures = pictureMapper.selectList(new QueryWrapper<Picture>()
                .ge("updateTime", startTime)
                .le("updateTime", endTime)
                .eq("isDelete", 1));

        if (!deletedPictures.isEmpty()) {
            deletedPictures.forEach(picture ->
                    elasticsearchRestTemplate.delete(picture.getId().toString(), IndexCoordinates.of(PICTURE_INDEX))
            );
            log.info("从ES删除已删除的图片, 数量: {}", deletedPictures.size());
        }
    }

    /**
     * 增量同步用户数据
     */
    private void incrementalSyncUsers(Date startTime, Date endTime) {
        // 查询这段时间内更新的未删除数据
        List<User> users = userMapper.selectList(new QueryWrapper<User>()
                .ge("updateTime", startTime)
                .le("updateTime", endTime)
                .eq("isDelete", 0));

        if (!users.isEmpty()) {
            List<IndexQuery> queries = users.stream()
                    .map(user -> new IndexQueryBuilder()
                            .withId(user.getId().toString())
                            .withObject(user)
                            .build())
                    .collect(Collectors.toList());

            elasticsearchRestTemplate.bulkIndex(queries, IndexCoordinates.of(USER_INDEX));
            log.info("增量同步用户数据到ES, 数量: {}", users.size());
        }

        // 处理这段时间内被删除的数据
        List<User> deletedUsers = userMapper.selectList(new QueryWrapper<User>()
                .ge("updateTime", startTime)
                .le("updateTime", endTime)
                .eq("isDelete", 1));

        if (!deletedUsers.isEmpty()) {
            deletedUsers.forEach(user ->
                    elasticsearchRestTemplate.delete(user.getId().toString(), IndexCoordinates.of(USER_INDEX))
            );
            log.info("从ES删除已删除的用户, 数量: {}", deletedUsers.size());
        }
    }

    /**
     * 增量同步帖子数据
     */
    private void incrementalSyncPosts(Date startTime, Date endTime) {
        // 查询这段时间内更新的未删除数据
        List<Post> posts = postMapper.selectList(new QueryWrapper<Post>()
                .ge("updateTime", startTime)
                .le("updateTime", endTime)
                .eq("isDelete", 0));

        if (!posts.isEmpty()) {
            List<IndexQuery> queries = posts.stream()
                    .map(post -> new IndexQueryBuilder()
                            .withId(post.getId().toString())
                            .withObject(post)
                            .build())
                    .collect(Collectors.toList());

            elasticsearchRestTemplate.bulkIndex(queries, IndexCoordinates.of(POST_INDEX));
            log.info("增量同步帖子数据到ES, 数量: {}", posts.size());
        }

        // 处理这段时间内被删除的数据
        List<Post> deletedPosts = postMapper.selectList(new QueryWrapper<Post>()
                .ge("updateTime", startTime)
                .le("updateTime", endTime)
                .eq("isDelete", 1));

        if (!deletedPosts.isEmpty()) {
            deletedPosts.forEach(post ->
                    elasticsearchRestTemplate.delete(post.getId().toString(), IndexCoordinates.of(POST_INDEX))
            );
            log.info("从ES删除已删除的帖子, 数量: {}", deletedPosts.size());
        }
    }

    /**
     * 清理已删除的图片数据
     */
    private void cleanDeletedPictures() {
        List<Picture> deletedPictures = pictureMapper.selectList(
                new QueryWrapper<Picture>().eq("isDelete", 1)
        );

        if (!deletedPictures.isEmpty()) {
            deletedPictures.forEach(picture ->
                    elasticsearchRestTemplate.delete(picture.getId().toString(), IndexCoordinates.of(PICTURE_INDEX))
            );
            log.info("清理已删除的图片数据, 数量: {}", deletedPictures.size());
        }
    }

    /**
     * 清理已删除的用户数据
     */
    private void cleanDeletedUsers() {
        List<User> deletedUsers = userMapper.selectList(
                new QueryWrapper<User>().eq("isDelete", 1)
        );

        if (!deletedUsers.isEmpty()) {
            deletedUsers.forEach(user ->
                    elasticsearchRestTemplate.delete(user.getId().toString(), IndexCoordinates.of(USER_INDEX))
            );
            log.info("清理已删除的用户数据, 数量: {}", deletedUsers.size());
        }
    }

    /**
     * 清理已删除的空间数据
     */
    private void cleanDeletedSpaces() {
        List<Space> deletedSpaces = spaceMapper.selectList(
                new QueryWrapper<Space>().eq("isDelete", 1)
        );

        if (!deletedSpaces.isEmpty()) {
            deletedSpaces.forEach(space ->
                    elasticsearchRestTemplate.delete(space.getId().toString(), IndexCoordinates.of(SPACE_INDEX))
            );
            log.info("清理已删除的空间数据, 数量: {}", deletedSpaces.size());
        }
    }
}
