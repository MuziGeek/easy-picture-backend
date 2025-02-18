package com.muzi.easypicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzi.easypicturebackend.esdao.EsPictureDao;
import com.muzi.easypicturebackend.esdao.EsPostDao;
import com.muzi.easypicturebackend.exception.BusinessException;
import com.muzi.easypicturebackend.exception.ErrorCode;
import com.muzi.easypicturebackend.mapper.ShareRecordMapper;
import com.muzi.easypicturebackend.model.dto.share.ShareQueryRequest;
import com.muzi.easypicturebackend.model.dto.share.ShareRequest;
import com.muzi.easypicturebackend.model.entity.Picture;
import com.muzi.easypicturebackend.model.entity.Post;
import com.muzi.easypicturebackend.model.entity.ShareRecord;
import com.muzi.easypicturebackend.model.entity.User;
import com.muzi.easypicturebackend.model.vo.PictureVO;
import com.muzi.easypicturebackend.model.vo.ShareRecordVO;
import com.muzi.easypicturebackend.service.PictureService;
import com.muzi.easypicturebackend.service.PostService;
import com.muzi.easypicturebackend.service.ShareRecordService;
import com.muzi.easypicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
* @author 57242
* @description 针对表【share_record(分享记录表)】的数据库操作Service实现
* @createDate 2025-02-16 20:15:31
*/
@Service
@Slf4j
public class ShareRecordServiceImpl extends ServiceImpl<ShareRecordMapper, ShareRecord>
    implements ShareRecordService{
    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private PostService postService;

    @Resource
    private EsPictureDao esPictureDao;

    @Resource
    private EsPostDao esPostDao;

    @Override
    @Async("asyncExecutor")
    @Transactional(rollbackFor = Exception.class)
    public CompletableFuture<Boolean> doShare(ShareRequest shareRequest, Long userId) {
        try {
            Long targetId = shareRequest.getTargetId();
            Integer targetType = shareRequest.getTargetType();
            Boolean isShared = shareRequest.getIsShared();

            // 参数校验
            if (targetId == null || targetType == null || isShared == null || userId == null) {
                log.error("Invalid parameters: targetId={}, targetType={}, isShared={}, userId={}",
                        targetId, targetType, isShared, userId);
                return CompletableFuture.completedFuture(false);
            }

            // 校验目标类型
            if (targetType != 1 && targetType != 2) { // 只允许图片(1)和帖子(2)
                log.error("Invalid target type: {}", targetType);
                return CompletableFuture.completedFuture(false);
            }

            // 获取目标内容所属用户ID
            Long targetUserId = getTargetUserId(targetId, targetType);
            if (targetUserId == null) {
                log.error("Target content not found: targetId={}, targetType={}", targetId, targetType);
                return CompletableFuture.completedFuture(false);
            }

            // 查询当前分享状态
            QueryWrapper<ShareRecord> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userId", userId)
                        .eq("targetId", targetId)
                        .eq("targetType", targetType);
            ShareRecord oldShareRecord = this.getOne(queryWrapper);

            if (oldShareRecord == null) {
                // 首次分享
                ShareRecord shareRecord = new ShareRecord();
                shareRecord.setUserId(userId);
                shareRecord.setTargetId(targetId);
                shareRecord.setTargetType(targetType);
                shareRecord.setTargetUserId(targetUserId); // 设置目标内容所属用户ID
                shareRecord.setShareTime(new Date());
                shareRecord.setIsRead(0);
                this.save(shareRecord);
                updateShareCount(targetId, targetType, 1);
            } else {
                // 更新分享状态
                if (oldShareRecord.getIsShared() != isShared) {
                    oldShareRecord.setIsShared(isShared);
                    oldShareRecord.setShareTime(new Date());
                    if (isShared) {
                        oldShareRecord.setIsRead(0); // 重新分享时设置为未读
                    }
                    this.updateById(oldShareRecord);
                    updateShareCount(targetId, targetType, isShared ? 1 : -1);
                }
            }

            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Error in doShare: ", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * 获取目标内容所属用户ID
     */
    private Long getTargetUserId(Long targetId, Integer targetType) {
        try {
            switch (targetType) {
                case 1: // 图片
                    Picture picture = pictureService.getById(targetId);
                    return picture != null ? picture.getUserId() : null;
                case 2: // 帖子
                    Post post = postService.getById(targetId);
                    return post != null ? post.getUserId() : null;
                default:
                    return null;
            }
        } catch (Exception e) {
            log.error("Error getting target user id: ", e);
            return null;
        }
    }

    /**
     * 更新分享数
     */
    private void updateShareCount(Long targetId, Integer targetType, int delta) {
        switch (targetType) {
            case 1: // 图片
                pictureService.update()
                              .setSql("shareCount = shareCount + " + delta)
                              .eq("id", targetId)
                              .ge("shareCount", -delta)
                              .update();
                updateEsPictureShareCount(targetId, delta);
                break;
            case 2: // 帖子
                postService.update()
                           .setSql("shareCount = shareCount + " + delta)
                           .eq("id", targetId)
                           .ge("shareCount", -delta)
                           .update();
                updateEsPostShareCount(targetId, delta);
                break;
            default:
                log.error("Unsupported target type: {}", targetType);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ShareRecordVO> getAndClearUnreadShares(Long userId) {
        // 1. 获取未读分享记录
        QueryWrapper<ShareRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("targetUserId", userId)
                    .eq("isRead", 0)
                    .ne("userId", userId)  // 排除自己分享自己的记录
                    .orderByDesc("shareTime")
                    .last("LIMIT 50");  // 限制最多返回50条数据

        List<ShareRecord> unreadShares = this.list(queryWrapper);
        if (CollUtil.isEmpty(unreadShares)) {
            return new ArrayList<>();
        }

        // 2. 批量更新为已读
        List<Long> shareIds = unreadShares.stream()
                                          .map(ShareRecord::getId)
                                          .collect(Collectors.toList());

        this.update(new UpdateWrapper<ShareRecord>()
                .set("isRead", 1)
                .in("id", shareIds));

        return convertToVOList(unreadShares);
    }

    /**
     * 更新 ES 中图片的分享数
     */
    private void updateEsPictureShareCount(Long pictureId, int delta) {
        try {
            esPictureDao.findById(pictureId).ifPresent(esPicture -> {
                esPicture.setShareCount(esPicture.getShareCount() + delta);
                esPictureDao.save(esPicture);
            });
        } catch (Exception e) {
            log.error("Failed to update ES picture share count, pictureId: {}", pictureId, e);
        }
    }

    /**
     * 更新 ES 中帖子的分享数
     */
    private void updateEsPostShareCount(Long postId, int delta) {
        try {
            esPostDao.findById(postId).ifPresent(esPost -> {
                esPost.setShareCount(esPost.getShareCount() + delta);
                esPostDao.save(esPost);
            });
        } catch (Exception e) {
            log.error("Failed to update ES post share count, postId: {}", postId, e);
        }
    }

    @Override
    public Page<ShareRecordVO> getUserShareHistory(ShareQueryRequest shareQueryRequest, Long userId) {
        long current = shareQueryRequest.getCurrent();
        long size = shareQueryRequest.getPageSize();

        // 创建分页对象
        Page<ShareRecord> page = new Page<>(current, size);

        // 构建查询条件
        QueryWrapper<ShareRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("targetUserId", userId)
                    .ne("userId", userId);  // 排除自己分享自己的记录

        // 处理目标类型查询
        Integer targetType = shareQueryRequest.getTargetType();
        if (targetType != null) {
            queryWrapper.eq("targetType", targetType);
        }

        queryWrapper.orderByDesc("shareTime");

        // 执行分页查询
        Page<ShareRecord> sharePage = this.page(page, queryWrapper);

        // 转换结果
        List<ShareRecordVO> records = convertToVOList(sharePage.getRecords());

        // 构建返回结果
        Page<ShareRecordVO> voPage = new Page<>(sharePage.getCurrent(), sharePage.getSize(), sharePage.getTotal());
        voPage.setRecords(records);

        return voPage;
    }

    @Override
    public boolean isContentShared(Long targetId, Integer targetType, Long userId) {
        QueryWrapper<ShareRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("targetId", targetId)
                    .eq("targetType", targetType)
                    .eq("userId", userId)
                    .eq("isShared", true);
        return this.count(queryWrapper) > 0;
    }

    private List<ShareRecordVO> convertToVOList(List<ShareRecord> shareRecords) {
        if (CollUtil.isEmpty(shareRecords)) {
            return new ArrayList<>();
        }

        return shareRecords.stream().map(share -> {
            ShareRecordVO vo = new ShareRecordVO();
            BeanUtils.copyProperties(share, vo);

            // 设置分享用户信息
            User user = userService.getById(share.getUserId());
            vo.setUser(userService.getUserVO(user));

            // 根据类型获取目标内容
            switch (share.getTargetType()) {
                case 1: // 图片
                    Picture picture = pictureService.getById(share.getTargetId());
                    if (picture != null) {
                        PictureVO pictureVO = PictureVO.objToVo(picture);
                        // 设置用户信息
                        User pictureUser = userService.getById(picture.getUserId());
                        pictureVO.setUser(userService.getUserVO(pictureUser));
                        vo.setTarget(pictureVO);
                    }
                    break;
                case 2: // 帖子
                    Post post = postService.getById(share.getTargetId());
                    if (post != null) {
                        // 设置用户信息
                        User postUser = userService.getById(post.getUserId());
                        post.setUser(userService.getUserVO(postUser));
                        vo.setTarget(post);
                    }
                    break;
                default:
                    log.error("Unsupported target type: {}", share.getTargetType());
                    break;
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public long getUnreadSharesCount(Long userId) {
        return this.count(new QueryWrapper<ShareRecord>()
                .eq("targetUserId", userId)
                .eq("isRead", 0)
                .ne("userId", userId));  // 排除自己分享自己的记录
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllUnreadShares(Long userId) {
        this.update(new UpdateWrapper<ShareRecord>()
                .set("isRead", 1)
                .eq("targetUserId", userId)
                .eq("isRead", 0));
    }

    @Override
    public Page<ShareRecordVO> getMyShareHistory(ShareQueryRequest shareQueryRequest, Long userId) {
        long current = shareQueryRequest.getCurrent();
        long size = shareQueryRequest.getPageSize();

        // 创建分页对象
        Page<ShareRecord> page = new Page<>(current, size);

        // 构建查询条件
        QueryWrapper<ShareRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)  // 查询用户自己的分享记录
                    .eq("isShared", true);  // 只查询分享状态为true的记录

        // 处理目标类型查询
        Integer targetType = shareQueryRequest.getTargetType();
        if (targetType != null) {
            queryWrapper.eq("targetType", targetType);
        }

        queryWrapper.orderByDesc("shareTime");

        // 执行分页查询
        Page<ShareRecord> sharePage = this.page(page, queryWrapper);

        // 转换结果
        List<ShareRecordVO> records = convertToVOList(sharePage.getRecords());

        // 构建返回结果
        Page<ShareRecordVO> voPage = new Page<>(sharePage.getCurrent(), sharePage.getSize(), sharePage.getTotal());
        voPage.setRecords(records);

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shareContent(Long targetId, Integer targetType, Long userId) {
        // 获取目标内容所属用户ID
        Long targetUserId = getTargetUserId(targetId, targetType);
        if (targetUserId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "目标内容不存在");
        }

        ShareRecord shareRecord = new ShareRecord();
        shareRecord.setUserId(userId);
        shareRecord.setTargetId(targetId);
        shareRecord.setTargetType(targetType);
        shareRecord.setTargetUserId(targetUserId);
        shareRecord.setShareTime(new Date());
        shareRecord.setIsRead(0);
        shareRecord.setIsShared(true);
        this.save(shareRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unshareContent(Long targetId, Integer targetType, Long userId) {
        this.update(new UpdateWrapper<ShareRecord>()
                .set("isShared", false)
                .eq("targetId", targetId)
                .eq("targetType", targetType)
                .eq("userId", userId));
    }
}




