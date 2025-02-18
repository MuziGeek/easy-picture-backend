package com.muzi.easypicturebackend.model.dto.post;

import lombok.Data;

import java.util.List;

@Data
public class PostAddRequest {
    private String title;

    // 使用 Markdown 格式的内容，图片使用 ![alt](url) 格式
    private String content;

    private String category;

    private List<String> tags;

    // 附件列表仅用于存储额外的附件，不包括正文中的图片
    private List<PostAttachmentRequest> attachments;
}
