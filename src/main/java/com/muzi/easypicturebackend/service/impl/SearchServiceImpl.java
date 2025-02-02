package com.muzi.easypicturebackend.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.muzi.easypicturebackend.exception.BusinessException;
import com.muzi.easypicturebackend.exception.ErrorCode;
import com.muzi.easypicturebackend.model.dto.search.SearchRequest;
import com.muzi.easypicturebackend.model.entity.Picture;
import com.muzi.easypicturebackend.model.entity.User;
import com.muzi.easypicturebackend.model.vo.PictureVO;
import com.muzi.easypicturebackend.model.vo.UserVO;
import com.muzi.easypicturebackend.service.PictureService;
import com.muzi.easypicturebackend.service.SearchService;
import com.muzi.easypicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    private static final String PICTURE_INDEX = "picture";
    private static final String USER_INDEX = "user";

//    @Resource
//    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Override
    public Page<?> doSearch(SearchRequest searchRequest) {
        String searchText = searchRequest.getSearchText();
        String type = searchRequest.getType();

        // 校验参数
        if (StringUtils.isBlank(searchText)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        }

        // 根据type选择不同的搜索策略
        return switch (type) {
            case "picture" -> searchPicture(searchRequest);
            case "user" -> searchUser(searchRequest);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的搜索类型");
        };
    }

    /**
     * 搜索图片
     */
    private Page<PictureVO> searchPicture(SearchRequest searchRequest) {
        String searchText = searchRequest.getSearchText();
        Integer current = searchRequest.getCurrent();
        Integer pageSize = searchRequest.getPageSize();


        // 构建查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();

        // 搜索条件
        queryWrapper.and(wrapper ->
                wrapper.like("name", searchText)
                       .or().like("introduction", searchText)
                       .or().like("tags", searchText)
        );

        // 尝试将搜索文本转换为图片 ID
        try {
            Long pictureId = Long.parseLong(searchText);
            queryWrapper.or().eq("id", pictureId);
        } catch (NumberFormatException ignored) {
        }

        // 必要条件：已通过审核、未删除、公共图库
        queryWrapper.eq("reviewStatus", 1)
                    .eq("isDelete", 0)
                    .and(wrapper ->
                            wrapper.isNull("spaceId")
                                   .or().eq("spaceId", 0)
                    );

        // 排序条件
        queryWrapper.orderByDesc("createTime");

        // 分页查询
        Page<Picture> page = new Page<>(current, pageSize);
        IPage<Picture> picturePage = pictureService.getBaseMapper().selectPage(page, queryWrapper);
        // 获取搜索结果并转换为 PictureVO
        List<PictureVO> pictureVOList = picturePage.getRecords().stream()
                                                   .map(picture -> {
                                                       PictureVO pictureVO = PictureVO.objToVo(picture);
                                                       // 获取并设置脱敏后的用户信息
                                                       User user = userService.getById(picture.getUserId());
                                                       if (user != null) {
                                                           pictureVO.setUser(userService.getUserVO(user));
                                                       }
                                                       return pictureVO;
                                                   })
                                                   .collect(Collectors.toList());
        Page<PictureVO> pictureVOPage = new Page<>(current, pageSize, picturePage.getTotal());

        return pictureVOPage.setRecords(pictureVOList);

    }

    /**
     * 搜索用户
     */
    private Page<UserVO> searchUser(SearchRequest searchRequest) {
        String searchText = searchRequest.getSearchText();
        Integer current = searchRequest.getCurrent();
        Integer pageSize = searchRequest.getPageSize();

        // 构建查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        // 搜索条件
        queryWrapper.and(wrapper ->
                wrapper.like("userName", searchText)
                       .or().like("userAccount", searchText)
                       .or().like("userProfile", searchText)
        );

        // 尝试将搜索文本转换为用户 ID
        try {
            Long userId = Long.parseLong(searchText);
            queryWrapper.or().eq("id", userId);
        } catch (NumberFormatException ignored) {
        }

        // 必要条件：未删除
        queryWrapper.eq("isDelete", 0);

        // 排序条件
        queryWrapper.orderByDesc("createTime");

        // 分页查询
        Page<User> page = new Page<>(current, pageSize);
        IPage<User> userPage = userService.getBaseMapper().selectPage(page, queryWrapper);

        // 获取搜索结果并转换为 UserVO
        List<UserVO> userVOList = userPage.getRecords().stream()
                                          .map(userService::getUserVO)
                                          .collect(Collectors.toList());
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());

        return userVOPage.setRecords(userVOList);
    }
}
